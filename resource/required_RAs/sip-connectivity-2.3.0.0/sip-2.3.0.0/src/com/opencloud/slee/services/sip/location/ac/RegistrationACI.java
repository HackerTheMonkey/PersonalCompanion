package com.opencloud.slee.services.sip.location.ac;

import com.opencloud.slee.services.sip.location.ContactImpl;

import javax.slee.ActivityContextInterface;

/**
 * ACI to store attributes for SIP registrations.
 * Used by Registrar SBB when setting SLEE timers for SIP registrations
 */
public interface RegistrationACI extends ActivityContextInterface {

    /** SIP Address-Of-Record (public address) for this registration */
    public String getAddressOfRecord();
    public void setAddressOfRecord(String s);

    /** contacts - currently registered contact addresses */
    public ContactImpl[] getContacts();
    public void setContacts(ContactImpl[] contacts);
}
