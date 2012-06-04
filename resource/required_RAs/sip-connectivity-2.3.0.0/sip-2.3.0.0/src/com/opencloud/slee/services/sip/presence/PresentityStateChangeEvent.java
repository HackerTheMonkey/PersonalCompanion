package com.opencloud.slee.services.sip.presence;

import java.io.Serializable;

public final class PresentityStateChangeEvent implements Serializable {

    public PresentityStateChangeEvent(String sipAddressOfRecord, String sipETag, String sipIfMatch, PresentityState presenceState, long submissionTime, long expiryTime) {
        if (sipAddressOfRecord == null) throw new NullPointerException("sipAddressOfRecord");
        this.sipAddressOfRecord = sipAddressOfRecord;
        this.sipETag = sipETag;
        this.sipIfMatch = sipIfMatch;
        this.presenceState = presenceState;
        this.submissionTime = submissionTime;
        this.expiryTime = expiryTime;
    }
    
    public PresentityStateChangeEvent(String sipAddressOfRecord, String sipETag, String sipIfMatch, PresentityState presenceState, Long submissionTime, Long expiryTime) {
        this (sipAddressOfRecord, sipETag, sipIfMatch, presenceState, submissionTime != null ? submissionTime.longValue() : 0, expiryTime != null ? expiryTime.longValue() : 0);
    }

    public String getSipAddressOfRecord() {
        return this.sipAddressOfRecord;
    }
    
    public String getSipETag() {
        return this.sipETag;
    }
    
    public String getSipIfMatch() {
        return this.sipIfMatch;
    }

    public PresentityState getPresenceState() {
        return this.presenceState;
    }
    
    public long getSubmissionTime() {
        return this.submissionTime;
    }
    
    public long getExpiryTime() {
        return this.expiryTime;
    }

    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        sb.append("\nPresentityStateChangeEvent for: ");
        sb.append(sipAddressOfRecord);
        sb.append("\nPresence state is: ");
        sb.append(presenceState);
        sb.append("\nETag: ");
        sb.append(sipETag);
        sb.append("\nsipIfMatch: ");
        sb.append(sipIfMatch);
        sb.append("\nsubmissionTime: ");
        sb.append(submissionTime == 0 ? "null" : submissionTime);
        sb.append("\nexpiryTime: ");
        sb.append(expiryTime == 0 ? "null" : expiryTime);
        
        return sb.toString();

    }

    private final String sipAddressOfRecord;
    private final PresentityState presenceState;
    private final String sipETag;
    private final String sipIfMatch;
    private final long submissionTime;
    private final long expiryTime;
}
