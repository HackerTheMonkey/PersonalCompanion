package com.opencloud.slee.services.sip.common;

/**
 * Exception used to tell caller that a SIP error response should be sent out
 * on the current server transaction. The status code for the response sent 
 * should be the same as getStatusCode()
 */
public class SipSendErrorResponseException extends Exception {
        
    public SipSendErrorResponseException(String msg, int statusCode) {
        super(msg);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() { return statusCode; }

    final private int statusCode;
}
