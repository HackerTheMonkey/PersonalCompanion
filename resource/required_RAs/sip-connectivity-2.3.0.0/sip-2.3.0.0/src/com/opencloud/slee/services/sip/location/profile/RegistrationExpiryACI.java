package com.opencloud.slee.services.sip.location.profile;

import com.opencloud.slee.services.sip.location.FlowID;

import javax.slee.ActivityContextInterface;

/**
 * The {@link ProfileLocationSbb} uses this ACI to set registration expiry
 * timers. When a timer fires, the address of record is looked up in
 * the profile table, and any expired entries are removed.
 */
public interface RegistrationExpiryACI extends ActivityContextInterface {
    /**
     * SIP Address-Of-Record (public address) for this registration
     */
    String getAddressOfRecord();
    void setAddressOfRecord(String s);

    /**
     * Contact address for the registration
     */
    String getContactAddress();
    void setContactAddress(String s);
}