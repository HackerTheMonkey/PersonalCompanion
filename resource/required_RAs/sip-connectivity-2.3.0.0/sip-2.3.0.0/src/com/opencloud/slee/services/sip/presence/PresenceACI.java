package com.opencloud.slee.services.sip.presence;

import javax.slee.ActivityContextInterface;

public interface PresenceACI extends ActivityContextInterface {
    
    /*
     * Store presence information for a given presentity in an array of String arrays.
     * Each String array represents presence information provided by a given source.
     * Different sources may be for example different presence agents (for a single presentity),
     *  or hard state (as per registrations) vs soft state (as per published state) sources.
     */
    public String[][] getPresenceInformation();
    public void setPresenceInformation(String[][] presenceInformation);
    
    public String getSipUri();
    public void setSipUri(String sipUri);
    
    public int getTimerCount();
    public void setTimerCount(int count);
    
    // Integers to govern where each piece of information is stored in each String array
    public static final int URI = 0;
    public static final int ETAG = 1;
    public static final int BASIC = 2;
    public static final int NOTE = 3;
    public static final int SUBMISSION_TIME = 4;
    public static final int EXPIRY_TIME = 5;
    
    public static final int ARRAY_SIZE = 6;

}
