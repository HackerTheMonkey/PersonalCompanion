package com.opencloud.slee.services.sip.proxy;

import javax.slee.SbbLocalObject;
import javax.sip.ServerTransaction;
import javax.sip.ClientTransaction;
import javax.sip.message.Response;

/**
 * The proxy response listener is responsible for forwarding any
 * provisional and final responses back to the caller. Implementations
 * may modify responses as appropriate.
 */
public interface ProxyResponseListener extends SbbLocalObject {

    /**
     * The listener SBB should be attached to a SIP server transaction
     * ACI so that it can send asynchronous responses. This is a convenience
     * method so that the Proxy SBB can access the original server transaction
     * and request, when generating responses etc.
     */
    ServerTransaction getServerTransaction();

    /**
     * Forward a response back to the caller, via the listener SBB. The
     * listener should send the response via the server transaction it is attached
     * to, or statelessly via SipProvider.
     * @param response the response from the proxy. The proxy will have already
     * modified the Via header, but the listener SBB can add additional headers.
     */
    void forwardResponse(Response response);

    /**
     * The proxy has received a response to the proxied request. This callback
     * is invoked so that the application can perform any processing before
     * the proxy forwards the response, such as creating dialogs.
     * @param ct the client tranaction the response was received on.
     * @param response the response to the forwarded request, as received by the
     * proxy. The proxy will not have modified any headers yet.
     */
    void receivedResponse(ClientTransaction ct, Response response);
}
