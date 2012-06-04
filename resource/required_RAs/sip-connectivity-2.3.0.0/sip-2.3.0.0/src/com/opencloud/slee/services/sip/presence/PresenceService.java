package com.opencloud.slee.services.sip.presence;

import javax.slee.SbbLocalObject;

public interface PresenceService extends SbbLocalObject {

    /**
     * Get the presence information for a presentity.
     * @param uri the subscriber's canonical SIP address-of-record
     * @return null if not found
     */
    public PresentityState getPresenceState(String uri);

}
