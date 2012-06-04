package com.opencloud.slee.services.sip.common;

import com.opencloud.javax.sip.slee.OCSleeSipProvider;
import com.opencloud.javax.sip.slee.OCSipActivityContextInterfaceFactory;
import com.opencloud.javax.sip.header.OCHeaderFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sip.*;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.SbbContext;
import javax.slee.InitialEventSelector;
import javax.slee.facilities.TraceLevel;
import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * Base class for SIP services
 */
public abstract class OCSipSbb extends BaseSbb {

    public void setSbbContext(SbbContext context) {
        super.setSbbContext(context);
        try {
            Context myEnv = (Context) new InitialContext().lookup("java:comp/env");
            String providerName = (String) myEnv.lookup("sipProviderName");
            String factoryName = (String) myEnv.lookup("sipACIFactoryName");
            sipSbbInterface = (OCSleeSipProvider) myEnv.lookup(providerName);
            sipACIFactory = (OCSipActivityContextInterfaceFactory) myEnv.lookup(factoryName);
        } catch (NamingException ne) {
            severe("Could not set SBB context", ne);
        }
    }

    protected final OCSleeSipProvider getSleeSipProvider() { return sipSbbInterface; }
    protected final OCSipActivityContextInterfaceFactory getSipACIFactory() { return sipACIFactory; }
    protected final SipProvider getSipProvider() { return sipSbbInterface; }
    protected final AddressFactory getSipAddressFactory() { return sipSbbInterface.getAddressFactory(); }
    protected final HeaderFactory getSipHeaderFactory() { return sipSbbInterface.getHeaderFactory(); }
    protected final MessageFactory getSipMessageFactory() { return sipSbbInterface.getMessageFactory(); }

    protected final OCHeaderFactory getOCHeaderFactory() { return sipSbbInterface.getOCHeaderFactory(); }

    /**
     * Get the canonical form of the URI, sip:user@host[:port].
     * @param uri
     * @return the canonicalized URI as a string. 
     */
    public static String getCanonicalAddress(URI uri) {
        if (!uri.isSipURI()) throw new IllegalArgumentException("URI not a SIP URI");
        SipURI sipuri = (SipURI) uri;
        StringBuffer sb = new StringBuffer();
        sb.append(sipuri.getScheme()).append(':');
        sb.append(sipuri.getUser()).append('@').append(sipuri.getHost());
        // Only put port in if we need to (ie. it is non-standard).
        // So sip:fred@abc.com:5060 becomes sip:fred@abc.com,
        // and sips:bob@abc.com:5061 becomes sips:bob@abc.com.
        int port = sipuri.getPort();
        if (port != -1) {
            if (sipuri.isSecure() && port != 5061) sb.append(':').append(port);
            else if (port != 5060) sb.append(':').append(port);
        }
        return sb.toString();
    }

    /**
     * Generate a custom convergence name so that CANCELs will match their
     * respective INVITEs, so the CANCEL event will go to the same root SBB entity
     * that processed the INVITE.<br>
     * CN format is Call-ID:branch:CSeq.<br>
     * For other methods, use ActivityContext.
     */
    public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
        Object event = ies.getEvent();
        if (event instanceof RequestEvent) {
            Request request = ((RequestEvent)event).getRequest();

            String method = request.getMethod();

            if (method.equals(Request.INVITE) || method.equals(Request.CANCEL)) {
                String callId = ((CallIdHeader)request.getHeader(CallIdHeader.NAME)).getCallId();
                String branch = ((ViaHeader)request.getHeader(ViaHeader.NAME)).getBranch();
                int seq = ((CSeqHeader)request.getHeader(CSeqHeader.NAME)).getSequenceNumber();
                cnBuffer.setLength(0);
                cnBuffer.append(callId);
                cnBuffer.append(':').append(branch);
                cnBuffer.append(':').append(seq);
                ies.setCustomName(cnBuffer.toString());
            }
            else {
                ies.setActivityContextSelected(true);
            }
        }
        return ies;
    }

    public void sendResponse(ServerTransaction st, int statusCode) {
        sendResponse(st, statusCode, null);
    }

    public void sendResponse(ServerTransaction st, int statusCode, Header[] headers) {
        try {
            Response response = getSipMessageFactory().createResponse(statusCode, st.getRequest());
            if (headers != null) {
                for (int i = 0; i < headers.length; i++) {
                    response.addHeader(headers[i]);
                }
            }
            sendResponse(st, response);
        } catch (Exception e) {
            warn("unable to send response", e);
        }
    }

    public void sendResponse(ServerTransaction st, Response response) throws SipException, InvalidArgumentException {
        if (st == null) {
            if (isTraceable(TraceLevel.FINEST)) finest("sending stateless response:\n" + response);
            getSipProvider().sendResponse(response);
        }
        else {
            if (isTraceable(TraceLevel.FINEST)) finest("sending response:\n" + response);
            st.sendResponse(response);
        }
    }

    /**
     * Parse an env-entry string containing a comma-separated list of domain names.
     * @param myDomains a comma-separated list of domains, e.g. "opencloud.com,opencloud.co.nz".
     * @return an array of domain name strings
     * @throws IllegalArgumentException if the domains string is null or empty
     */
    public static String[] parseDomains(String myDomains) {
        if ((myDomains == null) || myDomains.length() == 0) {
            throw new IllegalArgumentException("invalid domains env-entry");
        }
        StringTokenizer st = new StringTokenizer(myDomains, ",");
        ArrayList<String> domains = new ArrayList<String>(2);
        while (st.hasMoreTokens()) {
            domains.add(st.nextToken().trim());
        }
        if (domains.size() == 0) throw new IllegalArgumentException("invalid domains env-entry");
        return domains.toArray(new String[domains.size()]);
    }

    /**
     * Determine if the URI is local to one of the given domain names.
     * @param uri a sip, tel or other URI
     * @param domains array of domain names that are considered local domains
     * @return {@code true} if the URI is a SIP URI and its host part matches (or is a subdomain of)
     * one of the domain names.
     */
    public static boolean isLocalDomain(URI uri, String[] domains) {
        if (!uri.isSipURI()) return false;

        // go through our list of domains, try to find match
        String host = ((SipURI)uri).getHost();
        for (String domain : domains) if (isInDomain(host, domain)) return true;
        return false;
    }

    private static boolean isInDomain(String host, String domain) {
        host = host.toLowerCase();
        domain = domain.toLowerCase();
        return host.endsWith(domain) &&
                (host.length() == domain.length() ||                       // host matches domain exactly, or
                 host.charAt(host.length() - domain.length() - 1) == '.'); // host is subdomain of domain
    }

    private OCSleeSipProvider sipSbbInterface;
    private OCSipActivityContextInterfaceFactory sipACIFactory;

    private final StringBuffer cnBuffer = new StringBuffer(96);

}
