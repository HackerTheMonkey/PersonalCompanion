/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.opencloud.slee.services.location;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 *
 * @author hasanein
 */
public class CallManagementRule implements Serializable
{
    /**
     * Here are the member variables of the CMR which represent all the
     * various fields set by the user for the sake of creating a CMR that
     * will eventually contribute to the routing of the incoming calls.
     */
    private String ruleName = null;
    private Date ruleStartTimeStamp = null;
    private Date ruleEndTimeStamp = null;
    private String longitude = null;
    private String latitude = null;
    private int proximity = 0; // 0 means that proximity is disabled
    private String callHandlingMethod = null;
    private String[] incomingNumbers = null;
    private String[] devicesList = null;
    private String ringingType = null;

    public String getCallHandlingMethod() {
        return callHandlingMethod;
    }

    public void setCallHandlingMethod(String callHandlingMethod) {
        this.callHandlingMethod = callHandlingMethod;
    }

    public String[] getDevicesList() {
        return devicesList;
    }

    public void setDevicesList(String[] devicesList) {
        this.devicesList = devicesList;
    }

    public String[] getIncomingNumbers() {
        return incomingNumbers;
    }

    public void setIncomingNumbers(String[] incomingNumbers) {
        this.incomingNumbers = incomingNumbers;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public int getProximity() {
        return proximity;
    }

    public void setProximity(int proximity) {
        this.proximity = proximity;
    }

    public String getRingingType() {
        return ringingType;
    }

    public void setRingingType(String ringingType) {
        this.ringingType = ringingType;
    }

    public Date getRuleEndTimeStamp() {
        return ruleEndTimeStamp;
    }

    public void setRuleEndTimeStamp(Date ruleEndTimeStamp) {
        this.ruleEndTimeStamp = ruleEndTimeStamp;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Date getRuleStartTimeStamp() {
        return ruleStartTimeStamp;
    }

    public void setRuleStartTimeStamp(Date ruleStartTimeStamp) {
        this.ruleStartTimeStamp = ruleStartTimeStamp;
    }

    @Override
    public String toString() {
        return "CallManagementRule [ruleName=" + ruleName
                + ", ruleStartTimeStamp=" + ruleStartTimeStamp
                + ", ruleEndTimeStamp=" + ruleEndTimeStamp + ", longitude="
                + longitude + ", latitude=" + latitude + ", proximity="
                + proximity + ", callHandlingMethod=" + callHandlingMethod
                + ", incomingNumbers=" + Arrays.toString(incomingNumbers)
                + ", devicesList=" + Arrays.toString(devicesList)
                + ", ringingType=" + ringingType + "]";
    }
    
}
