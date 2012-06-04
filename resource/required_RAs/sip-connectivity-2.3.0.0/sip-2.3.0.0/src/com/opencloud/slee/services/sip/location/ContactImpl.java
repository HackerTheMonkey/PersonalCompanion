package com.opencloud.slee.services.sip.location;

import javax.slee.facilities.TimerID;
import java.io.Serializable;

/**
 * Represents a single registration binding.
 */
public class ContactImpl implements Registration.Contact, Serializable {

    public ContactImpl(String contact, float qValue, long expireTime, String callId, int cseq, FlowID flowID) {
        if (contact == null) throw new IllegalArgumentException("contact");
        if (callId == null) throw new IllegalArgumentException("callId");
        if (qValue < 0.0 || qValue > 1.0) throw new IllegalArgumentException("qValue");
        if (expireTime < 0) throw new IllegalArgumentException("expireDelta");
        if (cseq < 0) throw new IllegalArgumentException("cseq");

        this.contact = contact;
        this.qValue = qValue;
        this.expireTime = expireTime;
        this.callId = callId;
        this.cseq = cseq;
        this.flowID = flowID;
    }

    public String getContactURI() { return contact; }

    public float getQValue() { return qValue; }

    public long getExpiryAbsolute() { return expireTime; }
    public long getExpiryDelta() { return expireTime - System.currentTimeMillis(); }

    public String getCallId() { return callId; }

    public int getCSeq() { return cseq; }

    public TimerID getExpiryTimer() { return timerID; }
    public void setExpiryTimer(TimerID id) { timerID = id; }

    public FlowID getFlowID() { return flowID; }

    private final String contact;
    private final float qValue;
    private final long expireTime; // stored as absolute time
    private final String callId;
    private final int cseq;
    private TimerID timerID;
    private final FlowID flowID;
}
