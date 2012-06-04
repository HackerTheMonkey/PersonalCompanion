package com.opencloud.slee.services.sip.persistent;

import com.opencloud.slee.services.sip.common.OCSipSbb;
import com.opencloud.javax.sip.PersistentOutboundConnection;
import com.opencloud.javax.sip.ConnectionUpEvent;
import com.opencloud.javax.sip.ConnectionDownEvent;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.SbbContext;
import javax.slee.UnrecognizedActivityException;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.TimerOptions;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceStartedEvent;
import java.text.ParseException;
import java.util.Collections;

/**
 * This SBB demonstrates how to setup and teardown a persistent outbound SIP
 * connection, using the OC SIP stack's
 * <a href="http://tools.ietf.org/html/draft-ietf-sip-outbound-03.txt">draft-ietf-sip-outbound-03</a>
 * support.<p>
 *
 * The SBB is triggered on service activation, and attempts to open a persistent
 * connection using a REGISTER request with the appropriate parameters.<p>
 *
 * When the service is deactivated, the SBB sends a REGISTER request to tear down
 * the connection.<p>
 *
 * The SBB will also re-register periodically based on the expiration time
 * set by the server.
 */
public abstract class PersistentOutboundConnectionSbb extends OCSipSbb {
    public void setSbbContext(SbbContext context) {
        super.setSbbContext(context);
        try {
            Context env = (Context) new InitialContext().lookup("java:comp/env");
            String hostname = (String)env.lookup("hostname");
            int port = ((Integer)env.lookup("port")).intValue();
            String myContactURI = "sip:" + hostname + ":" + port + ";transport=tcp";
            registerRequestTemplate = buildRegisterTemplate((String)env.lookup("registrarURI"),
                    (String)env.lookup("publicURI"), myContactURI, hostname, port);

        } catch (Exception e) {
            severe("failed to set SBB context", e);
        }
    }

    protected String getTraceMessageType() {
        return getClass().getName();
    }

    public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci) {
        info("service started, registering");
        register();
    }

    public void onTimerEvent(TimerEvent event, ActivityContextInterface aci) {
        if (!event.getTimerID().equals(getRegistrationExpiryTimer())) return;
        info("got timer event, re-register");
        register();
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci) {
        if (getServiceActivityFactory().getActivity().equals(aci.getActivity())) {
            info("service deactivating, unregister");
            unregister();
        }
    }

    public void on200Response(ResponseEvent event, ActivityContextInterface aci) {
        registrationSuccessful(event.getResponse());
        attachConnectionActivity(((com.opencloud.javax.sip.ClientTransaction)event.getClientTransaction()).getPersistentOutboundConnection());
    }

    public void on300Response(ResponseEvent event, ActivityContextInterface aci) {
        registrationFailed(event.getResponse().getStatusCode());
    }

    public void on400Response(ResponseEvent event, ActivityContextInterface aci) {
        registrationFailed(event.getResponse().getStatusCode());
    }

    public void on500Response(ResponseEvent event, ActivityContextInterface aci) {
        registrationFailed(event.getResponse().getStatusCode());
    }

    public void on600Response(ResponseEvent event, ActivityContextInterface aci) {
        registrationFailed(event.getResponse().getStatusCode());
    }

    public void onTransactionTimeout(TimeoutEvent event, ActivityContextInterface aci) {
        registrationFailed(Response.REQUEST_TIMEOUT);
    }

    public void onConnectionUpEvent(ConnectionUpEvent event, ActivityContextInterface aci) {
        info("connection up: " + event.getConnection());
    }

    public void onConnectionDownEvent(ConnectionDownEvent event, ActivityContextInterface aci) {
        info("connection down: " + event.getConnection() + ", permanent=" + event.isPermanent());
    }

    private void register() {
        try {
            sendRequest(createRegister(3600), true);
        } catch (SipException e) {
            severe("failed to send registration request", e);
            detachAllActivities(); // SBB will be cleaned up
        }
    }

    private void unregister() {
        try {
            sendRequest(createRegister(0), false); // don't care about response, don't attach
            stopRegistrationExpiryTimer();
        } catch (SipException e) {
            warn("failed to send unregistration request", e);
        } finally {
            detachAllActivities(); // SBB will be cleaned up
        }
    }

    private void registrationSuccessful(Response response) {
        ContactHeader contact = (ContactHeader) response.getHeader(ContactHeader.NAME);
        int expires = contact.getExpires();
        info("registration successful, status=" + response.getStatusCode() + ", expires=" + expires);
        startRegistrationExpiryTimer(expires - 10); // fire before expiry time so we have time to re-register
    }

    private void registrationFailed(int statusCode) {
        warn("registration failed, status=" + statusCode + ", unregistering");
        unregister(); // unregister to tear down connection (if any)
    }

    private void attachConnectionActivity(PersistentOutboundConnection conn) {
        if (conn == null) return;
        try {
            ActivityContextInterface aci = getSipACIFactory().getActivityContextInterface(conn);
            aci.attach(getSbbLocalObject());
        } catch (UnrecognizedActivityException e) {
            warn("failed to attach to connection activity", e);
        }
    }

    private Request createRegister(int expires) {
        Request register = (Request) registerRequestTemplate.clone();
        try {
            // Set branch
            ((ViaHeader)register.getHeader(ViaHeader.NAME)).setBranch(getSleeSipProvider().getNewBranch());

            // Set sequence
            ((CSeqHeader)register.getHeader(CSeqHeader.NAME)).setSequenceNumber(getSequence());
            setSequence(getSequence() + 1);

            // Set expires
            ((ContactHeader)register.getHeader(ContactHeader.NAME)).setExpires(expires);

            return register;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendRequest(Request request, boolean attach) throws SipException {
        ClientTransaction ct = getSipProvider().getNewClientTransaction(request);
        try {
            if (attach) {
                ActivityContextInterface aci = getSipACIFactory().getActivityContextInterface(ct);
                aci.attach(getSbbLocalObject());
            }
        } catch (UnrecognizedActivityException e) {
            throw new SipException("failed to attach", e);
        }
        ct.sendRequest();
    }

    private void startRegistrationExpiryTimer(int seconds) {
        ServiceActivity service = getServiceActivityFactory().getActivity();
        try {
            ActivityContextInterface serviceACI = getServiceActivityContextInterfaceFactory().getActivityContextInterface(service);
            info("start registration expiry timer, expire in " + seconds + " seconds");
            TimerID timer = getTimerFacility().setTimer(serviceACI, null, System.currentTimeMillis() + (seconds * 1000), new TimerOptions());
            setRegistrationExpiryTimer(timer);
        } catch (UnrecognizedActivityException e) { } // should never happen with service activity
    }

    private void stopRegistrationExpiryTimer() {
        TimerID timer = getRegistrationExpiryTimer();
        if (timer != null) {
            getTimerFacility().cancelTimer(timer);
            setRegistrationExpiryTimer(null);
        }
    }

    private Request buildRegisterTemplate(String registrarURI, String publicURI, String contactURI, String hostname, int port)
            throws ParseException, InvalidArgumentException {
        CSeqHeader cseq = getSipHeaderFactory().createCSeqHeader(0, Request.REGISTER);
        FromHeader from = getSipHeaderFactory().createFromHeader(getSipAddressFactory().createAddress(publicURI), null);
        ToHeader to = getSipHeaderFactory().createToHeader(getSipAddressFactory().createAddress(publicURI), null);
        MaxForwardsHeader max = getSipHeaderFactory().createMaxForwardsHeader(70);
        ViaHeader via = getSipHeaderFactory().createViaHeader(hostname, port, "tcp", null);
        Request register = getSipMessageFactory().createRequest(getSipAddressFactory().createURI(registrarURI),  Request.REGISTER,
                getSipProvider().getNewCallId(), cseq, from, to, Collections.singletonList(via), max);
        ContactHeader contact = getSipHeaderFactory().createContactHeader(getSipAddressFactory().createAddress(contactURI));
        contact.setParameter("+sip.instance", UUID);
        contact.setParameter("reg-id", String.valueOf(REG_ID));
        register.setHeader(contact);
        return register;
    }


    public abstract int getSequence();
    public abstract void setSequence(int seq);

    public abstract TimerID getRegistrationExpiryTimer();
    public abstract void setRegistrationExpiryTimer(TimerID timer);

    private Request registerRequestTemplate;

    private static final String UUID = "\"<urn:uuid:00000000-0000-0000-0000-000000000001>\""; // Note quotes around URN
    private static final int REG_ID = 1;
}
