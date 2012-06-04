package com.opencloud.slee.services.sip.location.profile;

import com.opencloud.slee.services.sip.location.ContactImpl;
import com.opencloud.slee.services.sip.location.FlowID;

import javax.slee.profile.Profile;
import javax.slee.profile.ProfileContext;
import javax.slee.profile.ProfileVerificationException;
import javax.slee.CreateException;
import java.util.*;

public abstract class RegistrationProfile implements Profile, RegistrationProfileCMP, RegistrationProfileManagement {

    public void setProfileContext(ProfileContext context) {
        this.context = context;
    }

    public void profilePostCreate() throws CreateException {
        setAddressOfRecord(context.getProfileName());
        profileLoad();
    }

    public void profileLoad() {
        ContactImpl[] c = getBindings();
        contacts = c == null ? new LinkedList<ContactImpl>() : new LinkedList<ContactImpl>(Arrays.asList(c));
    }

    public void profileStore() {
        setBindings(contacts == null ? null : contacts.toArray(new ContactImpl[contacts.size()]));
    }

    public void unsetProfileContext() { } 
    public void profileInitialize() { }
    public void profileActivate() { }
    public void profilePassivate() { }
    public void profileRemove() { }
    public void profileVerify() throws ProfileVerificationException { }

    public void addContact(ContactImpl contact) {
        contacts.add(contact);
    }

    public ContactImpl getContact(String contactURI) {
        for (ContactImpl contact : contacts) {
            if (contactURI.equals(contact.getContactURI())) return contact;
        }
        return null;
    }

    public ContactImpl removeContact(String contactURI) {
        for (ContactImpl contact : contacts) {
            if (contactURI.equals(contact.getContactURI())) {
                contacts.remove(contact);
                return contact;
            }
        }
        return null;
    }

    public ContactImpl getContact(FlowID flowID) {
        for (ContactImpl contact : contacts) {
            if (flowID.equals(contact.getFlowID())) return contact;
        }
        return null;
    }

    public ContactImpl removeContact(FlowID flowID) {
        for (ContactImpl contact : contacts) {
            if (flowID.equals(contact.getFlowID())) {
                contacts.remove(contact);
                return contact;
            }
        }
        return null;
    }

    public String[] getContactAddresses() {
        List<String> addresses = new ArrayList<String>(contacts.size());
        for (ContactImpl contact : contacts) {
            addresses.add(contact.getContactURI());
        }
        return addresses.toArray(new String[addresses.size()]);
    }

    public List<ContactImpl> getContacts() {
        return Collections.unmodifiableList(contacts);
    }

    private ProfileContext context;
    private List<ContactImpl> contacts;
}
