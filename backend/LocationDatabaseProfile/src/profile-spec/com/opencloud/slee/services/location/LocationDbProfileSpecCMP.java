package com.opencloud.slee.services.location;

import java.util.HashMap;

public interface LocationDbProfileSpecCMP
{
    /**
     * The key of this HashMap will be the text (string) representation
     * address contained within the specific contact header field, while the
     * value of it will be the text value of the ContactHeader field itself.
     * @return
     */
    public HashMap<String, String> getContacts();
    public void setContacts(HashMap<String, String> contacts);
    /**
     * The key to the HashMap is the name of the CMR
     * being set as the value that corresponds to that
     * key.
     */
    public HashMap<String, CallManagementRule> getCallManagementRules();
    public void setCallManagementRules(HashMap<String, CallManagementRule> CMRs);

    public String getCallId();
    public void setCallId(String callId);

    public long getCallSeq();
    public void setCallSeq(long CSeq);

    public boolean getThirdPartyRegFlag();
    public void setThirdPartyRegFlag(boolean Flag);

    /**
     * The location of the user is considered a global one, which means that
     * any mobile device carried by the user and sends a location update, then
     * that location update will overwrite any pre-existing location information
     * in the AOR profile and it will be considered as the current location
     * of the user (obviously, a user can have only a single location at any time).
     */
    public String getLatitude();
    public void setLatitude(String latitude);

    public String getLongitude();
    public void setLongitude(String longitude);

    public String getTimestamp();
    public void setTimestamp(String timestamp);
}
