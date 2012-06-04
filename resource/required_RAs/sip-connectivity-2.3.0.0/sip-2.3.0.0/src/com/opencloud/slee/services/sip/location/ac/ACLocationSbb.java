package com.opencloud.slee.services.sip.location.ac;

import com.opencloud.slee.services.sip.common.BaseSbb;
import com.opencloud.slee.services.sip.location.Registration;
import com.opencloud.slee.services.sip.location.ContactImpl;
import com.opencloud.slee.services.sip.location.FlowID;

import javax.slee.*;
import javax.slee.facilities.*;
import javax.slee.nullactivity.NullActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

/**
 * Uses AC Naming to implement a SIP location service.
 * <p>
 * Each registration is stored using an activity context, which is bound
 * to a name in the AC Naming Facility.
 * <p>
 * Each activity context has one or more SLEE timers set on it, one timer
 * for each contact address. The timer events are initial events, these
 * will cause a root SBB of this service to be created, which will then remove
 * the expired contact information from the activity context.   
 */
public abstract class ACLocationSbb extends BaseSbb {

    // Use this address to detect "our" timer events
    public static final Address TIMER_ADDRESS = new Address(AddressPlan.UNDEFINED, "AC_TIMER");

    public InitialEventSelector checkTimerEvent(InitialEventSelector ies) {
        // Only trigger on timer events fired by this SBB
        Address address = ies.getAddress();
        ies.setInitialEvent(address != null ? address.equals(TIMER_ADDRESS) : false);
        return ies;
    }

    // Initial event - get the registration info from ACI and remove expired
    // registrations
    public void onTimerEvent(TimerEvent event, RegistrationACI regACI) {
        regACI.detach(getSbbLocalObject());

        RegistrationImpl reg = new RegistrationImpl(regACI, getTimerFacility(), getSbbTracer());

        reg.checkExpiry(event.getTimerID());
        reg.updateACI(getACNamingFacility());
    }

    // LocationService local interface impl

    public Registration getRegistration(String uri) {
        // lookup the named AC with this name
        ActivityContextInterface aci = getACNamingFacility().lookup(uri);
        if (aci == null) {
            if (isTraceable(TraceLevel.FINEST)) finest("getRegistration: lookup " + uri + ", not found");
            return null;
        }
        else {
            if (isTraceable(TraceLevel.FINEST)) finest("getRegistration: lookup " + uri + ", found");
        }

        RegistrationACI regACI = asSbbActivityContextInterface(aci);

        return new RegistrationImpl(regACI, getTimerFacility(), getSbbTracer());
    }

    public Registration getOrCreateRegistration(String uri) {
        try {
            ActivityContextInterface aci = getACNamingFacility().lookup(uri);
            RegistrationACI regACI;
            if (aci == null) {
                if (isTraceable(TraceLevel.FINEST)) finest("getOrCreateRegistration: lookup " + uri + ", not found");
                aci = getNullACIFactory().getActivityContextInterface(getNullActivityFactory().createNullActivity());
                regACI = asSbbActivityContextInterface(aci);
                regACI.setAddressOfRecord(uri);
                getACNamingFacility().bind(regACI, uri);
                if (isTraceable(TraceLevel.FINEST)) finest("getOrCreateRegistration: bound AC name: " + uri);
            }
            else {
                if (isTraceable(TraceLevel.FINEST)) finest("getRegistration: lookup " + uri + ", found");
                regACI = asSbbActivityContextInterface(aci);
            }

            return new RegistrationImpl(regACI, getTimerFacility(), getSbbTracer());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateRegistration(Registration reg) {
        if (isTraceable(TraceLevel.FINEST)) finest("updateRegistration: " + reg.getAddressOfRecord());
        ((RegistrationImpl)reg).updateACI(getACNamingFacility());
    }

    public void removeRegistration(Registration reg) {
        if (isTraceable(TraceLevel.FINEST)) finest("removeRegistration: " + reg.getAddressOfRecord());
        ActivityContextInterface aci = getACNamingFacility().lookup(reg.getAddressOfRecord());
        if (aci == null) return;
        try {
            getACNamingFacility().unbind(reg.getAddressOfRecord());
        } catch (NameNotBoundException e) { }
        ((NullActivity)aci.getActivity()).endActivity(); // this will unbind the name
    }

    protected String getTraceMessageType() {
        return "ACLocationSbb";
    }

    public abstract RegistrationACI asSbbActivityContextInterface(ActivityContextInterface aci);

    private static class RegistrationImpl implements Registration {
        private RegistrationImpl(RegistrationACI aci, TimerFacility tf, Tracer tracer) {
            this.aci = aci;
            this.tracer = tracer;
            this.timerFacility = tf;

            ContactImpl[] c = aci.getContacts();
            if (c == null) {
                contacts = new ArrayList(2);
            }
            else {
                contacts = new ArrayList(c.length);
                for (int i = 0; i < c.length; i++) {
                    contacts.add(c[i]);
                }
            }
        }

        private void updateACI(ActivityContextNamingFacility acNamingFacility) {
            if (dirty) {
                if (contacts.size() == 0) {
                    if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("no contacts remain, removing AC for " + getAddressOfRecord());
                    // all registrations have been removed - remove ACI
                    try {
                        acNamingFacility.unbind(getAddressOfRecord());
                    } catch (NameNotBoundException e) { }
                    ((NullActivity)aci.getActivity()).endActivity(); // this will unbind the name
                }
                else {
                    if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("updating AC for " + getAddressOfRecord());
                    aci.setContacts((ContactImpl[])contacts.toArray(new ContactImpl[contacts.size()]));
                }
            }
        }

        public String getAddressOfRecord() { return aci.getAddressOfRecord(); }

        public List getContacts() {
            return Collections.unmodifiableList(contacts);
        }

        public Contact getContact(String contactURI) {
            int idx = findIndex(contactURI);
            return idx == -1 ? null : (Contact) contacts.get(idx);
        }

        public Contact getContact(FlowID flowID) {
            int idx = findIndex(flowID);
            return idx == -1 ? null : (Contact) contacts.get(idx);
        }

        public Contact removeContact(String contactURI) {
            int idx = findIndex(contactURI);
            if (idx == -1) return null;
            else {
                if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("removeContact: " + contactURI);
                return removeContact(idx);
            }
        }

        public Contact removeContact(FlowID flowID) {
            int idx = findIndex(flowID);
            if (idx == -1) return null;
            else {
                if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("removeContact: flow=" + flowID);
                return removeContact(idx);
            }
        }

        public Contact addContact(String contactURI, float q, long expires, String callId, int cseq, FlowID flowID) {
            ContactImpl newContact = new ContactImpl(contactURI, q, expires, callId, cseq, flowID);
            contacts.add(newContact);
            if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("addContact: " + contactURI);
            dirty = true;
            newContact.setExpiryTimer(timerFacility.setTimer(aci, TIMER_ADDRESS, expires, new TimerOptions()));
            if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("set registration expiry timer for " + contactURI + ", expires in " + (expires - System.currentTimeMillis()) + "ms");
            return newContact;
        }

        public Contact updateContact(String contactURI, float q, long expires, String callId, int cseq, FlowID flowID) {
            removeContact(contactURI);
            return addContact(contactURI, q, expires, callId, cseq, flowID);
        }

        private int findIndex(String uri) {
            for (int i = 0; i < contacts.size(); i++) {
                Contact c = (Contact) contacts.get(i);
                if (c.getContactURI().equalsIgnoreCase(uri)) return i;
            }
            return -1;
        }

        private int findIndex(FlowID flow) {
            for (int i = 0; i < contacts.size(); i++) {
                Contact c = (Contact) contacts.get(i);
                FlowID f = c.getFlowID();
                if (f != null && f.equals(flow)) return i;
            }
            return -1;
        }

        private Contact removeContact(int idx) {
            dirty = true;
            ContactImpl contact = (ContactImpl) contacts.remove(idx);
            if (contact != null) {
                TimerID timer = contact.getExpiryTimer();
                if (timer != null) timerFacility.cancelTimer(timer);
            }
            return contact;
        }
        
        private void checkExpiry(TimerID id) {
            for (Iterator it = contacts.iterator(); it.hasNext(); ) {
                ContactImpl c = (ContactImpl) it.next();
                if (c.getExpiryTimer().equals(id)) {
                    if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("registration timed out, removing contact " + c.getContactURI() + ", flow=" + c.getFlowID());
                    it.remove();
                    dirty = true;
                }
            }
        }

        private final RegistrationACI aci;
        private final ArrayList contacts;
        private final Tracer tracer;
        private final TimerFacility timerFacility;
        private boolean dirty = false;
    }
}
