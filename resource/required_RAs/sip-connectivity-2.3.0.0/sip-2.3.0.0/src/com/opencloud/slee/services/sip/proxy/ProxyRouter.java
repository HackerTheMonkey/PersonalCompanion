package com.opencloud.slee.services.sip.proxy;

import com.opencloud.javax.sip.header.FlowIDHeader;
import com.opencloud.slee.services.sip.common.OCSipSbb;
import com.opencloud.slee.services.sip.common.SimpleMessageDigest;
import com.opencloud.slee.services.sip.common.SipSendErrorResponseException;
import com.opencloud.slee.services.sip.location.FlowID;
import com.opencloud.slee.services.sip.location.LocationService;
import com.opencloud.slee.services.sip.location.Registration;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.facilities.Tracer;
import javax.slee.facilities.TraceLevel;
import java.text.ParseException;
import java.util.*;

/**
 * Utility class for apps that need proxy-like routing behaviour
 */
public class ProxyRouter {

    public ProxyRouter(ProxyConfig config, Tracer tracer) {
        this.config = config;
        this.tracer = tracer;
    }

    public Request routeRequest(final Request request) throws SipSendErrorResponseException {
        Request[] requests = routeRequest(request, null);
        return requests[0];
    }

    public Request[] routeRequest(final Request request, URI[] targets) throws SipSendErrorResponseException {
        if (request == null) throw new NullPointerException("request");

        Request copiedRequest = (Request) request.clone();

        final String method = request.getMethod();
        final boolean dialogCreating = method.equals(Request.INVITE) || method.equals(Request.SUBSCRIBE);

        // Did the request come in on an incoming flow? If so, need to record-route appropriately.
        FlowIDHeader flowHeader = (FlowIDHeader) request.getHeader(FlowIDHeader.NAME);
        final FlowID incomingFlow = flowHeader == null ? null : FlowID.fromString(flowHeader.getFlowID());

        // 16.3 Request Validation
        validateRequest(copiedRequest);
        // 16.4 Route Information Preprocessing
        routePreProcess(copiedRequest);

        // 16.5 Determine Request Targets
        Target[] resolvedTargets;
        if (targets == null || targets.length == 0) {
            resolvedTargets = determineRequestTargets(new URI[] { copiedRequest.getRequestURI() });
        }
        else {
            resolvedTargets = determineRequestTargets(targets);
        }

        Request[] processedRequests = new Request[resolvedTargets.length];
        for (int i = 0; i < resolvedTargets.length; i++) {

            // 16.6 Request Forwarding
            // 1. Copy request
            Request requestToSend = (i == resolvedTargets.length - 1)
                    ? copiedRequest // don't copy it again if this is the last iteration
                    : (Request) copiedRequest.clone();

            // 2. Request-URI
            requestToSend.setRequestURI(resolvedTargets[i].getURI());

            // 2a. Check if next Route header is one of ours and specifies a flow-id
            FlowID outgoingFlow = checkRouteForOutgoingFlow(requestToSend);
            if (outgoingFlow == null) outgoingFlow = resolvedTargets[i].getFlow();
            // 2b. Flow-ID - request will be sent on this flow
            if (outgoingFlow != null) {
                requestToSend.setHeader(config.getSipHeaderFactory().createFlowIDHeader(outgoingFlow.toString()));
            }

            // 3. Max-Forwards
            decrementMaxForwards(requestToSend);
            // 4. Record-Route (may add 2 R-R headers if using flows)
            if (dialogCreating && config.isRecordRouteEnabled()) addRecordRouteHeader(requestToSend, incomingFlow, outgoingFlow);
            // 5. Add Additional Header Fields
            // 6. Postprocess routing information
            // 7. Determine Next-Hop Address, Port and Transport
            // Stack does this automatically
            // 8. Add a Via header field value
            addViaHeader(requestToSend);

            processedRequests[i] = requestToSend;
        }
        return processedRequests;
    }

    protected void validateRequest(Request request) throws SipSendErrorResponseException {
        tracer.finer("validateRequest");
        // 1. Reasonable syntax
        // 2. URI scheme
        // 3. Max-Forwards
        checkMaxForwards(request);
        // 4. Loop Detection
        if (config.isLoopDetectionEnabled()) loopDetection(request);
        // 5. Proxy-Require - TBD
        // 6. Proxy-Authorization - TBD
    }

    protected void checkMaxForwards(Request request) throws SipSendErrorResponseException {
        MaxForwardsHeader max = (MaxForwardsHeader) request.getHeader(MaxForwardsHeader.NAME);
        if (max == null) return;

        int maxForwards = max.getMaxForwards();

        if (maxForwards > 0) {
            tracer.finer("checkMaxForwards: OK");
            return;
        }
        else {
            tracer.finer("checkMaxForwards: Too many hops");
            // MAY respond to OPTIONS, otherwise return 483 Too Many Hops
            // At the moment this proxy does not implement OPTIONS itself
            // TODO support OPTIONS ;-)
            throw new SipSendErrorResponseException("Too many hops", Response.TOO_MANY_HOPS);
        }

    }

    protected void loopDetection(Request request) throws SipSendErrorResponseException {
        // check if this request looped back around, by looking for any Vias that we
        // inserted previously, that have the same branch value
        String calculatedBranch = generateBranchParameter(request);
        for (Iterator it = request.getHeaders(ViaHeader.NAME); it.hasNext(); ) {
            ViaHeader via = (ViaHeader)it.next();
            String viaBranch = via.getBranch();
            if (viaBranch == null) continue; // old UA with no branch, nothing we can do

            if (viaBranch.equals(calculatedBranch) && config.getSipProvider().isLocalHostname(via.getHost())) {
                if (tracer.isTraceable(TraceLevel.FINER)) tracer.finer("loopDetection: loop detected, sending " + Response.LOOP_DETECTED + " response");
                throw new SipSendErrorResponseException("Loop detected", Response.LOOP_DETECTED);
            }
        }
        tracer.finer("loopDetection: OK, no loop detected");
    }

    protected void routePreProcess(Request request) {
        URI requestURI = request.getRequestURI();

        if (requestURI.isSipURI() && isProxySipURI((SipURI)requestURI)) {

            // The request was directed explicitly to this proxy.
            // This means the client is a strict router - so we must replace
            // request-URI with last value in Route header field.
            // If there is no route header then this is a malformed request.
            RouteHeader lastRoute = null;
            int numRouteHeaders = 0;
            ListIterator it = request.getHeaders(RouteHeader.NAME);
            // find last routeHeader and remove it from list
            while (it.hasNext()) {
                RouteHeader r = (RouteHeader) it.next();
                if (!it.hasNext()) {
                    lastRoute = r;
                    it.remove();
                }
                else numRouteHeaders++;
            }

            if (lastRoute == null) return; // no route headers, nothing to do

            // We removed the only route header, delete the header from the message
            if (numRouteHeaders == 0) request.removeHeader(RouteHeader.NAME);

            if (tracer.isTraceable(TraceLevel.FINER))
                tracer.finer("routePreProcess: strict routing, replace request-URI with last Route header: " + lastRoute.getAddress().getURI());

            request.setRequestURI(lastRoute.getAddress().getURI());
        }
        else {

            // Loose routing
            // From RFC3261 16.4:
            // If the first value in the Route header field indicates this proxy,
            // the proxy MUST remove that value from the request.
            Iterator routeHeaders = request.getHeaders(RouteHeader.NAME);
            if (routeHeaders.hasNext()) {
                RouteHeader r = (RouteHeader) routeHeaders.next();
                // is this route header for our hostname & port?
                URI uri = r.getAddress().getURI();

                if (uri.isSipURI() && isProxySipURI((SipURI)uri)) {
                    if (tracer.isTraceable(TraceLevel.FINER))
                        tracer.finer("routePreProcess: loose routing, remove top Route header: " + uri);
                    // remove this route header
                    routeHeaders.remove();
                    // If this was the last one, remove the header entirely
                    if (!routeHeaders.hasNext()) request.removeHeader(RouteHeader.NAME);
                }
            }
        }
    }

    /**
     * Determines target SIP URI(s) for request, using location service or
     * other criteria.
     * @param uris list of URIs we want to send to
     * @return a list of URIs
     */
    protected Target[] determineRequestTargets(URI[] uris) throws SipSendErrorResponseException {
        if (!config.useLocationService()) {
            Target[] targets = new Target[uris.length];
            for (int i = 0; i < targets.length; i++) targets[i] = new Target(uris[i]);
            return targets;
        }

        ArrayList targets = new ArrayList(4);

        for (int i = 0; i < uris.length; i++) {
            URI requestURI = uris[i];

            // TODO maddr behaviour
            if (requestURI.isSipURI() && OCSipSbb.isLocalDomain(requestURI, config.getProxyDomains())) {
                if (tracer.isTraceable(TraceLevel.FINER))
                    tracer.finer("determineRequestTargets: " + requestURI + " is local");
                // determine local SIP target(s) using location service etc
                List localTargets = findLocalTargets((SipURI)requestURI);
                if (localTargets == null || localTargets.isEmpty()) { // not found (or not currently registered)
                    if (tracer.isTraceable(TraceLevel.FINER))
                        tracer.finer("determineRequestTargets: no targets for: " + requestURI);
                }
                else {
                    if (tracer.isTraceable(TraceLevel.FINER))
                        tracer.finer("determineRequestTargets: target set for " + requestURI + ": " + localTargets);
                    targets.addAll(localTargets);
                }
            }
            else {
                // destination addr is outside our domain
                if (tracer.isTraceable(TraceLevel.FINER))
                    tracer.finer("determineRequestTargets: " + requestURI + " is outside our domain, forwarding");
                targets.add(new Target(requestURI));
            }

        }
        if (targets.isEmpty()) throw new SipSendErrorResponseException("User not registered", Response.TEMPORARILY_UNAVAILABLE);

        return (Target[]) targets.toArray(new Target[targets.size()]);
    }

    protected List findLocalTargets(SipURI uri) {
        List contacts = null;
        LocationService locationService = config.getLocationService();

        Registration reg = locationService.getRegistration(OCSipSbb.getCanonicalAddress(uri));
        if (reg == null) {
            return null; // user unknown
        }

        contacts = reg.getContacts();
        if (contacts.isEmpty()) {
            return null; // user known but not currently registered
        }

        if (config.isForkingEnabled()) {
            // return all known contacts
            ArrayList targets = new ArrayList(contacts.size());
            for (int i = 0; i < contacts.size(); i++) {
                Registration.Contact contact = (Registration.Contact) contacts.get(i);
                try {
                    SipURI contactURI = (SipURI) config.getSipAddressFactory().createURI(contact.getContactURI());
                    targets.add(new Target(contactURI, contact.getFlowID()));
                } catch (Exception e) {
                    tracer.warning("Failed to parse contact URI: " + contact.getContactURI(), e);
                }
            }
            return targets;
        }
        else {
            // find the contact with the highest q-value
            float bestQ = 0;
            int bestIndex = 0;
            for (int i = 0; i < contacts.size(); i++) {
                Registration.Contact contact = (Registration.Contact) contacts.get(i);
                if (contact.getQValue() > bestQ) {
                    bestQ = contact.getQValue();
                    bestIndex = i;
                }
            }
            Registration.Contact contact = (Registration.Contact) contacts.get(bestIndex);
            try {
                SipURI contactURI = (SipURI) config.getSipAddressFactory().createURI(contact.getContactURI());
                return Collections.singletonList(new Target(contactURI, contact.getFlowID()));
            } catch (ParseException e) {
                tracer.warning("Failed to parse contact URI: " + contact.getContactURI(), e);
                return Collections.EMPTY_LIST; // caller will notice empty list and deal with it
            }
        }
    }

    /**
     * Decrement the value of max-forwards.  If no max-forwards header present,
     * create a max-forwards header with the RFC3261 recommended default value (70).
     */
    protected void decrementMaxForwards(Request request) {
        MaxForwardsHeader max = (MaxForwardsHeader) request.getHeader(MaxForwardsHeader.NAME);
        try {
            if (max == null) {
                // add max-forwards with default 70 hops
                max = config.getSipHeaderFactory().createMaxForwardsHeader(70);
                request.setHeader(max);
            }
            else {
                // decrement max-forwards
                max.setMaxForwards(max.getMaxForwards() - 1);
            }
        } catch (InvalidArgumentException e) {
            // should not happen since we know max forwards values are positive
            throw new RuntimeException(e);
        }
    }

    protected void addRecordRouteHeader(Request request, FlowID incomingFlow, FlowID outgoingFlow) throws SipSendErrorResponseException {
        // If EITHER incomingFlow or outgoingFlow is set, we must do a "double Record-Route", so that
        // the proxy can select the correct flow for mid-dialog requests in either direction.
        // See http://www1.ietf.org/mail-archive/web/sip/current/msg14326.html.

        try {
            RecordRouteHeader[] rrs;
            if (incomingFlow != null || outgoingFlow != null) {
                if (tracer.isTraceable(TraceLevel.FINEST))
                    tracer.finest("addRecordRouteHeader: double R-R: incoming=" + incomingFlow + ", outgoing=" + outgoingFlow);
                rrs = createDoubleRR(request, incomingFlow, outgoingFlow);
            }
            else {
                rrs = new RecordRouteHeader[] { createRRHeader(request) };
            }

            // JAIN SIP should specify that Record-Route headers are automatically
            // added to the top (like Via headers) but unfortunately it doesn't,
            // so we have to fiddle with the headers ourselves here...

            // get existing headers (if any)
            ListIterator it = request.getHeaders(RecordRouteHeader.NAME);
            // insert our Record-Route headers first (replacing existing headers)
            for (int i = 0; i < rrs.length; i++) {
                if (i == 0) request.setHeader(rrs[i]);
                else request.addHeader(rrs[i]);
            }
            // re-insert existing headers
            while (it.hasNext()) {
                RecordRouteHeader rr = (RecordRouteHeader) it.next();
                request.addHeader(rr);
            }
        } catch (SipException e) {
            throw new SipSendErrorResponseException(e.getMessage(), Response.SERVER_INTERNAL_ERROR);
        }
    }

    private RecordRouteHeader[] createDoubleRR(Request request, FlowID incomingFlow, FlowID outgoingFlow) throws SipException {
        RecordRouteHeader rrs[] = new RecordRouteHeader[2];
        try {
            rrs[0] = createRRHeader(request);
            if (outgoingFlow != null) rrs[0].setParameter("flow", outgoingFlow.toString());
            rrs[1] = createRRHeader(request);
            if (incomingFlow != null) rrs[1].setParameter("flow", incomingFlow.toString());
            // headers in [outgoing, incoming] order so they will get added in correct order above...
            return rrs;
        } catch (ParseException e) {
            throw new SipException("failed to set flow parameter", e);
        }
    }

    private RecordRouteHeader createRRHeader(Request request) throws SipException {
        ViaHeader lastVia = (ViaHeader) request.getHeader(ViaHeader.NAME);
        SipURI myURI = config.getSipProvider().getLocalSipURI(lastVia.getTransport());
        myURI.setLrParam();
        Address myName = config.getSipAddressFactory().createAddress(myURI);
        RecordRouteHeader myHeader = config.getSipHeaderFactory().createRecordRouteHeader(myName);
        return myHeader;
    }

    private FlowID checkRouteForOutgoingFlow(Request request) {
        // If we previously double record-routed, there will be a Route header present that points
        // to us, and it may have a flow-id parameter.
        // If the top route points to us, remove it and return the flow-id, if any.
        Iterator routes = request.getHeaders(RouteHeader.NAME);
        if (!routes.hasNext()) return null;
        RouteHeader route = (RouteHeader) routes.next();
        if (isProxySipURI((SipURI)route.getAddress().getURI())) {
            // This is our route header, from a double R-R. Remove it.
            routes.remove();
            if (!routes.hasNext()) request.removeHeader(RouteHeader.NAME);
            String flowParam = route.getParameter("flow");
            FlowID flow = flowParam == null ? null : FlowID.fromString(flowParam); 
            if (tracer.isTraceable(TraceLevel.FINEST))
                tracer.finest("removed 2nd route header, flow=" + flow);
            return flow;
        }
        return null;
    }

    protected void addViaHeader(Request request) throws SipSendErrorResponseException {
        try {
            // Use the same transport given in the incoming request's Via header.
            // This assumes the underlying RA supports all transports (OCSIP RA is
            // deployed with TCP and UDP by default). If the transport is not known
            // then we will get an error when attempting to create the client txn.

            ViaHeader lastVia = (ViaHeader) request.getHeader(ViaHeader.NAME);
            String transport = lastVia.getTransport();

            // If loop detection enabled, create a Via header with our special branch param,
            // otherwise let stack create branch automatically
            String branch = config.isLoopDetectionEnabled() ? generateBranchParameter(request) : null;
            ViaHeader via = config.getSipProvider().getLocalVia(transport, branch);

            request.addHeader(via);
        } catch (Exception e) {
            throw new SipSendErrorResponseException(e.getMessage(), Response.SERVER_INTERNAL_ERROR);
        }
    }

    protected String generateBranchParameter(Request request) {
        // Calculates a hash based on all fields relevant to the proxy
        // so that when receiving a request, the proxy can detect if any
        // fields have changed (if not, request has looped).
        // Based on recommendation in RFC3261 16.6 step 8, except Via
        // header is not part of the hash, since this will change even
        // on a looped request (see http://bugs.sipit.net/show_bug.cgi?id=648).
        SimpleMessageDigest md = new SimpleMessageDigest();
        // Request-URI
        md.update(request.getRequestURI().toString());
        // Call-ID
        md.update(((CallIdHeader)request.getHeader(CallIdHeader.NAME)).getCallId());
        // From tag
        md.update(((FromHeader)request.getHeader(FromHeader.NAME)).getTag());
        // To tag
        md.update(((ToHeader)request.getHeader(ToHeader.NAME)).getTag());
        // sequence number
        md.update(((CSeqHeader)request.getHeader(CSeqHeader.NAME)).getSequenceNumber());

        for (Iterator it = request.getHeaders(RouteHeader.NAME); it.hasNext(); ) {
            md.update(it.next().toString());
        }
        for (Iterator it = request.getHeaders(ProxyRequireHeader.NAME); it.hasNext(); ) {
            md.update(it.next().toString());
        }
        for (Iterator it = request.getHeaders(ProxyAuthorizationHeader.NAME); it.hasNext(); ) {
            md.update(it.next().toString());
        }

        return "z9hG4bK" + md.digestHex(); // RFC3261 magic cookie required for branch IDs
    }

    // Is the SIP URI addressed to this proxy?
    protected boolean isProxySipURI(SipURI uri) {
        final boolean isLocal = config.getSipProvider().isLocalSipURI(uri);
        if (tracer.isTraceable(TraceLevel.FINER)) {
            tracer.finer(uri + (isLocal ? " is " : " is not ") + "a local address for this proxy");
        }
        return isLocal;
    }

    private static final class Target {
        Target(URI uri) {
            this(uri, null);
        }

        Target(URI uri, FlowID flow) {
            this.uri = uri;
            this.flow = flow;
        }

        URI getURI() { return uri; }
        FlowID getFlow() { return flow; }

        public String toString() {
            return "Target[<" + uri + (flow != null ? ">,flow=" + flow + "]" : ">]");
        }

        private final URI uri;
        private final FlowID flow;
    }

    private final ProxyConfig config;
    private final Tracer tracer;
}
