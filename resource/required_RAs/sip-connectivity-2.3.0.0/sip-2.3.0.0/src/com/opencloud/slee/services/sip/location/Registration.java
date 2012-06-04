package com.opencloud.slee.services.sip.location;

import java.util.List;

/**
 * Represents the SIP registration/location info for a subscriber
 */
public interface Registration {

    /**
     * Get the SIP Address-Of-Record for this registration
     */
    public String getAddressOfRecord();

    /**
     * Get the current list of contacts for this subscriber
     * @return an unmodifiable list of {@link Contact} objects.
     */
    public List getContacts();

    /**
     * Get the details associated with a single contact address
     */
    public Contact getContact(String contactURI);

    public Contact removeContact(String contactURI);

    public Contact getContact(FlowID flowID);

    public Contact removeContact(FlowID flowID);

    /**
     * Add a new contact address for this subscriber
     * @param uri SIP contact URI
     * @param q q-value
     * @param expires absolute expiry time, in ms
     * @param callId Call-Id for the register request that created this contact
     * @param cseq sequence number for the register request that created this contact
     * @param flowID
     * @return the new contact object
     */
    public Contact addContact(String uri, float q, long expires, String callId, int cseq, FlowID flowID);

    /**
     * Update an existing contact address for this subscriber
     * @param uri SIP contact URI
     * @param q q-value
     * @param expires absolute expiry time, in ms
     * @param callId Call-Id for the register request that created this contact
     * @param cseq sequence number for the register request that created this contact
     * @param flowID
     * @return the new contact object
     */
    public Contact updateContact(String uri, float q, long expires, String callId, int cseq, FlowID flowID);

    public interface Contact {
        public String getContactURI();
        public FlowID getFlowID();

        public float getQValue();
        public long getExpiryAbsolute();
        public long getExpiryDelta();
        public String getCallId();
        public int getCSeq();
    }
}
