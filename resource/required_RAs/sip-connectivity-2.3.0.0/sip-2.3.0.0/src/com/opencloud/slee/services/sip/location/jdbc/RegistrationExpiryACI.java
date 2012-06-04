package com.opencloud.slee.services.sip.location.jdbc;

import com.opencloud.slee.services.sip.location.FlowID;

import javax.slee.ActivityContextInterface;

/**
 * The {@link JDBCLocationSbb} uses this ACI to set registration expiry
 * timers. When a timer fires, the address of record is looked up in
 * the database, and any expired entries are removed.
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

    /**
     * Flow-ID for the registration (may be null)
     */
    FlowID getFlowID();
    void setFlowID(FlowID flow);

    /**
     * Call-Id used in the original registration request - only expire if CSeq and Call-Id
     * in the ACI match what is in the current registration entry.
     */
    String getCallId();
    void setCallId(String callId);

    /**
     * CSeq used in the original registration request - only expire if CSeq and Call-Id
     * in the ACI match what is in the current registration entry.
     */
    int getCSeq();
    void setCSeq(int cseq);
}
