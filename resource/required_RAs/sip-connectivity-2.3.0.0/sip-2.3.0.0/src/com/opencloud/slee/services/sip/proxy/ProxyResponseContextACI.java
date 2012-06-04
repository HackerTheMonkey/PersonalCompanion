package com.opencloud.slee.services.sip.proxy;

import javax.slee.ActivityContextInterface;
import javax.slee.facilities.TimerID;
import javax.sip.message.Response;

/**
 * Each client transaction initiated by the proxy gets a ResponseContextACI.
 * A proxy may be attached to zero or more of these. Together these
 * ACIs make up the logical "response context" as defined in RFC3261 ch.16.
 * When all client transactions have completed, the Proxy will select the
 * "best" response from the response context to return to the client.
 */
public interface ProxyResponseContextACI extends ActivityContextInterface {
    /**
     * The status code of a response received on this transaction. This will
     * be set to the last response status code received on the transaction,
     * including provisional 1xx responses. The proxy will also set the
     * status code explicitly if either:
     * - the client transaction times out (set to 408 Request Timeout)
     * - there is a network I/O error sending the request (set to 503 service unavailable)
     */
    public int getResponseStatusCode();
    public void setResponseStatusCode(int statusCode);

    /**
     * The to-tag returned by the upstream element in an error response.
     * Proxies SHOULD preserve the to-tag if it is set in an error response,
     * but MUST NOT change it if it was already present in the original
     * request. 
     */
    public String getErrorResponseToTag();
    public void setErrorResponseToTag(String toTag);

    // TODO need to save additional headers that might be returned in
    // error responses, eg. authorization, contact info...

    /**
     * The id of the "Timer C" timer for this client transaction. 
     */
    public TimerID getTimerC();
    public void setTimerC(TimerID id);
}
