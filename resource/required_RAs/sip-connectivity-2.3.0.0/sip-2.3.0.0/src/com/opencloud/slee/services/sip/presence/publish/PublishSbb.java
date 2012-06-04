package com.opencloud.slee.services.sip.presence.publish;

import java.util.ArrayList;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.SIPETagHeader;
import javax.sip.header.SIPIfMatchHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.SbbContext;
import javax.slee.facilities.TraceLevel;

import com.opencloud.slee.services.sip.location.LocationService;
import com.opencloud.slee.services.sip.presence.PresenceACI;
import com.opencloud.slee.services.sip.presence.PresenceAwareSbb;
import com.opencloud.slee.services.sip.presence.PresentityState;
import com.opencloud.slee.services.sip.presence.PresentityStateChangeEvent;

public abstract class PublishSbb extends PresenceAwareSbb {
    
    protected String getTraceMessageType() { return "PublishSbb"; }
    
    public void setSbbContext(SbbContext context) {
        super.setSbbContext(context);
        try {
            Context myEnv = (Context) new InitialContext().lookup("java:comp/env");
        } catch (NamingException ne) {
            severe("Could not set SBB context", ne);
        }
    }
    
    public void onPublishEvent(RequestEvent event, ActivityContextInterface aci) {
        
        if (isTraceable(TraceLevel.FINEST))
            finest("onPublishEvent: PublishSbb received request:\n" + event.getRequest());

        Request request = event.getRequest();
        ServerTransaction st = event.getServerTransaction();
        String sipAddress = getCanonicalAddress(((ToHeader)request.getHeader(ToHeader.NAME)).getAddress().getURI());
        
        // Check that the resource identified by the request-URI is one that this server is responsible for
        //  This will be achieved by checking that the resource has been registered with the Registrar
        try {
            if (getLocationService().getRegistration(sipAddress) == null) {
                sendErrorResponse(Response.NOT_FOUND, "Presentity identified by Request-URI '" + sipAddress + "' not registered with Registrar", st, request);
                return;
            }
        } catch (CreateException e) {
            warn ("Error creating location service: ", e);
        }
        
        // Check that the event package is correct - should be 'presence'
        if (request.getHeader(EventHeader.NAME) != null) {
            String eventType = ((EventHeader)request.getHeader(EventHeader.NAME)).getEventType();
            if (!eventType.equals("presence")) {
                sendErrorResponse(Response.BAD_EVENT, "PUBLISH request received with incorrect event package, package was '" + eventType + "', expected 'presence'", st, request);
                return;
            }
        }
        
        // Check for and validate SIP-If-Match header
        String eTag = null;
        String newETag = (new Integer ((int)(Math.random() * 10000))).toString();
        
        if ((SIPIfMatchHeader)request.getHeader(SIPIfMatchHeader.NAME) != null) {
            
            // Found a SIP-If-Match header, retrieve E-Tag
            SIPIfMatchHeader sipIfMatchHeader = (SIPIfMatchHeader)request.getHeader(SIPIfMatchHeader.NAME); 
            eTag = sipIfMatchHeader.getETag();
            if (isTraceable(TraceLevel.FINEST)) finest("onPublishEvent: found E-Tag: " + eTag);
            // Check that the entity-tag matches an entity-tag stored by the server
            if (!entityTagIsKnownByServer(sipAddress, eTag)) {
                if (isTraceable(TraceLevel.FINEST)) finest("onPublishEvent: E-Tag '" + eTag + "' not recognised by server");
                sendErrorResponse(Response.CONDITIONAL_REQUEST_FAILED, "Entity-tag is not known to this server, value of entity-tag is: " + eTag, st, request);
                return;
            }
        }
        
        // Validate requested Expiry time
        long expiryTime = validateExpireTime(request);
        // Check for correct media type
        if (request.getContent() != null && request.getHeader(ContentTypeHeader.NAME) != null) {
            ContentTypeHeader contentTypeHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
            if (!contentTypeHeader.getContentType().equalsIgnoreCase("application") || (!contentTypeHeader.getContentSubType().equalsIgnoreCase("pidf+xml"))) {
                sendErrorResponse(Response.UNSUPPORTED_MEDIA_TYPE, "Uncrecognised media type", st, request);
                return;
            }
        }
        
        // Process the PUBLISH request
        String body = request.toString();
        if (expiryTime == 0) {
            // this is a request to remove all stored presence information associated with this entity-tag
            if (eTag == null) {
                // this is a mal-formed request - can't remove presence information if it was never published in the first place!
                sendErrorResponse(Response.BAD_REQUEST, "Received request to remove presence information, but no entity-tag was provided", st, request);
                return;
            } else {
               updatePresenceState(sipAddress, newETag, eTag, body, System.currentTimeMillis(), new Long(expiryTime));
            }
        } else {
            // this is either an initial request, or a request to modify or refresh a previous publication
            // get the body of PUBLISH request, if it has one
            
            if (eTag == null) {
                // this an initial request, make sure there's a body containing some presence info
                if (request.getContent() != null) {
                    updatePresenceState(sipAddress, newETag, null, body, System.currentTimeMillis(), System.currentTimeMillis() + (expiryTime * 1000));
                } else {
                    sendErrorResponse(Response.BAD_REQUEST, "Received initial PUBLISH request, but no presence information was provided", st, request);
                    return;
                }
            } else {
                // this is either a request to refresh the publication (no body), or to modify or refresh the published presence info
                updatePresenceState(sipAddress, newETag, eTag, (body != null ? body.toString() : null), System.currentTimeMillis(), System.currentTimeMillis() + (expiryTime * 1000));
                
            }
        }
        
        // if this point is reached, all processing has been successfully undertaken, so return a 200 OK response
        sendOkResponse(st, request, (int)expiryTime, newETag);
        
    }
    
    public void onInDialogPublishEvent(RequestEvent event, ActivityContextInterface aci) {
        
    }
    
    private long validateExpireTime(Request request) {
        
        // Set the requestedExpires to a default value of 3600 before validating
        long requestedExpires = 3600;
        ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
        if (expiresHeader != null && expiresHeader.getExpires() >= 0) requestedExpires = expiresHeader.getExpires();
        
        // Validate the requested expiry time: if requested expiry too long, set to max-expires
        if (requestedExpires > MAX_EXPIRES) requestedExpires = MAX_EXPIRES;
        return requestedExpires;
    }

    private void sendOkResponse (ServerTransaction st, Request request, int expiryTime, String eTag) {
        
        // return an OK message back to the publisher
        try {
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
            
            // add an Expires header
            ExpiresHeader expiresHeader = getSipHeaderFactory().createExpiresHeader(expiryTime);
            response.addHeader(expiresHeader);
            
            // add a SIP-ETag header
            SIPETagHeader sipETagHeader = getSipHeaderFactory().createSIPETagHeader(eTag);
            response.addHeader(sipETagHeader);
            
            st.sendResponse(response);
            if (isTraceable(TraceLevel.FINEST)) finest("sending OK response:\n" + response);
            
        } catch (Exception e) {
            // some error occured. Try to send an error response instead
            sendErrorResponse(Response.SERVER_INTERNAL_ERROR, "An error occurred while attempting to send 200 OK response", st, request);
        }
    }
    
    // Send an error response - the initial request was not acceptable
    private void sendErrorResponse(int errorCode, String errorMessage, ServerTransaction st, Request request) {
        
        warn ("Error encountered: " + errorMessage);
        try {
            Response response = getSipMessageFactory().createResponse(errorCode, st.getRequest());
            if (isTraceable(TraceLevel.FINEST)) finest("sendErrorResponse: attempting to send error response: \n" + response);
            st.sendResponse(response);
        } catch (Exception e) {
            warn("unable to send " + errorCode + " response", e);
        }
        
    }
    
    private PresentityState parsePresenceXml(String presenceXml) {
        PresentityState state = null;
        if (getXmlValue("basic", presenceXml) != null) state = new PresentityState(getXmlValue("basic", presenceXml), getXmlValue("note", presenceXml));
        return state;
    }
    
    private void updatePresenceState(String sipAddress, String sipETag, String sipIfMatch, String presenceXml, Long submissionTime, Long expiryTime) {
        ActivityContextInterface nullAci = getNullACIFactory().getActivityContextInterface(getNullActivityFactory().createNullActivity());
        if (isTraceable(TraceLevel.FINE)) 
            fine("Firing PresentityStateChangeEvent event from PublishSbb with sip address of record of: " + sipAddress + "; presence info is: " + ((parsePresenceXml(presenceXml) == null) ? "null" : parsePresenceXml(presenceXml).toString()));
        firePresentityStateChangeEvent(new PresentityStateChangeEvent(sipAddress, sipETag, sipIfMatch, parsePresenceXml(presenceXml), submissionTime, expiryTime), nullAci, null);
    }
    
    private String getXmlValue(String key, String xml) {
        
        String value = null;
        String openTag = "<" + key + ">";
        String closeTag = "</" + key + ">";
        if (xml.indexOf(openTag) > 0 && xml.indexOf(closeTag) > 0) {
            value = xml.substring(xml.indexOf(openTag) + key.length() + 2, xml.indexOf(closeTag));
            if (value.length() < 1) value = null;
        }
        return value;
    }
    
    private boolean entityTagIsKnownByServer(String sipAddressOfRecord, String eTag) {
        
        if (getPresenceACI(sipAddressOfRecord) != null) {
            PresenceACI aci = asSbbActivityContextInterface(getPresenceACI(sipAddressOfRecord));
            if (aci.getPresenceInformation() != null) {
                String[][] presenceInfo = aci.getPresenceInformation();
                for (int i = 0; i < presenceInfo.length; i++) {
                    if (eTag.equals(presenceInfo[i][PresenceACI.ETAG])) return true;
                }
            }
        }
        return false;
    }
    
    // Get the LocationService ChildSbb
    private LocationService getLocationService() throws CreateException {
        ChildRelation rel = getLocationServiceChildRelation();
        return (LocationService) rel.create();
    }
    
    public abstract void firePresentityStateChangeEvent(PresentityStateChangeEvent event, ActivityContextInterface aci, javax.slee.Address address);
    public abstract ChildRelation getLocationServiceChildRelation();
    public abstract PresenceACI asSbbActivityContextInterface(ActivityContextInterface aci);
    
    // TODO configurable
    private static final long MAX_EXPIRES = 3600;
    
}
