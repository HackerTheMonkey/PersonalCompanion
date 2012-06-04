package com.opencloud.slee.services.sip.presence;

import java.io.Serializable;

public class NotifyStateChangeEvent implements Serializable {
    
    public NotifyStateChangeEvent(String sipAddressOfRecord, PresentityState presenceState) {
        if (sipAddressOfRecord == null) throw new NullPointerException("sipAddressOfRecord");
        if (presenceState == null) throw new NullPointerException("presenceState");
        this.sipAddressOfRecord = sipAddressOfRecord;
        this.presenceState = presenceState;
    }
    
    public String getSipAddressOfRecord() {
        return this.sipAddressOfRecord;
    }

    public PresentityState getPresenceState() {
        return this.presenceState;
    }

    public String toString() {
        return "SipNotifyStateChangeEvent[" + sipAddressOfRecord + ":" + presenceState.getBasicState() + "/" + presenceState.getNoteState() + "]";
    }
    
    private final String sipAddressOfRecord;
    private final PresentityState presenceState;

}
