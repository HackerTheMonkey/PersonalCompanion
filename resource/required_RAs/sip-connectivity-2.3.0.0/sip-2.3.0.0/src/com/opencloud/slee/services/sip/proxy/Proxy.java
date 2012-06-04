package com.opencloud.slee.services.sip.proxy;

import com.opencloud.slee.services.sip.common.SipSendErrorResponseException;

import javax.sip.message.Request;
import javax.sip.address.URI;
import javax.slee.SbbLocalObject;

/**
 * Generic SIP proxy local interface. May be used by other
 * SBBs to proxy SIP requests. Automatically handles manipulation
 * of Via, Route, Record-Route headers as per RFC3261 ch.16.
 * <p>
 * Each proxy SBB entity handles a single SIP transaction, and is removed
 * when the transaction is complete.
 */
public interface Proxy extends SbbLocalObject, ProxyResponseListener {

    /**
     * Determines if the Proxy inserts a Record-Route header so that
     * it stays on the path.
     * Default: true
     * @param recordRoute
     */
    void setRecordRoute(boolean recordRoute);

    /**
     * Determines if the proxy uses the
     * {@link com.opencloud.slee.services.sip.location.LocationService LocationService}
     * SBB to determine the target of a proxied request. If not, the Request-URI in
     * proxied requests will not be changed.
     * Default: true
     * @param useLocationService
     */
    void setUseLocationService(boolean useLocationService);

    /**
     * Determines if the proxy forks requests (sends requests to multiple targets
     * in parallel) if more than one target address is specified.
     * Default: false
     * @param fork
     */
    void setForking(boolean fork);

    /**
     * Proxy a request following standard proxy rules. Targets will be resolved
     * using the {@link com.opencloud.slee.services.sip.location.LocationService LocationService}
     * SBB that the Proxy was deployed with. If no local targets are found, the
     * Proxy will attempt to forward the request using standard routing rules.
     * Responses seen by the Proxy are passed to the listener SBB for forwarding.
     * @param callback
     * @param request the request to be proxied. The request object will NOT be modified.
     */
    void proxyRequest(ProxyResponseListener callback, final Request request);

    /**
     * Proxy a request following standard proxy rules, using the supplied target URIs.
     * If forking is disabled, the Proxy will only send to the first URI in the list.
     * Targets will be resolved using the {@link com.opencloud.slee.services.sip.location.LocationService LocationService}
     * SBB that the Proxy was deployed with. If no local targets are found, the Proxy
     * will attempt to forward the request using standard routing rules.
     * Responses seen by the Proxy are passed to the listener SBB for forwarding.
     * @param callback
     * @param request the request to be proxied. The request object will NOT be modified.
     * @param targets the target URIs that this request is to be sent to.
     */
    void proxyRequest(ProxyResponseListener callback, final Request request, URI[] targets);

    /**
     * Proxy a request statelessly to its default destination, don't care about
     * responses. This should be used for proxying ACKs, or proxying CANCELs that
     * don't match a previous INVITE transaction.
     * @param request  the request to be proxied. The request object will NOT be modified.
     */
    void proxyRequestStateless(final Request request);

    /**
     * Cancel any in-progress proxy client transactions. The caller should ensure that
     * CANCEL transactions match a previous INVITE transaction being proxied
     * by this Proxy SBB (using initial event selection rules or similar).
     * The caller should respond immediately to the CANCEL with a 200 OK.
     * Any active INVITE client transactions are cancelled, and will eventually
     * terminate when a 487 Request Terminated response is received from the
     * called party.
     * If the client transactions are non-INVITE transactions, the SBB does not send
     * a CANCEL, instead it just detaches from the client transaction activities so
     * no responses will be seen.
     * @return <code>true</code> if any matching INVITE client transactions were found
     * and cancelled, otherwise <code>false</code>.
     */
    boolean cancel();

    /**
     * Perform typical proxy request processing, but without actually forwarding
     * the request. This will perform the header manipulation on the request, as
     * if it was being proxied, but instead of forwarding the request to its
     * destination, the modified request is returned to the caller. This is a
     * convenience method for apps that need to route requests properly but want
     * to control the sending/receiving of requests/responses themselves, eg. B2BUA.
     * @param request the request to be proxied. The request object will NOT be modified.
     * @return the modified request, with Request-URI, Via, Route and Record-Route
     * modified as per normal proxy processing.
     * @throws SipSendErrorResponseException if the original request is not valid and
     * cannot be processed. The caller should construct an error response using
     * the error code in the exception.
     */
    Request requestProcessing(final Request request) throws SipSendErrorResponseException;

    /**
     * Perform typical proxy request processing, but without actually forwarding
     * the request. This will perform the header manipulation on the request, as
     * if it was being proxied, but instead of forwarding the request to its
     * destination, the modified request is returned to the caller. This is a
     * convenience method for apps that need to route requests properly but want
     * to control the sending/receiving of requests/responses themselves, eg. B2BUA.
     * <p>This method may be used by applications that want to do their own request forking,
     * rather than leaving it up to the proxy SBB.
     * @param request the request to be proxied. The request object will NOT be modified.
     * @param targets the desired target Request-URIs for the outgoing request.
     * This may be used by apps that want specify targets explicitly instead
     * of relying on the proxy. If <code>targets</code> is <code>null</code> or
     * empty, the Request-URI in the original request will be used, which is what
     * a proxy would do normally.<br>
     * If the {@link #setUseLocationService(boolean) useLocationService} field
     * is <code>true</code>, and the target Request-URI is in this proxy's domain,
     * the target URI will be looked up in the location service to find contact
     * addresses.
     * If {@link #setForking(boolean) forking} is enabled, all contact addresses
     * will be used as targets, otherwise only the highest preference contact address
     * will be used.
     * @return an array of modified requests, each with Request-URI, Via, Route and
     * Record-Route modified as per normal proxy processing. It is possible for this
     * method to return more requests than the number of targets, if {@link #setForking(boolean) forking}
     * was enabled, and some of the targets resolved to multiple contacts by the
     * location service.
     * @throws SipSendErrorResponseException if the original request is not valid
     * and cannot be processed. The caller should construct an error response using
     * the error code in the exception.
     */
    Request[] requestProcessing(final Request request, URI[] targets) throws SipSendErrorResponseException;
}
