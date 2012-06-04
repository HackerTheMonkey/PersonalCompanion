package com.opencloud.slee.services.sip.presence;

import javax.slee.ActivityContextInterface;
import javax.slee.facilities.NameNotBoundException;

import com.opencloud.slee.services.sip.common.OCSipSbb;

public abstract class PresenceAwareSbb extends OCSipSbb {

    private String getSubscriptionKey(String sipAddressOfRecord) {
        if (sipAddressOfRecord == null) throw new NullPointerException("sipAddressOfRecord");
        return "sipSubscribe_" + sipAddressOfRecord;
    }
    
    private String getPresenceKey(String sipAddressOfRecord) {
        if (sipAddressOfRecord == null) throw new NullPointerException("sipAddressOfRecord");
        return "sipPresence_" + sipAddressOfRecord;
    }
    
    protected ActivityContextInterface getSubscriptionACI(String sipAddressOfRecord) {
        return new PresenceAciManager(sipAddressOfRecord).getSubscriptionAci();
    }
    
    protected ActivityContextInterface getPresenceACI(String sipAddressOfRecord) {
        return new PresenceAciManager(sipAddressOfRecord).getPresenceAci();
    }
    
    protected void removeSubscriptionACI(String sipAddressOfRecord) {
        try {
            getACNamingFacility().unbind(getSubscriptionKey(sipAddressOfRecord));
        } catch (NameNotBoundException e) {
            warn("Error encountered unbinding ACI name: " + getSubscriptionKey(sipAddressOfRecord), e);
        }
    }
    
    protected void removePresenceACI(String sipAddressOfRecord) {
        try {
            getACNamingFacility().unbind(getPresenceKey(sipAddressOfRecord));
        } catch (NameNotBoundException e) {
            warn("Error encountered unbinding ACI name: " + getPresenceKey(sipAddressOfRecord), e);
        }
    }

    protected ActivityContextInterface getOrCreateSubscriptionACI (String sipAddressOfRecord) {
        return new PresenceAciManager(sipAddressOfRecord).getOrCreateSubscriptionAci();
    }
    
    protected ActivityContextInterface getOrCreatePresenceACI (String sipAddressOfRecord) {
        return new PresenceAciManager(sipAddressOfRecord).getOrCreatePresenceAci();
    }
    
    /**
     * Inner class for management of acis.
     * Various sub-classes of PresenceAwareSbb require references to  a given presentity's presence aci, 
     * subscription aci, or both. If the order in which these references are obtained is not consistent, 
     * it possible for deadlocks to occur. The use of this class ensures that references to the acis
     * are always obtained in the same order (presence aci then subscription aci), preventing deadlock conditions.
     */
    private class PresenceAciManager {
        
        PresenceAciManager (String addressOfRecord) {
            if (addressOfRecord == null) throw new NullPointerException("Error creating PresenceAciManager: addressOfRecord is null");
            this.addressOfRecord = addressOfRecord;
            this.presenceAci = getACNamingFacility().lookup(getPresenceKey(addressOfRecord));
            this.subscriptionAci = getACNamingFacility().lookup(getSubscriptionKey(addressOfRecord));
        }
        
        public ActivityContextInterface getPresenceAci() {
            return presenceAci;
        }
        
        public ActivityContextInterface getSubscriptionAci() {
            return subscriptionAci;
        }
        
        public ActivityContextInterface getOrCreateSubscriptionAci() {
            // Check to see whether there is already a subscription ACI for the subscribed presentity, if not create one
            return getSubscriptionAci() == null ? createAci(getSubscriptionKey(addressOfRecord)) : getSubscriptionAci();
        }
        
        public ActivityContextInterface getOrCreatePresenceAci() {
            // Check to see whether there is already a presence ACI for the presentity, if not create one
            return getPresenceAci() == null ? createAci(getPresenceKey(addressOfRecord)) : getPresenceAci();
        }
        
        private ActivityContextInterface createAci(String key) {
            ActivityContextInterface nullAci = null;
            try {
                // create a null activity interface for the ACI, and bind the provided key to it 
                nullAci = getNullACIFactory().getActivityContextInterface(getNullActivityFactory().createNullActivity());
                getACNamingFacility().bind(nullAci, key);
            }
            catch (Exception e) {
                warn("Error encountered binding ACI to name: " + key, e);
            }
            return nullAci;
        }
        
        private String addressOfRecord;
        private ActivityContextInterface presenceAci;
        private ActivityContextInterface subscriptionAci;
        
    }
}
