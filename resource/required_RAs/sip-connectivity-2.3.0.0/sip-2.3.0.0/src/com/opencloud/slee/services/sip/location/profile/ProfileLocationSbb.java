package com.opencloud.slee.services.sip.location.profile;

import com.opencloud.slee.services.sip.common.BaseSbb;
import com.opencloud.slee.services.sip.location.ContactImpl;
import com.opencloud.slee.services.sip.location.FlowID;
import com.opencloud.slee.services.sip.location.Registration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.*;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerID;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.TraceLevel;
import javax.slee.profile.ProfileAlreadyExistsException;
import javax.slee.profile.ProfileTable;
import java.util.Collections;
import java.util.List;

/**
 * Uses SLEE 1.1 writeable profiles to store SIP registrations.
 */
public abstract class ProfileLocationSbb extends BaseSbb {

    // Use this address to detect "our" timer events
    public static final Address TIMER_ADDRESS = new Address(AddressPlan.UNDEFINED, "PROFILE_TIMER");

    public InitialEventSelector checkTimerEvent(InitialEventSelector ies) {
        // Only trigger on timer events fired by this SBB
        Address address = ies.getAddress();
        ies.setInitialEvent(address != null && address.equals(TIMER_ADDRESS));
        return ies;
    }

    public void setSbbContext(SbbContext context) {
        super.setSbbContext(context);
        try {
            Context env = (Context) new InitialContext().lookup("java:comp/env");
            String tableName = (String) env.lookup("registrationProfileTable");
            registrationTable = getProfileFacility().getProfileTable(tableName);
        } catch (Exception e) {
            severe("failed to set SBB context", e);
        }
    }

    public void onTimerEvent(TimerEvent event, RegistrationExpiryACI regExpiryACI) {
        regExpiryACI.detach(getSbbLocalObject());

        String publicAddress = regExpiryACI.getAddressOfRecord();
        String contactAddress = regExpiryACI.getContactAddress();

        if (isTraceable(TraceLevel.FINER)) finer("received expiration timer event for contact: " + publicAddress + "/" + contactAddress);

        Registration reg = getRegistration(publicAddress);
        if (reg == null) {
            if (isTraceable(TraceLevel.FINER)) finer("contact " + publicAddress + "/" + contactAddress + " not found, nothing to do");
            return;
        }

        Registration.Contact contact = reg.removeContact(contactAddress);
        if (contact == null) {
            if (isTraceable(TraceLevel.FINER)) finer("contact " + publicAddress + "/" + contactAddress + " not found, nothing to do");
            return;
        }

        if (isTraceable(TraceLevel.FINER)) finer("contact " + publicAddress + "/" + contactAddress + " has expired, removing");
        updateRegistration(reg);
    }

    public Registration getRegistration(String uri) {
        RegistrationProfileLocal profile = lookupRegistration("getRegistration", registrationTable, uri, false);
        return profile == null ? null : new RegistrationImpl(profile);
    }

    public Registration getOrCreateRegistration(String uri) {
        return new RegistrationImpl(lookupRegistration("getOrCreateRegistration", registrationTable, uri, true));
    }

    public void updateRegistration(Registration reg) {
        // Remove the profile if no contacts remain
        if (reg.getContacts().isEmpty()) {
            if (isTraceable(TraceLevel.FINEST)) finest("no contacts remain, removing profile for " + reg.getAddressOfRecord());
            ((RegistrationImpl)reg).removeProfile("updateRegistration");
        }
    }

    public void removeRegistration(Registration reg) {
        ((RegistrationImpl)reg).removeProfile("removeRegistration");
    }

    protected String getTraceMessageType() {
        return "ProfileLocationSbb";
    }

    private RegistrationProfileLocal lookupRegistration(String context, ProfileTable table, String addressOfRecord, boolean create) {
        if (isTraceable(TraceLevel.FINEST)) finest(context + ": lookup profile for address-of-record \"" + addressOfRecord + "\"");
        RegistrationProfileLocal profile = (RegistrationProfileLocal) table.findProfileByAttribute("addressOfRecord", addressOfRecord);

        if (profile == null) {
            if (isTraceable(TraceLevel.FINEST)) finest(context + ": no profile matching address-of-record \"" + addressOfRecord + (create?"\", create" : "\""));
            if (create) {
                try {
                    profile = (RegistrationProfileLocal) registrationTable.create(addressOfRecord);
                    if (isTraceable(TraceLevel.FINEST)) finest(context + ": created profile " + profile.getProfileTableName() + "/" + profile.getProfileName());
                } catch (ProfileAlreadyExistsException e) {
                    throw new RuntimeException(e);
                } catch (CreateException e) {
                    throw new RuntimeException(e);
                }
            }
            return profile;
        }
        else {
            if (isTraceable(TraceLevel.FINEST)) finest(context + ": found profile " + profile.getProfileTableName() + "/" + profile.getProfileName());
            return profile;
        }
    }

    private void startExpiryTimer(String addressOfRecord, ContactImpl contact) {
        ActivityContextInterface aci = getNullACIFactory().getActivityContextInterface(getNullActivityFactory().createNullActivity());

        RegistrationExpiryACI regExpiryACI = asSbbActivityContextInterface(aci);
        regExpiryACI.setAddressOfRecord(addressOfRecord);
        regExpiryACI.setContactAddress(contact.getContactURI());

        contact.setExpiryTimer(getTimerFacility().setTimer(regExpiryACI, TIMER_ADDRESS, contact.getExpiryAbsolute(), new TimerOptions()));
        if (isTraceable(TraceLevel.FINEST)) finest("set registration expiry timer for " + contact.getContactURI() +
                ", expires in " + contact.getExpiryDelta() + "ms");
    }

    private class RegistrationImpl implements Registration {
        RegistrationImpl(RegistrationProfileLocal profile) {
            this.profile = profile;
            this.addressOfRecord = profile.getAddressOfRecord();
        }

        RegistrationProfileLocal getProfile() { return profile; }

        void removeProfile(String context) {
            if (!profileRemoved) {
                if (isTraceable(TraceLevel.FINEST)) finest(context + ": removing profile " + profile.getProfileTableName() + "/" + profile.getProfileName());
                profile.remove();
                profileRemoved = true;
            }
        }

        public String getAddressOfRecord() { return addressOfRecord; }

        public List getContacts() {
            return profileRemoved ? Collections.emptyList() : profile.getContacts();
        }

        public Contact getContact(String contactURI) {
            return profileRemoved ? null : profile.getContact(contactURI);
        }

        public Contact getContact(FlowID flowID) {
            return profileRemoved ? null : profile.getContact(flowID);
        }

        public Contact removeContact(String contactURI) {
            ContactImpl contact = profile.removeContact(contactURI);
            if (contact != null) {
                if (isTraceable(TraceLevel.FINEST)) finest("removeContact: " + contactURI);
                // stop timer
                TimerID id = contact.getExpiryTimer();
                if (id != null) getTimerFacility().cancelTimer(id);
            }
            return contact;
        }

        public Contact removeContact(FlowID flowID) {
            ContactImpl contact = profile.removeContact(flowID);
            if (contact != null) {
                if (isTraceable(TraceLevel.FINEST)) finest("removeContact: flow=" + flowID);
                // stop timer
                TimerID id = contact.getExpiryTimer();
                if (id != null) getTimerFacility().cancelTimer(id);
            }
            return contact;
        }

        public Contact addContact(String uri, float q, long expires, String callId, int cseq, FlowID flowID) {
            ContactImpl contact = new ContactImpl(uri, q, expires, callId, cseq, flowID);
            profile.addContact(contact);
            if (isTraceable(TraceLevel.FINEST)) finest("addContact: " + uri);
            startExpiryTimer(profile.getAddressOfRecord(), contact);
            return contact;
        }

        public Contact updateContact(String uri, float q, long expires, String callId, int cseq, FlowID flowID) {
            removeContact(uri);
            return addContact(uri, q, expires, callId, cseq, flowID);
        }

        private final RegistrationProfileLocal profile;
        private final String addressOfRecord;
        private boolean profileRemoved = false;
    }

    public abstract RegistrationExpiryACI asSbbActivityContextInterface(ActivityContextInterface aci);

    private ProfileTable registrationTable;
}
