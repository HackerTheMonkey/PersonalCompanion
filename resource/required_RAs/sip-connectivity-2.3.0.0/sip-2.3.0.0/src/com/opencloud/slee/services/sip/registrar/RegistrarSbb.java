package com.opencloud.slee.services.sip.registrar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.DateHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.ChildRelation;
import javax.slee.CreateException;
import javax.slee.facilities.TraceLevel;

import com.opencloud.javax.sip.header.FlowIDHeader;
import com.opencloud.slee.services.sip.location.FlowID;
import com.opencloud.slee.services.sip.location.LocationService;
import com.opencloud.slee.services.sip.location.Registration;
import com.opencloud.slee.services.sip.presence.PresenceAwareSbb;
import com.opencloud.slee.services.sip.presence.PresentityState;
import com.opencloud.slee.services.sip.presence.PresentityStateChangeEvent;

/**
 * New registrar SBB. Registrations stored as named ACIs, use initial
 * event selector so that same SBB entity processes all registration
 * events for a given address-of-record.
 */
public abstract class RegistrarSbb extends PresenceAwareSbb {

    protected String getTraceMessageType() { return "RegistrarSbb"; }

    public void onRegisterEvent(RequestEvent event, ActivityContextInterface aci) {
        if (isTraceable(TraceLevel.FINEST))
            finest("onRegisterEvent: received request:\n" + event.getRequest());

        aci.detach(getSbbLocalObject());
        Request request = event.getRequest();
        ServerTransaction st = event.getServerTransaction();

        try {
            // RFC3261 10.3

            // Is this request for this domain?

            // Process require header

            // Authenticate
            // Authorize
            // OK we're authorized now ;-)

            URI uri = ((ToHeader)request.getHeader(ToHeader.NAME)).getAddress().getURI();
            String sipAddressOfRecord = getCanonicalAddress(uri);

            LocationService ls = getLocationService();

            if (request.getHeader(ContactHeader.NAME) == null) {
                // This was only a query
                if (isTraceable(TraceLevel.FINE)) fine("received registration query for: " + sipAddressOfRecord);
                processQuery(st, ls.getRegistration(sipAddressOfRecord));
                return;
            }

            // see if this is a "remove all" request, true if there is a "Contact: *" header
            boolean removeAll = false;
            ArrayList newContacts = new ArrayList(4);
            for (Iterator it = request.getHeaders(ContactHeader.NAME); it.hasNext(); ) {
                ContactHeader contact = (ContactHeader) it.next();
                // Is the header a wildcard ("Contact: *") header?
                if (contact.getAddress().isWildcard()) removeAll = true;
                newContacts.add(contact);
            }

            ExpiresHeader expiresHeader = request.getExpires();
            String callId = ((CallIdHeader)request.getHeader(CallIdHeader.NAME)).getCallId();
            int cseq = ((CSeqHeader)request.getHeader(CSeqHeader.NAME)).getSequenceNumber();

            if (removeAll) {
                if (isTraceable(TraceLevel.FINE)) fine("received remove all request for: " + sipAddressOfRecord);
                // check this is a valid "remove all" request
                if ((expiresHeader == null) || (expiresHeader.getExpires() != 0) || (newContacts.size() > 1)) {
                    if (isTraceable(TraceLevel.FINE)) fine("remove all request malformed");
                    // malformed request in RFC3261 ch10.3 step 6
                    sendFinalResponse(st, Response.BAD_REQUEST, null, false);
                    return;
                }
                // Looks like a valid request, process the removal
                processRemoveAll(st, ls, sipAddressOfRecord, callId, cseq);
                return;
            }

            // The request is an add or modify, so we may need to create the ACI
            // if we don't already have it
            if (isTraceable(TraceLevel.FINE)) fine("received registration change request for: " + sipAddressOfRecord);

            Registration reg = ls.getOrCreateRegistration(sipAddressOfRecord);
            Iterator it = newContacts.iterator();

            while (it.hasNext()) {
                ContactHeader contact = (ContactHeader) it.next();

                long requestedExpires = validateExpireTime(st, contact, expiresHeader);
                if (requestedExpires == -1) return; // error response has been sent

                if (isTraceable(TraceLevel.FINER)) fine("expire time for contact [" + contact.getAddress() + "] is " + requestedExpires + "s");

                requestedExpires = requestedExpires * 1000; // convert to ms, relative

                // Get the q-value (preference) for this binding - default to 0.0 (min)
                float q = 0;
                if (contact.getQValue() != -1) q = contact.getQValue();
                if ((q > 1) || (q < 0)) {
                    sendFinalResponse(st, Response.BAD_REQUEST, null, false);
                    return;
                }

                Address contactAddress = contact.getAddress();
                String contactURIString = contactAddress.getURI().toString();

                // Get the flow-id (if any) - see draft-ietf-sip-outbound-03.
                // The OC stack makes a unique flow token available in the X-Flow-ID header
                FlowIDHeader flowHeader = (FlowIDHeader) request.getHeader(FlowIDHeader.NAME);
                FlowID flowID = flowHeader == null ? null : FlowID.fromString(flowHeader.getFlowID());

                Registration.Contact existingEntry = reg.getContact(contactURIString);

                if (existingEntry == null) {
                    // new registration
                    if (requestedExpires != 0) {
                        if (isTraceable(TraceLevel.FINE)) fine("adding registration: " + sipAddressOfRecord + " -> " + contactAddress +
                                (flowID != null ? ", flow=" + flowID : ""));
                        reg.addContact(contactURIString, q, System.currentTimeMillis() + requestedExpires, callId, cseq, flowID);
                    }
                }
                else {
                    // update existing registration
                    if (callId.equals(existingEntry.getCallId()) && cseq <= existingEntry.getCSeq()) {
                        // invalid request, call-id must be different or cseq greater
                        sendFinalResponse(st, Response.BAD_REQUEST, null, false);
                        return;
                    }

                    if (requestedExpires == 0) {
                        if (flowID == null) {
                            if (isTraceable(TraceLevel.FINE)) fine("removing registration: " + sipAddressOfRecord + " -> " + contactAddress);
                            reg.removeContact(contactURIString);
                        }
                        else {
                            if (isTraceable(TraceLevel.FINE)) fine("removing registration: " + sipAddressOfRecord + " -> " + contactAddress + ", flow=" + flowID);
                            reg.removeContact(flowID);
                        }
                    }
                    else {
                        if (isTraceable(TraceLevel.FINE)) fine("updating registration: " + sipAddressOfRecord + " -> " + contactAddress +
                                (flowID != null ? ", flow=" + flowID : ""));
                        reg.updateContact(contactURIString, q, System.currentTimeMillis() + requestedExpires, callId, cseq, flowID);
                    }
                }
            }

            ls.updateRegistration(reg);

            // now send OK response with all contacts, rollback if send fails
            sendFinalResponse(st, Response.OK, reg.getContacts(), true);
           
            // update Presence information
            PresentityState newState = new PresentityState(reg.getContacts().isEmpty() ? PresentityState.CLOSED : PresentityState.OPEN, null);
            if (isTraceable(TraceLevel.FINE)) fine("Attempting to create PresentityState with values: " + (reg.getContacts().isEmpty() ? PresentityState.CLOSED : PresentityState.OPEN) + "/null");
            
            managePresenceStateChange(sipAddressOfRecord, newState);

        } catch (Exception e) {
            // should only get here if some system error occurs, rollback and try
            // sending an error response
            warn("unable to process registration, rolling back", e);
            getSbbContext().setRollbackOnly();
            sendFinalResponse(st, Response.SERVER_INTERNAL_ERROR, null, false);
        }
    }

    private void sendFinalResponse(ServerTransaction st,
                                   int statusCode,
                                   List contacts,
                                   boolean rollback) {
        try {
            Response response = getSipMessageFactory().createResponse(statusCode, st.getRequest());
            if (contacts != null) {
                for (Iterator it = contacts.iterator(); it.hasNext(); ) {
                    Registration.Contact contact = (Registration.Contact) it.next();
                    Address addr = getSipAddressFactory().createAddress(contact.getContactURI());
                    ContactHeader hdr = getSipHeaderFactory().createContactHeader(addr);
                    hdr.setExpires((int)(contact.getExpiryDelta()/1000) + 1);
                    hdr.setQValue(contact.getQValue());
                    response.addHeader(hdr);
                }
            }
            DateHeader date = getSipHeaderFactory().createDateHeader(Calendar.getInstance());
            response.addHeader(date);

            if (isTraceable(TraceLevel.FINEST))
                finest("sending response:\n" + response);

            st.sendResponse(response);
        } catch (Exception e) {
            warn("unable to send " + statusCode + " response", e);
            if (rollback) {
                warn("rolling back registration changes");
                getSbbContext().setRollbackOnly();
            }
        }
    }

    private void processQuery(ServerTransaction st, Registration reg) {
        sendFinalResponse(st, Response.OK, reg == null ? null : reg.getContacts(), false);
    }

    private void processRemoveAll(ServerTransaction st, LocationService ls, String uri, String callId, int cseq) {
        // go through all registrations
        // if callid different or cseq higher, remove registration
        // if callid same and cseq equal or lower, then this is an error
        // if all removed, remove regACI
        Registration reg = ls.getRegistration(uri);
        if (reg != null) {
            List contacts = reg.getContacts();
            for (Iterator it = contacts.iterator(); it.hasNext(); ) {
                Registration.Contact contact = (Registration.Contact) it.next();
                if (!callId.equals(contact.getCallId())) continue; // OK, will be deleted at end
                if (cseq > contact.getCSeq()) continue; // OK

                // callId same and cseq not greater - this is an illegal "remove all" request
                sendFinalResponse(st, Response.BAD_REQUEST, null, false);
                return;
            }
        }
        else {
            // nothing to remove
            sendFinalResponse(st, Response.BAD_REQUEST, null, false);
            return;
        }

        // If we got here then the remove was OK.
        // Tell location service to remove the record
        ls.removeRegistration(reg);
        sendFinalResponse(st, Response.OK, null, true); // will rollback changes if send fails
        managePresenceStateChange(reg.getAddressOfRecord(), new PresentityState(PresentityState.CLOSED, null));
    }

    /**
     * Obtain and validate the requested expiry time.
     * @param st
     * @param contact
     * @param expiresHeader
     * @return the time to expiry, in _seconds_, or -1 if expiry time too short
     */
    private long validateExpireTime(ServerTransaction st, ContactHeader contact, ExpiresHeader expiresHeader) {
        long requestedExpires = MAX_EXPIRES;
        if (contact.getExpires() >= 0) {
            requestedExpires = contact.getExpires();
        }
        else if ((expiresHeader != null) && (expiresHeader.getExpires() >= 0)) {
            requestedExpires = expiresHeader.getExpires();
        }

        if (requestedExpires > MAX_EXPIRES) {
            requestedExpires = MAX_EXPIRES;
        }
        else if (requestedExpires > 0 && requestedExpires < MIN_EXPIRES) {
            // requested expiry too short, send response with min-expires
            try {
                Response response = getSipMessageFactory().createResponse(Response.INTERVAL_TOO_BRIEF, st.getRequest());
                MinExpiresHeader min = getSipHeaderFactory().createMinExpiresHeader((int)MIN_EXPIRES);
                response.addHeader(min);

                if (isTraceable(TraceLevel.FINEST))
                    finest("sending response:\n" + response);

                st.sendResponse(response);
            } catch (Exception e) {
                warn("unable to send interval too brief response", e);
            }
            return -1;
        }
        return requestedExpires;
    }
    
    private void managePresenceStateChange (String sipAddressOfRecord, PresentityState newState) {
        
        // Fire an event to alert the Presence SBB of the change in registration
        ActivityContextInterface nullAci = getNullACIFactory().getActivityContextInterface(getNullActivityFactory().createNullActivity());
        if (isTraceable(TraceLevel.FINE)) fine("Firing PresentityStateChangeEvent event from RegistrarSbb with sip address of record of: " + sipAddressOfRecord + "; presence state is: " + newState.toString());
        firePresentityStateChangeEvent(new PresentityStateChangeEvent(sipAddressOfRecord, null, null, newState, Long.valueOf(System.currentTimeMillis()), null), nullAci, null);
        
    }

    private LocationService getLocationService() throws CreateException {
        ChildRelation rel = getLocationServiceChildRelation();
        return (LocationService) rel.create();
    }

    public abstract ChildRelation getLocationServiceChildRelation();
    
    public abstract void firePresentityStateChangeEvent(PresentityStateChangeEvent event, ActivityContextInterface aci, javax.slee.Address address);

    // TODO configurable
    private static final long MIN_EXPIRES = 60;
    private static final long MAX_EXPIRES = 3600;
}
