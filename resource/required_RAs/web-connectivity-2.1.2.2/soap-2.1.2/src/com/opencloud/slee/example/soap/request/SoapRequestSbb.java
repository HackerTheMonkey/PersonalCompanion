package com.opencloud.slee.example.soap.request;

import com.opencloud.slee.resources.soap.OutgoingSOAPRequestActivity;
import com.opencloud.slee.resources.soap.SOAPProvider;
import com.opencloud.slee.resources.soap.SOAPResponse;
import com.opencloud.slee.resources.soap.SOAPActivityContextInterfaceFactory;
import com.opencloud.slee.services.common.BaseSbb;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.SbbContext;
import javax.slee.serviceactivity.ServiceStartedEvent;
import javax.slee.facilities.TraceLevel;
import javax.xml.soap.Name;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.net.URL;

/**
 * Simple Soap "Request" SBB - makes a SOAP request on activation and logs the response
 */
public abstract class SoapRequestSbb extends BaseSbb {
    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);
        try {
            Context myEnv = (Context) new InitialContext().lookup("java:comp/env");
            soapProvider = (SOAPProvider) myEnv.lookup("slee/resources/soap/provider");
            aciFactory = (SOAPActivityContextInterfaceFactory) myEnv.lookup("slee/resources/soap/acifactory");
            requestURL = new URL( (String)(myEnv.lookup("requestURL")) );
            sendSyncRequest = (Boolean) myEnv.lookup("sendSyncRequest");
        }
        catch (Exception e) {
            severe("Could not set SBB context", e);
        }
    }

    protected String getTracerName() {
        return "SoapRequestSbb";
    }

    /**
     * Handles service activation
     */
    public void onServiceStarted(ServiceStartedEvent event, ActivityContextInterface aci) {
        try {
            if (isFinestTraceable()) finest("onServiceStarted: received service started event");

            SOAPMessage message = soapProvider.getMessageFactory().createMessage();
            SOAPBody body = message.getSOAPBody();
            
            SOAPFactory soapFactory = soapProvider.getSOAPFactory();
            Name pingName = soapFactory.createName("ping");
            Name echoTextName = soapFactory.createName("echoText");
            Name timestampName = soapFactory.createName("timestamp");
            
            //Populate the body of the message
            SOAPBodyElement bodyElement = body.addBodyElement(pingName);
            SOAPElement timestamp = bodyElement.addChildElement(timestampName);
            timestamp.addTextNode(new Date().toString());
            SOAPElement echoTextElement = bodyElement.addChildElement(echoTextName);
            echoTextElement.addTextNode("test test test");
            
            if (isFineTraceable()) fine("onServiceStarted: sending SOAP Request to " + requestURL);
            if (isFinestTraceable()) finest("  message body: " + messageToString(message));

            if(sendSyncRequest) {
                SOAPResponse response = soapProvider.sendSyncRequest(requestURL, message);
                processResponse(response);
            }
            else {
                OutgoingSOAPRequestActivity activity = soapProvider.sendRequest(requestURL, message);
                ActivityContextInterface soapACI = aciFactory.getActivityContextInterface(activity);
                soapACI.attach(getSbbLocalObject());
            }
        } catch (Exception e) {
            warning("Unable to send SOAP request", e);
        }
    }

    /**
     * Handles SOAP responses
     */
    public void onSoapResponse(SOAPResponse response, ActivityContextInterface aci) {
        processResponse(response);
    }
    
    /**
     * Processes responses 
     */
    private void processResponse(SOAPResponse response) {
        try {
            if (isFineTraceable()) {
                fine("received SOAP Response");
                fine("  response status: " + response.getStatusCode() + " " + response.getStatusReason());
            }

            if (isFinestTraceable()) {
                finest("  response body:   " + messageToString(response.getMessage()));
            }
        }
        catch (Exception e) {
            warning("unable to process response", e);
        }
    }

    private static String messageToString(SOAPMessage message) throws IOException, SOAPException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        message.writeTo(baos);
        return new String(baos.toByteArray(), "utf-8");
    }

    private URL requestURL;
    private boolean sendSyncRequest;
    private SOAPProvider soapProvider;
    private SOAPActivityContextInterfaceFactory aciFactory;
}
