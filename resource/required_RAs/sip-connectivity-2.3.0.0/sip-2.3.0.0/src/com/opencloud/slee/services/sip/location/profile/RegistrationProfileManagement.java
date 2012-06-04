package com.opencloud.slee.services.sip.location.profile;

/**
 * Allows management clients to easily view registrations, but not update.
 */
public interface RegistrationProfileManagement {
    String getAddressOfRecord();
    String[] getContactAddresses();
}
