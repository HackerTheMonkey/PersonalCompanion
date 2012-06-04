package com.opencloud.slee.example.soap.ping;

import com.opencloud.slee.resources.soap.IncomingSOAPRequestActivity;
import com.opencloud.slee.resources.soap.SOAPProvider;
import com.opencloud.slee.resources.soap.SOAPRequest;
import com.opencloud.slee.services.common.BaseSbb;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.SbbContext;
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

/**
 * Simple Soap "Ping" SBB - returns a SOAP message containing a timestamp and some text from
 * the request.
 */
public abstract class SoapPingSbb extends BaseSbb {

    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);
        try {
            Context myEnv = (Context) new InitialContext().lookup("java:comp/env");
            soapProvider = (SOAPProvider) myEnv.lookup("slee/resources/soap/provider");
        }
        catch (NamingException ne) {
            severe("Could not set SBB context", ne);
        }
    }

    protected String getTracerName() {
        return "SoapPingSbb";
    }

    /**
     * Handles Soap requests
     */
    public void onSoapRequest(SOAPRequest request, ActivityContextInterface aci) {
        try {
            if (isFinestTraceable()) finest("onSoapRequest: received SOAP Request");

            IncomingSOAPRequestActivity activity = (IncomingSOAPRequestActivity) aci.getActivity();

            SOAPMessage incomingMessage = request.getMessage();
            SOAPBody incomingBody = incomingMessage.getSOAPPart().getEnvelope().getBody();

            String text = "(none)";

            Iterator iter = incomingBody.getChildElements();
            while (iter.hasNext()) {
                Node node = (Node) iter.next();
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    SOAPElement element = (SOAPElement) node;
                    Iterator iter2 = element.getChildElements();
                    while (iter2.hasNext()) {
                        node = (Node) iter2.next();
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            text = node.getValue();
                            break;
                        }
                    }
                    break;
                }
            }

            if (isFinestTraceable()) finest("onSoapRequest: echoText = " + text);

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
            echoTextElement.addTextNode(text);

            if (isFinestTraceable()) finest("onSoapRequest: sending SOAP Response");
            activity.sendResponse(message);
        }
        catch (SOAPException e) {
            warning("unable to generate response", e);
        }
        catch (IOException e) {
            warning("unable to send response", e);
        }
    }

    private SOAPProvider soapProvider;
}
