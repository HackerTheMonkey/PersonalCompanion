package com.opencloud.slee.services.sip.location.jdbc;

import com.opencloud.slee.services.sip.location.Registration;
import com.opencloud.slee.services.sip.common.BaseSbb;
import com.opencloud.slee.services.sip.location.ContactImpl;
import com.opencloud.slee.services.sip.location.FlowID;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.*;
import javax.slee.facilities.TimerEvent;
import javax.slee.facilities.TimerOptions;
import javax.slee.facilities.TraceLevel;
import javax.slee.facilities.Tracer;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Uses JDBC to implement a SIP location service.
 * <p>
 * The database schema used is very simple and is intended for demonstration
 * purposes only.<p>
 * <b>Registrations Table</b><br>
 * This table stores a record for each contact address that the user currently
 * has registered.<p>
 * <b>PostgreSQL Schema:</b>
 * <pre>
 * CREATE TABLE registrations (
 *   sipaddress varchar(80) not null,          -- user's public SIP address
 *   contactaddress varchar(80) not null,      -- contact address for this registration
 *   expiry bigint,                            -- expiry time for this registration
 *   qvalue integer,                           -- weighting (1.0 >= qvalue >= 0.0) * 1000
 *   cseq integer,                             -- sequence number of register request
 *   callid varchar(80),                       -- SIP call-id of register request
 *   flowid varchar(80),                       -- Flow ID for incoming persistent connection
 *   PRIMARY KEY (sipaddress, contactaddress)
 * );
 * </pre>
 * <p>
 * <b>Oracle Schema:</b>
 * <pre>
 * CREATE TABLE registrations (
 *   sipaddress varchar(80) not null,          -- user's public SIP address
 *   contactaddress varchar(80) not null,      -- contact address for this registration
 *   expiry number(22),                        -- expiry time for this registration
 *   qvalue number(5),                         -- weighting (1.0 >= qvalue >= 0.0) * 1000
 *   cseq number(9),                           -- sequence number of register request
 *   callid varchar(80),                       -- SIP call-id of register request
 *   flowid varchar(80),                       -- Flow ID for incoming persistent connection
 * );
 * CREATE UNIQUE INDEX PK_registrations ON registrations (sipaddress, contactaddress);
 * </pre>
 * <b>Expiration of registrations</b><br>
 * By default, the SBB will set a SLEE timer on a null activity context for each new
 * or updated registration entry.
 * The timer event is an inital event, so the SLEE creates an SBB entity to
 * process the event and remove any expired entries.<br>
 * The use of SLEE timers can be disabled so that an external process can be used
 * to remove expired entries from the database, if desired. To disable timers, set the
 * <b>expirationEnabled</b> env-entry to <code>false</code>.
 */
public abstract class JDBCLocationSbb extends BaseSbb {

    // Use this address to detect "our" timer events
    public static final Address TIMER_ADDRESS = new Address(AddressPlan.UNDEFINED, "JDBC_TIMER");

    public void setSbbContext(SbbContext context) {
        super.setSbbContext(context);
        try {
            Context myEnv = (Context) new InitialContext().lookup("java:comp/env");
            ds = (DataSource) myEnv.lookup("jdbc/SipRegistry");
            expirationEnabled = ((Boolean)myEnv.lookup("expirationEnabled")).booleanValue();
            usage = getDefaultSbbUsageParameterSet();
        } catch (NamingException ne) {
            severe("Could not set SBB context", ne);
        }
    }

    public InitialEventSelector checkTimerEvent(InitialEventSelector ies) {
        // Only trigger on timer events fired by this SBB
        Address address = ies.getAddress();
        ies.setInitialEvent(address != null ? address.equals(TIMER_ADDRESS) : false);
        return ies;
    }

    // Initial event - get the registration info from ACI and remove expired
    // registrations
    public void onTimerEvent(TimerEvent event, RegistrationExpiryACI regExpiryACI) {
        regExpiryACI.detach(getSbbLocalObject());

        String publicAddress = regExpiryACI.getAddressOfRecord();
        String contactAddress = regExpiryACI.getContactAddress();
        FlowID flowID = regExpiryACI.getFlowID();

        if (isTraceable(TraceLevel.FINER)) finer("received expiration timer event for contact: " + publicAddress + "/" + contactAddress + ", flow=" + flowID);

        Registration reg = getRegistration(publicAddress);
        Registration.Contact contact = flowID == null ? reg.getContact(contactAddress) : reg.getContact(flowID);

        if (contact == null) {
            if (isTraceable(TraceLevel.FINER)) finer("contact " + publicAddress + "/" + contactAddress + " not found, nothing to do");
            return;
        }

        // Only expire if Call-Id and CSeq match. If they do not match, this means
        // the registration has been updated since the timer was started.
        String origCallId = regExpiryACI.getCallId();
        int origCSeq = regExpiryACI.getCSeq();

        if (origCallId.equals(contact.getCallId()) && origCSeq == contact.getCSeq()) {
            if (isTraceable(TraceLevel.FINER)) finer("contact " + publicAddress + "/" + contactAddress + " has expired, removing");
            reg.removeContact(contactAddress);
            updateRegistration(reg);
        }
        else {
            if (isTraceable(TraceLevel.FINER)) finer("contact " + publicAddress + "/" + contactAddress + " has been updated, not removing");
        }
    }

    // LocationService local interface impl

    public Registration getRegistration(String uri) {
        return loadRegistration(uri);
    }

    public Registration getOrCreateRegistration(String uri) {
        return loadRegistration(uri);
    }

    public void updateRegistration(Registration reg) {
        if (isTraceable(TraceLevel.FINEST)) finest("updateRegistration: " + reg.getAddressOfRecord());
        try {
            ((RegistrationImpl)reg).updateDB();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeRegistration(Registration reg) {
        if (isTraceable(TraceLevel.FINEST)) finest("removeRegistration: " + reg.getAddressOfRecord());
        try {
            ((RegistrationImpl)reg).deleteAllRecords();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RegistrationImpl loadRegistration(String uri) {
        try {
            return new RegistrationImpl(uri, ds, getSbbTracer(), usage);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RegistrationExpiryACI createExpiryACI(String publicAddress, String contactAddress, String callId, int cseq, FlowID flow) {
        try {
            ActivityContextInterface nullACI = getNullACIFactory().getActivityContextInterface(getNullActivityFactory().createNullActivity());
            RegistrationExpiryACI regExpiryACI = asSbbActivityContextInterface(nullACI);
            regExpiryACI.setAddressOfRecord(publicAddress);
            regExpiryACI.setContactAddress(contactAddress);
            regExpiryACI.setCallId(callId);
            regExpiryACI.setCSeq(cseq);
            regExpiryACI.setFlowID(flow);
            return regExpiryACI;
        } catch (UnrecognizedActivityException e) {
            throw new RuntimeException(e); // should never happen with null activities
        }
    }

    private void startExpiryTimer(String publicAddress, Registration.Contact contact) {
        if (!expirationEnabled) return;
        RegistrationExpiryACI aci = createExpiryACI(publicAddress, contact.getContactURI(), contact.getCallId(), contact.getCSeq(), contact.getFlowID());
        getTimerFacility().setTimer(aci, TIMER_ADDRESS, contact.getExpiryAbsolute(), new TimerOptions());
        if (isTraceable(TraceLevel.FINEST)) finest("set registration expiry timer for " + contact.getContactURI() + ", expires in " + contact.getExpiryDelta() + "ms");
    }

    protected String getTraceMessageType() {
        return "JDBCLocationSbb";
    }

    public abstract RegistrationExpiryACI asSbbActivityContextInterface(ActivityContextInterface aci);

    private class RegistrationImpl implements Registration {
        PreparedStatement query;
        PreparedStatement insert;
        PreparedStatement deleteAll;
        PreparedStatement delete;
        PreparedStatement update;
        JDBCLocationSbbUsage usage;
        
        private RegistrationImpl(String addressOfRecord, DataSource ds, Tracer tracer, JDBCLocationSbbUsage usage) throws SQLException {
            this.conn = ds.getConnection();
            this.tracer = tracer;
            this.usage = usage;
            this.addressOfRecord = addressOfRecord;
            
            // do JDBC query to init contacts
            this.contacts = initContacts();
        }

        private ArrayList initContacts() throws SQLException {
            if (query == null) query = conn.prepareStatement(queryGetUser);
            query.setString(1, addressOfRecord);
            ResultSet results = query.executeQuery();
            ArrayList newContacts = new ArrayList(2);
            while (results.next()) {
                String contactAddress = results.getString(2);
                long expires = results.getLong(3);
                int q = results.getInt(4); // an int between 0 and 1000, translates to
                                           // qvalue between 0.000 and 1.000
                long cseq = results.getInt(5);
                String callid = results.getString(6);
                String flowid = results.getString(7);

                newContacts.add(new ContactImpl(contactAddress, ((float)q)/1000, expires, callid, (int)cseq, flowid == null ? null : FlowID.fromString(flowid)));
                if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("read record for " + addressOfRecord + "/" + contactAddress);
            }
            results.close();
            query.close();
            
            // count all records we read (maybe 0)
            usage.incrementRecordsRead(newContacts.size());
            // count every attempt to read registrations
            usage.incrementRegistrationsRead(1);
            
            return newContacts;
        }
        
        private void deleteAllRecords() throws SQLException {
            if (deleteAll == null) deleteAll = conn.prepareStatement(deleteAllRegistrations);
            // delete existing records
            deleteAll.setString(1, addressOfRecord);
            deleteAll.execute();
            deleteAll.close();
        }

        private void deleteRecords() throws SQLException {
            // delete existing records
            if (null == removeContacts) return;
            
            for (Iterator iter = removeContacts.iterator(); iter.hasNext();) {
                Object next = iter.next();
                if (next instanceof FlowID) {
                    FlowID flowID = (FlowID)next;
                    if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("deleting record for " + getAddressOfRecord() + "/" + flowID);
                    deleteContact(flowID);
                }
                else {
                    String contactURI = (String)next;
                    if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("deleting record for " + getAddressOfRecord() + "/" + contactURI);
                    deleteContact(contactURI);
                }
            }
            
            usage.incrementRecordsRemoved(removeContacts.size());
            removeContacts.clear();
        }
        
        private void deleteContact(String contactURI) throws SQLException {
            if (delete == null) delete = conn.prepareStatement(deleteRegistration);
            delete.setString(1, addressOfRecord);
            delete.setString(2, contactURI);
            delete.execute();

            // sanity check what we just did to the db
            if (delete.getUpdateCount() != 1)
                tracer.warning("Update for " + addressOfRecord + "/" + contactURI + " hit "+delete.getUpdateCount()+" rows");

            delete.close();
        }

        private void deleteContact(FlowID flowID) throws SQLException {
            if (delete == null) delete = conn.prepareStatement(deleteRegistrationByFlow);
            delete.setString(1, addressOfRecord);
            delete.setString(2, flowID.toString());
            delete.execute();

            // sanity check what we just did to the db
            if (delete.getUpdateCount() != 1)
                tracer.warning("Update for " + addressOfRecord + "/" + flowID + " hit "+delete.getUpdateCount()+" rows");

            delete.close();
        }

        private void updateRecords() throws SQLException {
            // delete all existing records
            if (null == updateContacts) return;
            
            for (Iterator iter = updateContacts.iterator(); iter.hasNext();) {
                ContactImpl contact = (ContactImpl) iter.next();
                if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("updating record for " + getAddressOfRecord() + "/" + contact.getContactURI());
                updateContact(contact);
            }
            
            usage.incrementRecordsUpdated(updateContacts.size());
            updateContacts.clear();
        }
        
        private void updateContact(ContactImpl contact) throws SQLException {
            if (update == null) update = conn.prepareStatement(updateRegistration);
            // update an existing contact record in the DB  
            // set ...
            update.setLong(1, contact.getExpiryAbsolute());
            update.setFloat(2, contact.getQValue());
            update.setLong(3, contact.getCSeq());
            update.setString(4, contact.getCallId());
            update.setString(5, contact.getFlowID() == null ? null : contact.getFlowID().toString());
            // where ...
            update.setString(6, addressOfRecord);
            update.setString(7, contact.getContactURI());
            update.execute();
            
            // sanity check what we just did to the db
            if (update.getUpdateCount() != 1)
                tracer.warning("Update for " + addressOfRecord + "/" + contact.getContactURI() + " hit "+update.getUpdateCount()+" rows");

            update.close();

            startExpiryTimer(addressOfRecord, contact);
        }

        private void insertRecords() throws SQLException {
            // delete all existing records
            if (null == insertContacts) return;
            
            for (Iterator iter = insertContacts.iterator(); iter.hasNext();) {
                ContactImpl contact = (ContactImpl) iter.next();
                if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("inserting record for " + getAddressOfRecord() + "/" + contact.getContactURI());
                insertContact(contact);
            }
            
            usage.incrementRecordsInserted(insertContacts.size());
            insertContacts.clear();
        }
        
        private void insertContact(ContactImpl contact) throws SQLException {
            if (insert == null) insert = conn.prepareStatement(insertRegistration);
            // insert a new contact record into the DB        
            insert.setString(1, addressOfRecord);
            insert.setString(2, contact.getContactURI());
            insert.setLong(3, contact.getExpiryAbsolute());
            insert.setInt(4, (int)(contact.getQValue() * 1000));
            insert.setInt(5, contact.getCSeq());
            insert.setString(6, contact.getCallId());
            insert.setString(7, contact.getFlowID() == null ? null : contact.getFlowID().toString());
            insert.execute();
            insert.close();

            startExpiryTimer(addressOfRecord, contact);
        }

        private void updateDB() throws SQLException {
            if (dirty) {
                deleteRecords();
                if (contacts.size() > 0) {
                    insertRecords();
                    updateRecords();
                }
                else if (tracer.isTraceable(TraceLevel.FINEST)) {
                    tracer.finest("no contacts remain, removed record for " + getAddressOfRecord());
                }
            }
        }

        public String getAddressOfRecord() { return addressOfRecord; }

        public List getContacts() {
            return Collections.unmodifiableList(contacts);
        }

        public Contact getContact(String contactURI) {
            int idx = findIndex(contactURI);
            return idx == -1 ? null : (Contact) contacts.get(idx);
        }

        public Contact getContact(FlowID flowID) {
            int idx = findIndex(flowID);
            return idx == -1 ? null : (Contact) contacts.get(idx);
        }

        public Contact removeContact(String contactURI) {
            int idx = findIndex(contactURI);
            if (idx == -1) return null;
            else {
                dirty = true;
                if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("removeContact: " + contactURI);
                ContactImpl contact = (ContactImpl) contacts.remove(idx);
                if (null == removeContacts) removeContacts = new ArrayList(2);
                removeContacts.add(contactURI);
                return contact;
            }
        }
        
        public Contact removeContact(FlowID flowID) {
            int idx = findIndex(flowID);
            if (idx == -1) return null;
            else {
                dirty = true;
                if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("removeContact: flow=" + flowID);
                ContactImpl contact = (ContactImpl) contacts.remove(idx);
                if (null == removeContacts) removeContacts = new ArrayList(2);
                removeContacts.add(flowID);
                return contact;
            }
        }

        public Contact updateContact(String contactURI, float q, long expires, String callId, int cseq, FlowID flowID) {
            int idx = findIndex(contactURI);
            if (idx == -1) return null;
            else {
                dirty = true;
                if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("updateContact: " + contactURI);
                contacts.remove(idx);
                ContactImpl newContact = new ContactImpl(contactURI, q, expires, callId, cseq, flowID);
                contacts.add(newContact);
                if (null == updateContacts) updateContacts = new ArrayList(2);
                updateContacts.add(newContact);
                return newContact;
            }
        }

        public Contact addContact(String contactURI, float q, long expires, String callId, int cseq, FlowID flowID) {
            ContactImpl newContact = new ContactImpl(contactURI, q, expires, callId, cseq, flowID);
            contacts.add(newContact);
            if (null == insertContacts) insertContacts = new ArrayList(2);
            insertContacts.add(newContact);
            if (tracer.isTraceable(TraceLevel.FINEST)) tracer.finest("addContact: " + contactURI);
            dirty = true;
            return newContact;
        }

        private int findIndex(String uri) {
            for (int i = 0; i < contacts.size(); i++) {
                Contact c = (Contact) contacts.get(i);
                if (c.getContactURI().equalsIgnoreCase(uri)) return i;
            }
            return -1;
        }

        private int findIndex(FlowID flow) {
            for (int i = 0; i < contacts.size(); i++) {
                Contact c = (Contact) contacts.get(i);
                FlowID f = c.getFlowID();
                if (f != null && f.equals(flow)) return i;
            }
            return -1;
        }

        private final String addressOfRecord; // Full sip URI, ie. includes sip: prefix
                                              // Prefix will be stripped off for queries
        private final Connection conn;
        private final ArrayList contacts;
        private ArrayList updateContacts = null;
        private ArrayList removeContacts = null;
        private ArrayList insertContacts = null;
        private final Tracer tracer;
        private boolean dirty = false;

        private static final String deleteAllRegistrations = "delete from registrations where sipaddress = ? ";

        private static final String queryGetUser = "select * from registrations where sipaddress = ?";

        private static final String deleteRegistration = "delete from registrations where sipaddress = ? and contactaddress = ?";
        private static final String deleteRegistrationByFlow = "delete from registrations where sipaddress = ? and flowid = ?";
        private static final String updateRegistration = "update registrations " +
                                                         "set expiry = ?, qvalue = ?, cseq = ?, callid = ?, flowid = ? " +
                                                         "where sipaddress = ? and contactaddress = ?";
        private static final String insertRegistration = "insert into registrations values ( ?, ?, ?, ?, ?, ?, ? )";
    }

    public abstract JDBCLocationSbbUsage getDefaultSbbUsageParameterSet();
    private JDBCLocationSbbUsage usage;
    private DataSource ds;
    private boolean expirationEnabled;
}
