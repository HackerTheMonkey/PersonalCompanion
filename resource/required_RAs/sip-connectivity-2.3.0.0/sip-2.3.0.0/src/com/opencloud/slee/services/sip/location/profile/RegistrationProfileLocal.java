package com.opencloud.slee.services.sip.location.profile;

import com.opencloud.slee.services.sip.location.ContactImpl;
import com.opencloud.slee.services.sip.location.FlowID;

import javax.slee.profile.ProfileLocalObject;
import java.util.List;

/**
 * Profile local interface presented to SBBs.
 */
public interface RegistrationProfileLocal extends ProfileLocalObject {

    public String getAddressOfRecord();

    void addContact(ContactImpl contact);

    public ContactImpl getContact(String contactURI);
    public ContactImpl getContact(FlowID flowID);

    public ContactImpl removeContact(String contactURI);
    public ContactImpl removeContact(FlowID flowID);

    public List<ContactImpl> getContacts();
}
