package com.opencloud.slee.services.sip.presence;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.slee.ActivityContextInterface;
import javax.slee.Address;
import javax.slee.AddressPlan;
import javax.slee.InitialEventSelector;
import javax.slee.SbbContext;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.TraceLevel;

/**
 * New presence SBB. 
 * Should get a new SBB for every PresentityStateChangeEvent request received.
 * Queries profile table  for presence information.
 */
public abstract class EventStateCompositorSbb extends PresenceAwareSbb {

    protected String getTraceMessageType() { return "EventStateCompositorSbb"; }
    
    public void setSbbContext(SbbContext context) {
        super.setSbbContext(context);
        try {
            Context myEnv = (Context) new InitialContext().lookup("java:comp/env");
        } catch (Exception e) {
            severe("Could not set SBB context", e);
        } 
    }
    
    // Use this address to detect "our" timer events
    public static final Address TIMER_ADDRESS = new Address(AddressPlan.UNDEFINED, "ESC_TIMER");

    public InitialEventSelector checkTimerEvent(InitialEventSelector ies) {
        // Only trigger on timer events fired by this SBB
        Address address = ies.getAddress();
        ies.setInitialEvent(address != null ? address.equals(TIMER_ADDRESS) : false);
        return ies;
    }
    
    public void onPresentityStateChangeEvent(PresentityStateChangeEvent event, ActivityContextInterface aci) {
        
        if (isTraceable(TraceLevel.FINEST)) finest("PresentityStateChangeEvent event received by EventStateCompositorSbb: " + event.toString());
        aci.detach(getSbbLocalObject());
        updatePresenceInformation(event);
    }
    
    public void onExpiryTimer(TimerEvent timerEvent, PresenceACI aci) {
        
        if (isTraceable(TraceLevel.FINEST)) finest("onExpiryTimer event received by EventStateCompositorSbb");
        aci.detach(getSbbLocalObject());
        if (aci.getTimerCount() > 0) aci.setTimerCount(aci.getTimerCount() - 1);
        String[][] presenceInfo = aci.getPresenceInformation();
        
        // Cycle through the stored presence records. If any have expired, set them to null, and then remove them
        // Check whether removing records will result in a change of the presentity's composed presence state, and update if necessary
        if (presenceInfo != null) {
            PresentityState originalState = determineMostRecentPublishedPresentityState(presenceInfo);
            if (isTraceable(TraceLevel.FINEST)) finest("onExpiryTimer: initial presence state of presentity is: " + originalState.toString());
            for (int i = 0; i < presenceInfo.length; i++) {
                Long expiryTime = Long.parseLong(presenceInfo[i][PresenceACI.EXPIRY_TIME]);
                if (isTraceable(TraceLevel.FINEST)) finest("onExpiryTimer: current time is: " + System.currentTimeMillis() + "; expiry time of presence record is: " + expiryTime);
                if (expiryTime > 0 && expiryTime < System.currentTimeMillis()) presenceInfo[i] = null;
            }
            presenceInfo = removeNullRecords(presenceInfo);
            aci.setPresenceInformation(presenceInfo);
            if (isTraceable(TraceLevel.FINEST)) finest("onExpiryTimer: new presence state of presentity is: " + determineCurrentPresentityState(presenceInfo).toString());
            if (!originalState.equals(determineCurrentPresentityState(presenceInfo))) {
                if (isTraceable(TraceLevel.FINEST)) finest("onExpiryTimer: Presence state has changed, attempting to alert NotifySbbs");
                ActivityContextInterface subscriptionAci = getSubscriptionACI(aci.getSipUri());
                if (subscriptionAci != null) {
                    fireNotifyStateChangeEvent(new NotifyStateChangeEvent(aci.getSipUri(), determineCurrentPresentityState(presenceInfo)), subscriptionAci, null);
                }
            }
        }
        // If appropriate, unbind the PresenceACI from AC Naming
        unbindAciFromACNaming(aci);
    }
    
    public PresentityState getPresenceState(String uri) {
        return determineCurrentPresentityState(getOrCreatePresenceACI(uri).getPresenceInformation());
    }
    
    private void updatePresenceInformation (PresentityStateChangeEvent event) {
        
        if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: Updating presence information for " + event.getSipAddressOfRecord());
        
        PresenceACI presenceAci = getOrCreatePresenceACI(event.getSipAddressOfRecord());
        String[][] presenceInfo = presenceAci.getPresenceInformation();
        PresentityState oldState = presenceInfo == null ? new PresentityState(PresentityState.CLOSED, null) : determineCurrentPresentityState(presenceInfo);
        if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: oldState = " + oldState.toString());
        PresentityState newState = event.getPresenceState() == null ? oldState : event.getPresenceState();
        if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: initial value of newState = " + newState.toString());
        
        if (presenceInfo == null) {
            // no presence info for this presentity stored yet, so create new presence info record
            if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: No presence info found for " + event.getSipAddressOfRecord() + ", creating new presence record");
            presenceInfo = new String[1][];
            presenceInfo[0] = deriveStringArrayFromPresenceEvent(event);
        } else {
            // Determine and execute the required course of action: add/remove/update presence information
            //  Handle updates/removals of existing information first
            int i;
            int length = presenceInfo.length;
            if (event.getSipETag() != null && isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: received presence information derived from a PUBLISH request");
            for (i = 0; i < presenceInfo.length; i++) {
                if (event.getSipETag() != null) {
                    // from a PUBLISH request
                    if (event.getSipIfMatch() != null) {
                        // from a PUBLISH request that is modifying previously PUBLISHed presence info. Search for the record, and update/remove it
                        if ( presenceInfo[i][PresenceACI.ETAG] != null && event.getSipIfMatch().equals(presenceInfo[i][PresenceACI.ETAG])) {
                            if (event.getExpiryTime() <= System.currentTimeMillis()) {
                                // remove this record
                                if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: expiryTime of event is: " + event.getExpiryTime() + ", current time is: " + System.currentTimeMillis() + ", removing existing record");
                                presenceInfo = removeCurrentRecord(presenceInfo, i);
                                newState = determineCurrentPresentityState(presenceInfo);
                            } else {
                                // update the current record - E-Tag, expiry time, and presence info if appropriate
                                if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: expiryTime of event is: " + event.getExpiryTime() + ", current time is: " + System.currentTimeMillis() + ", updating existing record");
                                presenceInfo[i][PresenceACI.ETAG] = event.getSipETag();
                                presenceInfo[i][PresenceACI.EXPIRY_TIME] = String.valueOf(event.getExpiryTime());
                                if (event.getPresenceState() != null) {
                                    // if the request has presence info, then update the existing info
                                    presenceInfo[i][PresenceACI.BASIC] = event.getPresenceState().getBasicState();
                                    presenceInfo[i][PresenceACI.NOTE] = event.getPresenceState().getNoteState();
                                    newState = determineCurrentPresentityState(presenceInfo);
                                }
                            }
                            break;
                        }
                    }
                } else {
                    // from a REGISTER request. Update if presence state has changed
                    if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: received presence information derived from a REGISTER request...");
                    if (presenceInfo[i][PresenceACI.ETAG] == null) {
                        presenceInfo[i] = deriveStringArrayFromPresenceEvent(event);
                        if (event.getPresenceState().equals(new PresentityState(PresentityState.CLOSED, null))) {
                            // if the request was de-registering the presentity, remove that presentity's aci - the REGISTERed presence information is authoritative
                            newState = new PresentityState(PresentityState.CLOSED, null);
                            presenceAci.setPresenceInformation(presenceInfo);
                            unbindAciFromACNaming(presenceAci);
                        }
                        break;
                    }
                }    
            }
            // if neither removal or update of existing presence information, add new record
            if (i == length && isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: no matching records found, adding a new record...");
            if (i == length) presenceInfo = addNewPresenceRecord(presenceInfo, event);
        }
        
        // If required, set a timer to fire when the requested expiry time is exhausted
        if (event.getExpiryTime() > 0 && event.getExpiryTime() > System.currentTimeMillis()) setExpiryTimer(presenceAci, event.getExpiryTime());
    
        // update the presence information in the named ACI
        presenceAci.setPresenceInformation(presenceInfo);
        newState = determineCurrentPresentityState(presenceInfo);
        // Fire a NotifyStateChangeEvent event if appropriate
        if (getSubscriptionACI(event.getSipAddressOfRecord()) != null) {
            if (!oldState.equals(newState)) {
                if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: old and new states don't match, sending notification of state change...");
                fireNotifyStateChangeEvent(new NotifyStateChangeEvent(event.getSipAddressOfRecord(), newState), getSubscriptionACI(event.getSipAddressOfRecord()), null);
            }
            else {
                if (isTraceable(TraceLevel.FINEST)) finest("updatePresenceInformation: old and new states match, no notifications necessary");
            }
        }
    }
    
    private PresentityState determinePresentityState(String[][] presenceInformation, boolean onlyActiveStates) {
        
        if (presenceInformation == null) {
            if (isTraceable(TraceLevel.FINEST)) finest("No presence information stored for this presentity at present. Returning default presence state (closed/null)");
            return new PresentityState(PresentityState.CLOSED, null);
        }
        
        long submissionTime = 0;
        String basic = null;
        String note = null;
        for (int i = 0; i < presenceInformation.length; i++) {
            // check for an ETag: this indicates that the presence information was from a PUBLISH request
            if (presenceInformation[i][PresenceACI.ETAG] != null) {
                // use the most recent, valid PUBLISHed info that is available
                if (Long.parseLong(presenceInformation[i][PresenceACI.SUBMISSION_TIME]) > submissionTime) {
                    if (Long.parseLong(presenceInformation[i][PresenceACI.EXPIRY_TIME]) > System.currentTimeMillis() || !onlyActiveStates) {
                        submissionTime = Long.parseLong(presenceInformation[i][PresenceACI.SUBMISSION_TIME]);
                        basic = presenceInformation[i][PresenceACI.BASIC];
                        note = presenceInformation[i][PresenceACI.NOTE];
                    }
                }
            } else {
                // If there's no ETag, then the info is from a REGISTER.
                //  Use this info if the user is online, and no other information has been PUBLISHed.
                if (basic == null) {
                    basic = presenceInformation[i][PresenceACI.BASIC];
                    note = presenceInformation[i][PresenceACI.NOTE];
                }
                // If the REGISTERed state is Offline/null, use this as the definitive state
                if (basic.equalsIgnoreCase(PresentityState.CLOSED) && note == null) return new PresentityState(basic, null);
            }
        }
        
        if (basic == null) basic = PresentityState.CLOSED;
        if (isTraceable(TraceLevel.FINEST)) finest("determinePresentityState: Determined presence state of: " + basic + "/" + note);
        return new PresentityState(basic, note);
    }
    
    private PresentityState determineCurrentPresentityState(String[][] presenceInformation) throws NullPointerException {
        return determinePresentityState(presenceInformation, true);
    }
    
    private PresentityState determineMostRecentPublishedPresentityState (String[][] presenceInformation) throws NullPointerException {
        return determinePresentityState(presenceInformation, false);
    }
    
    private String[][] addNewPresenceRecord(String[][] presenceInfo, PresentityStateChangeEvent event) {
        
        if (isTraceable(TraceLevel.FINEST)) finest("Adding new record to presence info for: " + event.getSipAddressOfRecord());
        String[][] newPresenceInfo = new String[presenceInfo.length + 1][];
        for (int i = 0; i < presenceInfo.length; i++) {
            newPresenceInfo[i] = new String[presenceInfo[i].length];
            System.arraycopy(presenceInfo[i], 0, newPresenceInfo[i], 0, presenceInfo[i].length);
        }
        newPresenceInfo[newPresenceInfo.length - 1] = deriveStringArrayFromPresenceEvent(event);

        return newPresenceInfo;
    }
    
    private String[][] removeCurrentRecord(String[][] presenceInfo, int index) {
        
        String[][] newPresenceInfo = new String[presenceInfo.length - 1][];
        
        for (int i = 0; i < presenceInfo.length; i++) {
            int destIndex;
            if (i != index) {
                destIndex = i < index ? i : i -1;
                newPresenceInfo[destIndex] = new String[presenceInfo[i].length];
                System.arraycopy(presenceInfo[i], 0, newPresenceInfo[destIndex], 0, presenceInfo[i].length);
            }
        }
        return newPresenceInfo;
    }
    
    private String[][] removeNullRecords(String[][] presenceInfo) {
        
        List tempList = new ArrayList();
        for (int i = 0; i < presenceInfo.length; i++) {
            if (presenceInfo[i] != null) tempList.add(presenceInfo[i]);
        }
        
        String[][] output = new String[tempList.size()][];
        for (int i = 0; i < tempList.size(); i++) {
            output[i] = (String[])tempList.get(i);
        }
        
        return output;
    }
    
    private void setExpiryTimer(PresenceACI presenceAci, long expiryTime) {
        presenceAci.setTimerCount(presenceAci.getTimerCount() + 1);
        getTimerFacility().setTimer(presenceAci, TIMER_ADDRESS, expiryTime + expiryOffset, new TimerOptions());
        if (isTraceable(TraceLevel.FINEST)) finest("setExpiryTimer: Starting timer, setting to fire at " + expiryTime);
    }
                                 
    
    private void unbindAci(PresenceACI aci) {
        if (isTraceable(TraceLevel.FINEST)) finest("unbindAci: unbinding aci from AC naming, using uri: " + aci.getSipUri());
        if (getPresenceACI(aci.getSipUri()) != null) removePresenceACI(aci.getSipUri());
    }
    
    private void unbindAciFromACNaming(PresenceACI aci) {
        
        String[][] presenceInfo = aci.getPresenceInformation();
        if (presenceInfo == null || (presenceInfo.length == 1 && determineCurrentPresentityState(presenceInfo).equals((new PresentityState(PresentityState.CLOSED, null))))) {
            // if there is only one presence record, and its presence info 'closed/(empty)', then that is equivalent to the default
            //  presence status of the presentity, and the ACI can be unbound with AC Naming, and made available for GC.
            if (aci.getTimerCount() == 0) {
                if (isTraceable(TraceLevel.FINEST)) finest("unbindAciFromACNaming: attempting to unbind aci bound with uri of: " + aci.getSipUri());
                unbindAci(aci);
            }
        }
    }
        
    private String[] deriveStringArrayFromPresenceEvent(PresentityStateChangeEvent event) {
        
        String[] presenceArray = new String[PresenceACI.ARRAY_SIZE];
        presenceArray[PresenceACI.URI] = event.getSipAddressOfRecord();
        presenceArray[PresenceACI.ETAG] = event.getSipETag();
        presenceArray[PresenceACI.BASIC] = event.getPresenceState() == null ? null : event.getPresenceState().getBasicState();
        presenceArray[PresenceACI.NOTE] = event.getPresenceState() == null ? null : event.getPresenceState().getNoteState();
        presenceArray[PresenceACI.SUBMISSION_TIME] = String.valueOf(event.getSubmissionTime());
        presenceArray[PresenceACI.EXPIRY_TIME] = String.valueOf(event.getExpiryTime());
        
        return presenceArray;
    }
    
    protected PresenceACI getOrCreatePresenceACI(String sipUri) {
        PresenceACI presenceAci = asSbbActivityContextInterface(super.getOrCreatePresenceACI(sipUri));
        presenceAci.setSipUri(sipUri);
        return presenceAci;
    }
    
    private long expiryOffset = 100;
    
    public abstract void fireNotifyStateChangeEvent(NotifyStateChangeEvent event, ActivityContextInterface aci, javax.slee.Address address);
    public abstract PresenceACI asSbbActivityContextInterface(ActivityContextInterface aci);
    
}
