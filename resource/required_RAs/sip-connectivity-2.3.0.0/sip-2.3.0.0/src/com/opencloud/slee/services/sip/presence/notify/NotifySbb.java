package com.opencloud.slee.services.sip.presence.notify;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.ActivityEndEvent;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.SbbContext;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.TraceLevel;
import javax.slee.nullactivity.NullActivity;
import javax.slee.serviceactivity.ServiceActivity;

import net.java.slee.resource.sip.DialogActivity;

import com.opencloud.javax.sip.LazyParsedMessage;
import com.opencloud.javax.sip.LazyParsingException;
import com.opencloud.slee.services.sip.presence.NotifyStateChangeEvent;
import com.opencloud.slee.services.sip.presence.PresenceAwareSbb;
import com.opencloud.slee.services.sip.presence.PresenceService;
import com.opencloud.slee.services.sip.presence.PresentityState;
import com.opencloud.slee.services.sip.presence.SubscriptionACI;

public abstract class NotifySbb extends PresenceAwareSbb {
    
protected String getTraceMessageType() { return "NotifySbb"; }
    
    public void setSbbContext(SbbContext context) {
        super.setSbbContext(context);
        try {
            Context myEnv = (Context) new InitialContext().lookup("java:comp/env");
            String clients = (String) myEnv.lookup("nonCompliantSubExpiryClients");
            expiryTimeNonCompliant = clients.split(",");
        } catch (Exception e) {
            severe("Could not set SBB context", e);
        } 
    }

    public void onSubscribeEvent(RequestEvent event, ActivityContextInterface aci) {
        
        if (isTraceable(TraceLevel.FINEST))
            finest("onSubscribeEvent: NotifySbb received request:\n" + event.getRequest());

        Request request = event.getRequest();
        ServerTransaction st = event.getServerTransaction();
        
        try {
            // Check that the event package is 'presence'.
            // RFC3265 section 3.1.6.1: the subscription should be refused with a 
            // '489 Bad Event' response if a different event package is received.
            if (request.getHeader(EventHeader.NAME) != null) {
                String eventType = ((EventHeader)request.getHeader(EventHeader.NAME)).getEventType();
                if (eventType.equals("presence")) {
                    if (isTraceable(TraceLevel.FINE)) fine("NotifySbb detected presence event for received request");
                } else {
                    if (isTraceable(TraceLevel.FINE)) fine("WARNING: NotifySbb detected a non-presence event for received SUBSCRIBE request");
                    sendErrorResponse(st, request, Response.BAD_EVENT);
                    tearDown();
                    return;
                }
            }

            URI uri = ((ToHeader)request.getHeader(ToHeader.NAME)).getAddress().getURI();
            String sipAddressOfRecord = getCanonicalAddress(uri);
            setSipAddressOfRecord(sipAddressOfRecord);
            
            Dialog d = getSipProvider().getNewDialog(st);
            ActivityContextInterface dAci = getSipACIFactory().getActivityContextInterface((DialogActivity)d);
            // Check the SUBSCRIBE request to see whether it has an Expiry header and whether the requested expires value is either
            //  0 or less than the set minimum expiry. If either is true terminate the dialog and the subscription (if one already exists).
            if (request.getExpires() != null && request.getExpires().getExpires() < MIN_EXPIRES) {
                // If the requested Expiry time is 0, it is a request to terminate the subscription; send a one-off NOTIFY
                if (request.getExpires().getExpires() == 0) {
                    sendInitialOKResponse(request, st, aci);
                    sendNotifyRequest(d, sipAddressOfRecord, getPresenceState(), SubscriptionStateHeader.TERMINATED);
                } else {
                    // the requested expiry is too short; send an 'Expiry time too short' error response
                    sendErrorResponse(st, request, Response.INTERVAL_TOO_BRIEF);
                }
                // remove the dialog and subscription
                tearDown();
                return;
                
            } else {
                // attach to the dialog activity
                dAci.attach(getSbbLocalObject());
                // Check to see whether there is already a subscription ACI for the subscribed presentity
                //  If there is, get a reference to it, otherwise create a new one
                ActivityContextInterface nullAci = getOrCreateSubscriptionACI(sipAddressOfRecord);
                // attach to the subscription ACI to get updates from the presence service
                nullAci.attach(getSbbLocalObject());
                
                // increment the count of sbbs attached to this aci
                SubscriptionACI subscriptionAci = asSbbActivityContextInterface(nullAci);
                subscriptionAci.setSubscriberCount(subscriptionAci.getSubscriberCount() + 1);
            }
            
            if (isTraceable(TraceLevel.FINER)) finer("created " + (d.isServer() ? "UAS" : "UAC") + " dialog [id=" + d.getDialogId() + "]");

            // Attach to the service activity, so we can clean up when service is deactivated.
            attachServiceActivity();

            // set tag of to header
            ToHeader to = (ToHeader) request.getHeader(ToHeader.NAME);
            if (to.getTag() == null) {
                to.setTag(Integer.toString((int)(Math.random() * 10000)));
            }

            // return an OK message back to the subscriber to indicate that the subscription has been received
            sendInitialOKResponse(request, st, dAci);

            // Send the initial NOTIFY with default value of 'CLOSED'
            if (isTraceable(TraceLevel.FINEST)) finest("NotifySbb sending initial NOTIFY request:");
            sendNotifyRequest(d, sipAddressOfRecord, PRESENTITY_OFFLINE, SubscriptionStateHeader.ACTIVE);
            
            // Check to see whether the user that this SUBSCRIBE is for has any presence info registered with the Presence Service
            PresentityState curPresenceState = getPresenceState();
            if (curPresenceState != null && !curPresenceState.equals(PRESENTITY_OFFLINE)) sendNewPresenceInfo(d, sipAddressOfRecord, curPresenceState);

        } catch (Exception e) {
            // should only get here if some system error occurs, rollback and try sending an error response
            warn("unable to process subscription, rolling back", e);
            getSbbContext().setRollbackOnly();
        }
    }

    private long validateExpireTime(Request request) {
        
        // Set the requestedExpires to a default value of 3600 before validating
        //  (the default value for presence subscriptions is specified as 3600 in RFC 3865 6.4)
        long requestedExpires = 3600;
        ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
        if (expiresHeader != null && expiresHeader.getExpires() >= 0) {
            requestedExpires = expiresHeader.getExpires();
        }
        
        // If the user agent accepts subscription expiry lengths given by the server, validate the requested expiry time
        if (isSubscriptionTimeoutCompliant(request)) {
            if (requestedExpires > MAX_EXPIRES) {
                // requested expiry too long, set to max-expires
                requestedExpires = MAX_EXPIRES;
            }
        }
        return requestedExpires;
    }

    private void sendInitialOKResponse(Request request, ServerTransaction st, ActivityContextInterface aci) {
        
        try {
            // return an OK message back to the subscriber
            Response response = getSipMessageFactory().createResponse(Response.OK, request);
            
            // set contacts
            ArrayList newContacts = new ArrayList(4);
            for (Iterator it = request.getHeaders(ContactHeader.NAME); it.hasNext(); ) {
                ContactHeader contact = (ContactHeader) it.next();
                newContacts.add(contact);
            }
            
            Iterator it = newContacts.iterator();
            while (it.hasNext()) {
                ContactHeader contact = (ContactHeader) it.next();
                response.addHeader(contact);
            }
            
            // get the requested expiry time, and if appropriate, check that it falls between the min and max expiry time specified for the sbb
            long requestedExpires = validateExpireTime(request);
            ExpiresHeader expiresHeader = getSipHeaderFactory().createExpiresHeader((int)requestedExpires);
            response.addHeader(expiresHeader);
            
            st.sendResponse(response);
            if (isTraceable(TraceLevel.FINEST))
                finest("NotifySbb sending initial OK response:\n" + response);
            
            // Set up the subscription expiry timer
            if (requestedExpires > 0) {
                long expires = System.currentTimeMillis() + (requestedExpires * 1000);
                TimerID subscriptionTimerId = getTimerFacility().setTimer(aci, null, expires, new TimerOptions());
                setSubscriptionTimerId(subscriptionTimerId);
            }
            
        } catch (Exception e) {
            // should only get here if some system error occurs, rollback and try sending an error response
            warn("unable to process subscription, rolling back", e);
            getSbbContext().setRollbackOnly();
        }
    }

    private void sendErrorResponse(ServerTransaction st, Request request, int errorCode) {
        try {
            Response response = getSipMessageFactory().createResponse(errorCode, request);
            if (errorCode == Response.INTERVAL_TOO_BRIEF) {
                response.setHeader(getSipHeaderFactory().createMinExpiresHeader(((int)MIN_EXPIRES)));
            }
            st.sendResponse(response);
            if (isTraceable(TraceLevel.FINE)) fine("Sending bad event response: " + response);
        } catch (ParseException e) {
            warn("error parsing request", e);
        } catch (SipException e) {
            warn("SipException encountered", e);
        } catch (Exception e) {
            warn("Error encountered sending error response", e);
        }
    }

    public void sendNotifyRequest(Dialog d, String addressOfRecord, PresentityState presentityState, String subscriptionState) {

        try {
            Request notifyRequest = d.createRequest(Request.NOTIFY);
            
            // Set up the various headers that are required for the message
            HeaderFactory headerFactory = getSipHeaderFactory();
            
            // Subscription-state header
            SubscriptionStateHeader stateHeader;
            if (subscriptionState.equals(SubscriptionStateHeader.ACTIVE)){
                stateHeader = headerFactory.createSubscriptionStateHeader(SubscriptionStateHeader.ACTIVE);
            } else if (subscriptionState.equals(SubscriptionStateHeader.TERMINATED)) {
                stateHeader = headerFactory.createSubscriptionStateHeader(SubscriptionStateHeader.TERMINATED);
                stateHeader.setParameter("reason", SubscriptionStateHeader.TIMEOUT);
            } else {
                stateHeader = headerFactory.createSubscriptionStateHeader(subscriptionState);
            }
            notifyRequest.addHeader(stateHeader);
            
            // Event header
            EventHeader eventHeader = headerFactory.createEventHeader("presence");
            notifyRequest.addHeader(eventHeader);
            
            // pidf xml for message body
            String presXml = generatePresenceXml(addressOfRecord, presentityState);
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "pidf+xml");
            notifyRequest.setContent(presXml, contentTypeHeader);
            
            // Get the client transaction to send the request on, and the aci to attach the sbb to
            ClientTransaction ct = getSipProvider().getNewClientTransaction(notifyRequest);
            ActivityContextInterface ctAci = super.getSipACIFactory().getActivityContextInterface(ct);
            ctAci.attach(getSbbLocalObject());
            ct.sendRequest();
                        
            if (isTraceable(TraceLevel.FINEST))
                finest("NotifySbb sending NOTIFY request:\n" + notifyRequest);
            
        } catch (Exception e) {
            warn("Error sending NOTIFY request", e);
        }
    }

    private String generatePresenceXml (String uri, PresentityState status) {
        
        int tupleId = (int)(Math.random()*10000);
        
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<presence xmlns=\"urn:ietf:params:xml:ns:pidf\" entity=\"");
        xml.append(uri).append("\">");
        xml.append("<tuple id=\"");
        xml.append(tupleId);
        xml.append("\">");
        xml.append("<status><basic>");
        xml.append(status.getBasicState());
        xml.append("</basic>");
        xml.append("<note>");
        if (status.getNoteState() != null) xml.append(status.getNoteState());
        xml.append("</note></status>");
        xml.append("</tuple></presence>");
        
        return xml.toString();
    }
    
    private void sendNewPresenceInfo(Dialog d, String addressOfRecord, PresentityState state) {
        sendNotifyRequest(d, addressOfRecord, state, SubscriptionStateHeader.ACTIVE);
    }
    
    public void onNotifyStateChangeEvent(NotifyStateChangeEvent event, ActivityContextInterface aci) {
        
        if (isTraceable(TraceLevel.FINEST)) finest("NotifyStateChangeEvent event received by NotifySbb");
        
        try {       
            Dialog d = getCurrentDialog();
            
            if (event.getSipAddressOfRecord().equalsIgnoreCase(this.getSipAddressOfRecord())) {
                // Have received notification from presence service that the subscribed presentity's presence information 
                // has changed, so send the subscriber 
                // as recorded in the CMP. If different, notify the subscriber
                sendNewPresenceInfo(d, event.getSipAddressOfRecord(), event.getPresenceState());
            }
        } catch (Exception e) {
            warn("Encountered an error in NotifySbb onNotifyStateChangeEvent:", e);
        }
        
    }
    
    public void onRefreshTimer(TimerEvent timerEvent, SubscriptionACI aci) {
        
        try {
            // The subscription has expired. Send a final NOTIFY and tear down the sbb.
            if (isTraceable(TraceLevel.FINEST)) finest("Subscription timer has expired. Original request for this dialog is:\n" + getCurrentDialog().getFirstTransaction().getRequest());
            if (isTraceable(TraceLevel.FINEST)) finest("Sending final NOTIFY and tearing down sbb");
            sendNotifyRequest(getCurrentDialog(), getSipAddressOfRecord(), (getPresenceState() == null ? PRESENTITY_OFFLINE : getPresenceState()), SubscriptionStateHeader.TERMINATED);
        } catch (CreateException e) {
            warn("Error fetching presence state from Presence Service", e);
        } finally {
            tearDown();
        }
    }
    
    public void onTransactionTimeOutEvent(TimeoutEvent event, ActivityContextInterface aci) {
        aci.detach(getSbbLocalObject());
        if (isTraceable(TraceLevel.FINEST)) finest("TransactionTimeOut event received by presence sbb.");
        Request request;
        Transaction tx;
        if (event.isServerTransaction()) {
            tx = event.getServerTransaction();
        } else {
            tx = event.getClientTransaction();
        }
        request = tx.getRequest();
        
        if (isTraceable(TraceLevel.FINEST)) finest("Original request that timed out is:\n" + request);
        // If a request has timed out, then the client user agent is no longer listening on the current dialog, so cancel timer and tear down the sbb
        getTimerFacility().cancelTimer(getSubscriptionTimerId());
        tearDown();
    }
    
    public void onOKResponseEvent(ResponseEvent response, ActivityContextInterface aci) {
        aci.detach(getSbbLocalObject());
        if (isTraceable(TraceLevel.FINEST)) finest("Response event received by NotifySbb :\n" + response.getResponse());
    }
    
    public void onInDialogSubscribeEvent(RequestEvent event, ActivityContextInterface aci) {
        try {
            if (isTraceable(TraceLevel.FINEST)) finest("InDialog SUBSCRIBE event received by NotifySbb: \n" + event.getRequest());
            
            // If this method is reached, the subscriber has attempted to refresh the subscription,
            //  so kill the subscription termination timer, and send a new 200 OK response.
            //  The timer will be reset as part of the sending of the response.
            getTimerFacility().cancelTimer(getSubscriptionTimerId());
            sendInitialOKResponse(event.getRequest(), event.getServerTransaction(), aci);
            
            // Check whether this was a request to unsubscribe this subscription.
            if (event.getRequest().getExpires() != null && event.getRequest().getExpires().getExpires() == 0) {
                // If it was, send a dialog-ending NOTIFY request and tear down the subscription
                sendNotifyRequest(getCurrentDialog(),getSipAddressOfRecord(), getPresenceState(), SubscriptionStateHeader.TERMINATED);
                tearDown();     
            } else {
                // If not, send the current subscribed presentity's presence information
                sendNotifyRequest(getCurrentDialog(),getSipAddressOfRecord(), getPresenceState(), SubscriptionStateHeader.ACTIVE);
            }
        } catch (CreateException e) {
            warn("Error fetching presence state from Presence Service", e);
        }
            
    }

    public void onActivityEndEvent(ActivityEndEvent event, ActivityContextInterface aci) {
        if (aci.getActivity() instanceof ServiceActivity) {
            // Our service is being deactivated. Clean up.
            tearDown();
        }
    }

    private Dialog getCurrentDialog() {
        ActivityContextInterface[] acis = getSbbContext().getActivities();
        if (acis != null) {
            for (int i = 0; i < acis.length; i ++) {
                if (acis[i].getActivity() instanceof Dialog) {
                    return (Dialog) acis[i].getActivity();
                }
            }
        }
        throw new IllegalStateException("no dialog attached");
    }
    
    private boolean isSubscriptionTimeoutCompliant(Request request) {
        // Check whether the user agent will respect updated subscription expiry times
        boolean isCompliant = true;
        try {
            if (request.getHeader(UserAgentHeader.NAME) != null) {
                ListIterator iter = ((UserAgentHeader)request.getHeader(UserAgentHeader.NAME)).getProduct();
                while (iter.hasNext()) {
                    if (!agentIsCompliant((String)iter.next())) isCompliant = false;
                }
            }
        } catch (LazyParsingException e) {
            // if the user agent uses an illegal product name (refer RFC 3261 section 25), use the unparsed text of the header 
            if (!agentIsCompliant(((LazyParsedMessage)request).getUnparsedHeaderValue(UserAgentHeader.NAME))) isCompliant = false;
        }
        return isCompliant;
    }
    
    private boolean agentIsCompliant (String userAgentProduct) {
        
        for (int i = 0; i < expiryTimeNonCompliant.length; i++) {
            if (expiryTimeNonCompliant[i].equalsIgnoreCase(userAgentProduct)) {
                if (isTraceable(TraceLevel.FINEST)) finest("Found a non-compliant user-agent: " + userAgentProduct);
                return false;
            }
        }
        return true;
    }
    
    private PresenceService getPresenceService() throws CreateException {
        ChildRelation rel = getPresenceServiceChildRelation();
        return (PresenceService) rel.create();
    }
    
    private PresentityState getPresenceState(String uri) throws CreateException {
        if (isTraceable(TraceLevel.FINEST)) finest("Searching for PresentityState via Presence Service using: " + uri + " for search key...");
        return getPresenceService().getPresenceState(uri);
    }
    
    private PresentityState getPresenceState() throws CreateException {
        return getPresenceState(getSipAddressOfRecord());
    }
    
    private void tearDown() {
        
        if (getSubscriptionTimerId() != null) getTimerFacility().cancelTimer(getSubscriptionTimerId());
        
        ActivityContextInterface[] acis = getSbbContext().getActivities();
        for (int i = 0; i < acis.length; i++) {
            if (acis[i].getActivity() instanceof Dialog) {
                acis[i].detach(getSbbLocalObject());
                ((Dialog) acis[i].getActivity()).delete();
            } else if (acis[i].getActivity() instanceof NullActivity) {
                SubscriptionACI subAci = (SubscriptionACI) acis[i];
                subAci.detach(getSbbLocalObject());
                subAci.setSubscriberCount(subAci.getSubscriberCount()-1);
                if (subAci.getSubscriberCount() == 0) {
                    removeSubscriptionACI(getSipAddressOfRecord());
                }
            } else {
                acis[i].detach(getSbbLocalObject());
            }
        }
    }
    
    private static PresentityState PRESENTITY_OFFLINE = new PresentityState(PresentityState.CLOSED, null);
        
    public abstract ChildRelation getPresenceServiceChildRelation();
    public abstract SubscriptionACI asSbbActivityContextInterface(ActivityContextInterface aci);
        
    // TODO configurable
    private static final long MIN_EXPIRES = 20;
    private static final long MAX_EXPIRES = 3600;

    private String[] expiryTimeNonCompliant;
    
    public abstract String getSipAddressOfRecord();
    public abstract void setSipAddressOfRecord(final String sipAddressOfRecord);
    
    public abstract TimerID getSubscriptionTimerId();
    public abstract void setSubscriptionTimerId(TimerID timerId);

}
