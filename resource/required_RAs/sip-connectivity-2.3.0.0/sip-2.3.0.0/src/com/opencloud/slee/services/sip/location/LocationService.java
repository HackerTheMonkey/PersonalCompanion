package com.opencloud.slee.services.sip.location;

import javax.slee.SbbLocalObject;

/**
 * SIP Location Service local interface, for use by SIP Proxy and Registrar
 * applications. Implementions of this interface may use any mechanism for
 * storing registration data.
 */
public interface LocationService extends SbbLocalObject {

    /**
     * Get the registration details for a subscriber.
     * @param uri the subscriber's canonical SIP address-of-record
     * @return null if not found
     */
    public Registration getRegistration(String uri);

    /**
     * Get the registration details for a subscriber, creating an empty record
     * if none already exists
     * @param uri the subscriber's canonical SIP address-of-record
     * @return the {@link Registration} record for the subscriber.
     */
    public Registration getOrCreateRegistration(String uri);

    /**
     * Clients MUST call this before the end of the transaction, the
     * location service implementation will ensure that the registration
     * changes are written to its backing store.
     * @param reg a {@link Registration} object obtained earlier in the same
     * transaction using {@link #getRegistration(String)} or {@link #getOrCreateRegistration(String)}.
     */
    public void updateRegistration(Registration reg);

    /**
     * Remove all registration data for the subscriber
     * @param reg
     */
    public void removeRegistration(Registration reg);
}
