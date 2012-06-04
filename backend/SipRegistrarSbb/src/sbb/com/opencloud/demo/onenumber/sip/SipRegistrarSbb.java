package com.opencloud.demo.onenumber.sip;

import com.opencloud.demo.onenumber.sip.base.BaseSbb;
import com.opencloud.javax.sip.slee.OCSipActivityContextInterfaceFactory;
import com.opencloud.javax.sip.slee.OCSleeSipProvider;
import com.opencloud.slee.services.location.LocationDbProfileLocalInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.profile.ProfileLocalObject;
import javax.slee.profile.ProfileTable;

/**
 *
 * @author Hasanein.Khafaji
 */
public abstract class SipRegistrarSbb extends BaseSbb
{

    private HashMap<String, ArrayList> locationDatabaseHashMap = new HashMap<String, ArrayList>();
    private boolean debugEnabled = true;
    /**
     * This table should have been administratively created using the rhino
     * command line console or the web console.
     */
    private final String LOCATION_DATABASE_PROFILE_TABLE_NAME = "LocationProfileTable";
    private String[] supportedDomains =
    {
        "opencloud.com",
        "example.com",
        "test.com"
    };
    /**
     * This simple implementation of the SIP registrar is basically assuming
     * that registration is being done from a single device and any registration
     * requests sent from multiple devices for the same AOR will override each
     * other, depending on which request arrives the first.
     * @param sipRequestEvent
     * @param aci
     */
    public void onSipRegisterRequest(RequestEvent sipRequestEvent, ActivityContextInterface aci)
    {
        tracer.info("SIP REGISTER event has been received...");
        /**
         * Read the JNDI environment of this particular SBB and initialise
         * the local variables accordingly.
         */
        tracer.info("Traversing the JNDI environment for the OC SIP utility classes...");
        readJNDI();
        /**
         * Extracting some important header field values from the received
         * SIP REGISTER message
         */
        Request registerRequest = sipRequestEvent.getRequest();
        SipURI requestUri = (SipURI) registerRequest.getRequestURI();

        /**
         * Get the domain of the Request-Uri to be compared with
         * the domain that this registrar SBB is responsible for
         */
        String receivedDomainName = requestUri.getHost();

        if (isDomainNameSupported(receivedDomainName))
        {
            ListIterator contactHeadersIterator = registerRequest.getHeaders(ContactHeader.NAME);
            ArrayList contactAddresses = getContacts(contactHeadersIterator);

            int registerRequestType = getRegisterRequestType(registerRequest, contactAddresses);

            switch (registerRequestType)
            {
                /**
                 * Query Request
                 */
                case 0:
                    processQueryBinding(sipRequestEvent);
                    break;
                /**
                 * Add Request
                 */
                case 1:
                    tracer.info("ADD RECEIVED......:::::::::::::::::::::::::::::::::::");
                    processAddBinding(sipRequestEvent);
                    break;
                /**
                 * Remove Request
                 */
                case 2:
                    processRemoveBinding(sipRequestEvent);
            }
        }
        else
        {
            tracer.severe("REGISTER request received for a not-supported domain: " + receivedDomainName);
            tracer.info("Sending a 404 NOT FOUND response...");
            sendSipResponse(null, sipRequestEvent, 404);
            tracer.info("REGISTER Request: \n" + registerRequest);
        }

        /**
         * Detaching from the activity after completing the processing
         * of the REGISTER request.
         */
        aci.detach(sbbContext.getSbbLocalObject());
    }

    private void processAddBinding(RequestEvent sipRequestEvent)
    {
        tracer.info("SIP REGISTER request has been received of type ADD");
        /**
         * Get the AOR from the To header field
         */
        SipURI toHeaderFieldValue = (SipURI) ((ToHeader) sipRequestEvent.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI();
        String addressOfRecord = getCanonicalAddress(toHeaderFieldValue);
        if (debugEnabled)
        {
            tracer.info("DEBUG: Address-Of-Record: " + addressOfRecord);
        }
        /**
         * Get the entity responsible about the registration request, i.e. the
         * value of the From header field.
         */
        SipURI fromHeaderFieldValue = (SipURI) ((FromHeader) sipRequestEvent.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI();
        String fromHeaderFieldCanonicalValue = getCanonicalAddress(fromHeaderFieldValue);
        /**
         * Prepare the ArrayList that contains the information to be stored
         * in the Location Database. The elements should be inserted in the
         * constructed ArrayList in the following order:
         * 1- The list of the bound contact header fields
         * 2- The Call-ID
         * 3- The Call Sequence
         * 4- Whether or not the registration request is initiated by
         * a 3rd party entity.
         */
        ArrayList valueArrayList = new ArrayList();
        /**
         * Populate the ArrayList with the extracted information.
         */
        ArrayList<ContactHeader> contactHeaders = getContacts(sipRequestEvent.getRequest().getHeaders(ContactHeader.NAME));//1
        String callID = ((CallIdHeader) sipRequestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();//2
        long cSeq = ((CSeqHeader) sipRequestEvent.getRequest().getHeader(CSeqHeader.NAME)).getSeqNumber();//3
        boolean is3rdPartyRegistration = (addressOfRecord.equals(fromHeaderFieldCanonicalValue)) ? false : true;//4
        /**
         * Populate the LocationDatabase with the extracted information.
         */
        valueArrayList.add(contactHeaders); //0
        valueArrayList.add(callID);//1
        valueArrayList.add(cSeq);//2
        valueArrayList.add(is3rdPartyRegistration);//3
        /**
         * Store the collected information into the Location Database
         * The passed AOR will represent the name of the profile to be created
         * inside the Location Database profile table in order to store this
         * location information.
         *
         * The valueArrayList is the content of the profile.(the location
         * information).
         */
        storeBindingInformation(addressOfRecord, valueArrayList);
        /**
         * Send the SIP response back
         */
        sendSipResponse(contactHeaders, sipRequestEvent, 200);
    }

    private void processRemoveBinding(RequestEvent sipRequestEvent)
    {
        tracer.info("DEBUG: SIP REGISTER request has been received of type REMOVE");
        /**
         * Get the address-of-record for which the binding is to be removed
         */
        SipURI toHeaderFieldValue = (SipURI) ((ToHeader) sipRequestEvent.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI();
        String addressOfRecord = getCanonicalAddress(toHeaderFieldValue);
        /**
         * get the contact header fields to be disassociated from the given
         * address-of-record
         */
        ArrayList<ContactHeader> contactsArrayList = getContacts(sipRequestEvent.getRequest().getHeaders(ContactHeader.NAME));
        ContactHeader[] contactHeaders = new ContactHeader[contactsArrayList.size()];
        contactHeaders = contactsArrayList.toArray(contactHeaders);
        /**
         * Remove the binding information from the Location Database
         */
        removeBindingInformation(addressOfRecord, contactHeaders);
        /**
         * Respond back to the requestor with a 200 OK response message.
         */
        tracer.info("AOR: " + addressOfRecord + " has been removed from the Location Database...");
        sendSipResponse(null, sipRequestEvent, 200);
    }

    /**
     * This method should result in generating a SIP 200 OK response back
     * to the sender of the REGISTER request. This response should contain
     * a list of all the contact header fields that this SIP server is
     * responsible for or an OK message without a corresponding contact header
     * field.
     * 
     * @param sipRequestEvent
     */
    private void processQueryBinding(RequestEvent sipRequestEvent)
    {
        tracer.info("SIP REGISTER request has been received of type QUERY");
        /**
         * Get the address-of-record for which the query is to be performed.
         */
        SipURI toHeaderFieldValue = (SipURI) ((ToHeader) sipRequestEvent.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI();
        String addressOfRecord = getCanonicalAddress(toHeaderFieldValue);
        /**
         * Query the binding information for the given AOR from the Location
         * Database
         */
        ArrayList<ContactHeader> contactHeaders = queryBindingInformation(addressOfRecord);

        if (contactHeaders != null)
        {
            sendSipResponse(contactHeaders, sipRequestEvent, 200);
        }
        else
        {
            sendSipResponse(null, sipRequestEvent, 200);
        }
    }

    private ArrayList<ContactHeader> getContacts(ListIterator contactHeadersIterator)
    {
        ArrayList contactsArrayList = new ArrayList();
        while (contactHeadersIterator.hasNext())
        {
            ContactHeader contactHeader = (ContactHeader) contactHeadersIterator.next();
            contactsArrayList.add(contactHeader);
        }
        return contactsArrayList;
    }

    private boolean isContactsEmpty(ArrayList<ContactHeader> contactHeaders)
    {
        boolean isEmpty = false;
        /**
         * Here we need to conduct a check on each and every ContactHeader
         * that is passed inside the ArrayList for whether or not that ContactHeader
         * contains any contact addresses
         */
        for (ContactHeader contactHeader : contactHeaders)
        {
            if (contactHeader.getAddress() == null)
            {
                isEmpty = true;
                break;
            }
        }
        return isEmpty;
    }

    private boolean isDomainNameSupported(String domainName)
    {
        boolean supported = false;
        for (String dName : supportedDomains)
        {
            if (domainName.equals(dName))
            {
                return true;
            }
        }
        /**
         * This is just a temporary fix, accepting all domains.
         */
        return true;
    }

    private String getCanonicalAddress(SipURI sipUri)
    {
        StringBuilder canonicalAddress = new StringBuilder();

        String addressScheme = sipUri.getScheme();
        canonicalAddress.append(addressScheme);
        canonicalAddress.append(":");

        String username = sipUri.getUser();
        canonicalAddress.append(username);
        canonicalAddress.append("@");

        String hostname = sipUri.getHost();
        canonicalAddress.append(hostname);
        canonicalAddress.append(":");

        int port = sipUri.getPort();
        if (port == -1)
        {
            if (sipUri.isSecure())
            {
                port = 5061;
            }
            else
            {
                port = 5060;
            }
        }
        canonicalAddress.append(port);

        return canonicalAddress.toString();
    }

    public void readJNDI()
    {
        try
        {
            InitialContext initialContext = new InitialContext();
            /**
             * Getting the activity context interface factory
             */
            ocSipAciFactory = (OCSipActivityContextInterfaceFactory) initialContext.lookup("java:comp/env/slee/resources/sipra/siprafactory");
            /**
             * Getting the SIP Resource adapter implementation class from the
             * JNDI
             */
            ocSleeSipProvider = (OCSleeSipProvider) initialContext.lookup("java:comp/env/slee/resources/sipra/sipresourceadapter");
        }
        catch (NamingException ex)
        {
            Logger.getLogger(SipRegistrarSbb.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendSipResponse(ArrayList<ContactHeader> contactHeaders, RequestEvent sipRequestEvent, int responseStatusCode)
    {
        try
        {
            MessageFactory messageFactory = ocSleeSipProvider.getMessageFactory();
            Response sipResponse = messageFactory.createResponse(responseStatusCode, sipRequestEvent.getRequest());
            ServerTransaction serverTransaction = sipRequestEvent.getServerTransaction();
            /**
             * Populate the SIP response with the list of Contact Headers
             */
            if (contactHeaders != null)
            {
                for (ContactHeader contactHeader : contactHeaders)
                {
                    sipResponse.setHeader(contactHeader);
                }
            }
            /**
             * Send the SIP response back to the original caller
             */
            tracer.info("DEBUG: Sending a " + responseStatusCode + " response back to the SIP client...");
            serverTransaction.sendResponse(sipResponse);
            tracer.info("DEBUG: The SIP response has been received...");
        }
        catch (Exception e)
        {
            tracer.severe(e.getMessage());
        }
    }

    private int getRegisterRequestType(Request registerRequest, ArrayList<ContactHeader> contactHeaders)
    {
        /**
         * There could be 3 types of REGISTER requests
         * 0 - Query Request --> if there are not contact addresses present
         * 1 - Add Request --> if contact addresses are present with a non-zero
         * expire value
         * 2 - Remove Request --> if contact addresses are present with a zero
         * expire value
         */
        int requestType = 0;

        if (contactHeaders.isEmpty())
        {
            requestType = 0;
        }
        /**
         * This means that the contact header field exist but there are no
         * values present in it.
         */
        else if (isContactsEmpty(contactHeaders))
        {
            requestType = 0;
        }
        else if (isAddRequest(registerRequest, contactHeaders))
        {
            requestType = 1;
        }
        else
        {
            requestType = 2;
        }

        return requestType;
    }

    private boolean isAddRequest(Request registerRequest, ArrayList<ContactHeader> contactHeaders)
    {
        /**
         * If the SIP request has an expire header field with a zero value, then
         * this request is considered as a remove request.
         *
         * otherwise, we need to inspect the individual contact headers for the
         * existence of the expire parameter, if it exists with a zero value then
         * this would render the entire request as being a removal request.
         */
        boolean isAddRequest = true; //This is the default assumption
        ExpiresHeader expiresHeader = registerRequest.getExpires();
        if (expiresHeader != null && expiresHeader.getExpires() == 0)
        {
            /**
             * This means that the request is a removal request
             */
            isAddRequest = false;
        }
        /**
         * If the execution reaches to this point, this means that we need to
         * inspect the individual contact headers for their expire value
         */
        else
        {
            for (ContactHeader contactHeader : contactHeaders)
            {
                if (contactHeader.getExpires() == 0)
                {
                    isAddRequest = false;
                    break;
                }
            }
        }
        return isAddRequest;
    }

    /**
     * This method should check if there is a profile created for the given
     * AOR, and create it if it is not already exist and then store the passed-
     * in values accordingly.
     * @param addressOfRecord
     * @param registrationRecord
     */
    private void storeBindingInformation(String addressOfRecord, ArrayList registrationRecord)
    {
        try
        {
            ProfileTable locationProfileTable = profileFacility.getProfileTable(LOCATION_DATABASE_PROFILE_TABLE_NAME);

            if (locationProfileTable != null) // If the ProfileTable exists
            {
                /**
                 * Query for the existence of a Profile with the same name as
                 * the AOR
                 */
                ProfileLocalObject profileLocalObject = locationProfileTable.find(addressOfRecord);
                LocationDbProfileLocalInterface profileLocalInterface = null;
                /**
                 * If there is no profile found, create it.
                 */
                if (profileLocalObject != null)
                {
                    tracer.info("DEBUG: Profile for the AOR: " + addressOfRecord + " has been located");
                    profileLocalInterface = (LocationDbProfileLocalInterface) profileLocalObject;
                }
                else
                {
                    tracer.info("DEBUG: Creating a new Profile for the AOR: " + addressOfRecord);
                    profileLocalInterface = (LocationDbProfileLocalInterface) locationProfileTable.create(addressOfRecord);
                }
                /**
                 * Store the information into the obtained/created profile after pulling them
                 * from the passed-in ArrayList according to its pre-set ordered format.
                 */
                ArrayList<ContactHeader> contactHeadersArrayList = (ArrayList<ContactHeader>) registrationRecord.get(0);

                Object[] contactHeadersObjectArray = contactHeadersArrayList.toArray();
                ContactHeader[] contactHeaders = new ContactHeader[contactHeadersArrayList.size()];

                for (int i = 0; i < contactHeaders.length; i++)
                {
                    if (debugEnabled)
                    {
                        tracer.info("DEBUG: The data type of the converted object is: " + contactHeadersObjectArray[i].getClass().getName());
                    }

                    contactHeaders[i] = ((ContactHeader) contactHeadersObjectArray[i]);

                    if (debugEnabled)
                    {
                        tracer.info("DEBUG: contactHeaders[i]: " + contactHeaders[i].toString());
                    }
                }

                profileLocalInterface.addBinding(contactHeaders);
                profileLocalInterface.setCallId((String) registrationRecord.get(1));
                profileLocalInterface.setCallSeq((Long) registrationRecord.get(2));
                profileLocalInterface.setThirdPartyRegFlag((Boolean) registrationRecord.get(3));
                /**
                 * The value for these parameters are not included in the
                 * registration record ArrayList, rather a default value of
                 * NULL is being set to them which doesn't need to be communicated
                 * in this method call. The real values for them will be updated
                 * when a location update client sends a location update HTTP
                 * message to the LocationServerSbb
                 */
                profileLocalInterface.setLatitude(null); // Default Value
                profileLocalInterface.setLongitude(null); // Default Value
                profileLocalInterface.setTimestamp(null); // Default Value
            }
            else
            {
                tracer.severe("ProfileTable with the name: LocationProfileTable does not exist");
                tracer.severe("Please contact the JSLEE container administrator to resolve the issue...");
                /**
                 * Here we need to create a new exception to be thrown in case
                 * that the designated profile table does not exist.
                 */
            }
        }
        catch (Exception ex)
        {
            tracer.info(ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void removeBindingInformation(String addressOfRecord, ContactHeader[] contactHeaders)
    {
        try
        {
            ProfileTable locationProfileTable = profileFacility.getProfileTable(LOCATION_DATABASE_PROFILE_TABLE_NAME);

            if (locationProfileTable != null)
            {
                /**
                 * Query for the existence of a Profile with the same name as
                 * the AOR
                 */
                ProfileLocalObject profileLocalObject = locationProfileTable.find(addressOfRecord);
                LocationDbProfileLocalInterface profileLocalInterface = null;
                /**
                 * If there is no profile found, the removal request need to be
                 * ignored.
                 */
                if (profileLocalObject != null)
                {
                    profileLocalInterface = (LocationDbProfileLocalInterface) profileLocalObject;
                    /**
                     * Here we need to remove the contact header mapping passed
                     * in from the corresponding profile. When invoking the
                     * corresponding method in that particular profile, we need
                     * to pass in the text representation of the contact header
                     * address part (as it is the key used to store the contact
                     * header text value inside the profile implementation) without
                     * any parameters whatsoever.
                     */
                    String[] contactHeadersStrings = new String[contactHeaders.length];
                    for (int i = 0; i < contactHeaders.length; i++)
                    {
                        /**
                         * Get the address part of the ContactHeader
                         */
                        String contactHeaderAddressPart = ((contactHeaders[i].getAddress().getURI().toString()).contains(";")) ? ((contactHeaders[i].getAddress().getURI().toString()).split(";")[0]) : (contactHeaders[i].getAddress().getURI().toString());
                        contactHeadersStrings[i] = contactHeaderAddressPart;
                    }
                    tracer.info("DEBUG: Start removing the binding as per the received REGISTER request.");
                    profileLocalInterface.removeBinding(contactHeadersStrings);
                    /**
                     * Here we need to check if there are any more contacts still
                     * bound to the given address-of-record, if there are no more
                     * contacts, then the whole profile must be removed.
                     *
                     * THIS STEP IS TO BE DEFERED FOR A WHILE UNTIL THE QUERY
                     * BINDING PART IS DEBUGGED.
                     */
                    String[] remainingContactBindings = profileLocalInterface.queryBinding();
                    if (remainingContactBindings.length == 0)
                    {
                        tracer.info("DEBUGGING: Removing the profile for the AOR: " + addressOfRecord);
                        locationProfileTable.remove(addressOfRecord);
                    }
                    else
                    {
                        tracer.info("Unable to remove the profile for AOR: " + addressOfRecord + " as there are still contacts bounded to it");
                        tracer.info("Contacts are: ");
                        for(String contactEntry : remainingContactBindings)
                        {
                            tracer.info("contactEntry: " + contactEntry);
                        }
                        
                    }
                }
            }
            else
            {
                tracer.severe("ProfileTable with the name: LocationProfileTable does not exist");
                tracer.severe("Please contact the JSLEE container administrator...");
            }
        }
        catch (Exception ex)
        {
            tracer.info(ex.getMessage());
        }
    }

    private ArrayList<ContactHeader> queryBindingInformation(String addressOfRecord)
    {
        /**
         * Construct the ArrayList to be returned back to the requestor
         * method.
         */
        ArrayList<ContactHeader> contactHeaders = new ArrayList<ContactHeader>();

        try
        {
            tracer.info("DEBUG: Looking for the profile table...");
            ProfileTable locationProfileTable = profileFacility.getProfileTable(LOCATION_DATABASE_PROFILE_TABLE_NAME);
            if (locationProfileTable != null)
            {
                tracer.info("DEBUG: Profile table found...");
                tracer.info("DEBUG: Looking for the profile with the same given AOR...");
                /**
                 * Query for the existence of a Profile with the same name as
                 * the AOR
                 */
                ProfileLocalObject profileLocalObject = locationProfileTable.find(addressOfRecord);
                LocationDbProfileLocalInterface profileLocalInterface = null;
                /**
                 * If there is no profile found, then an empty ArrayList must be
                 * returned.
                 */
                if (profileLocalObject != null)
                {
                    tracer.info("DEBUG: Profile found...");
                    profileLocalInterface = (LocationDbProfileLocalInterface) profileLocalObject;
                    /**
                     * Get all the contact header fields that are bound to the
                     * given address-of-record.
                     */
                    tracer.info("DEBUG: Querying the profile for the available contacts...");
                    String[] contactHeaderFields = profileLocalInterface.queryBinding();
                    /**
                     * Convert the received String array of ContactHeader
                     * text values into a proper ContactHeader objects to be
                     * injected into the ArrayList to be returned back to the
                     * requestor. The conversion must make use of the SIP
                     * header parser API that are shipped with the NIST RI
                     * of the JAIN SIP Specs, which must be loaded as a library
                     * that this SBB must reference.
                     */
                    if (contactHeaderFields != null)
                    {
                        tracer.info("DEBUG: ContactHeaderFields have been found...");
                        tracer.info("DEBUG: Converting the ContactHeaders from String to their native format...");
                        ContactHeader[] nativeContactHeaders = convertToNativeContactHeaders(contactHeaderFields);
                        /**
                         * populate the to-be-returned ArrayList with the retrieved
                         * contact header fields.
                         */
                        tracer.info("DEBUG: Populating the ContactHeaders array to be returned...");
                        for (ContactHeader contactHeader : nativeContactHeaders)
                        {
                            contactHeaders.add(contactHeader);
                        }
                    }
                    else
                    {
                        tracer.info("DEBUG: Unable to obtain the ContactHeaderFields for the given AOR from the profile...");
                    }
                    return contactHeaders;
                }
            }
            else
            {
                tracer.severe("ProfileTable with the name: LocationProfileTable does not exist");
                tracer.severe("Please contact the JSLEE container administrator...");
                /**
                 * Returning an empty ArrayList
                 */
                return contactHeaders;
            }
        }
        catch (Exception ex)
        {
            tracer.info("--------------------------Exception Occured-------------------------");
        }
        return contactHeaders;
    }

    private ContactHeader[] convertToNativeContactHeaders(String[] contactHeadersStrings)
    {
        tracer.info("DEBUG: Starting the contact header conversion and parsing process...");
        ContactHeader[] contactHeaders = new ContactHeader[contactHeadersStrings.length];
        /**
         * Convert the String contact headers array into a native ContactHeaders array to be
         * sent back to the requestor into a SIP 200 OK response.
         */
        try
        {
            for (int i = 0; i < contactHeadersStrings.length; i++)
            {
                ContactHeader contactHeader = ocSleeSipProvider.getHeaderFactory().createContactHeader();
                String contactHeaderAddress = contactHeadersStrings[i].split("<")[1].split(">")[0].trim();
                /**
                 * Create the ContactHeader and set the required parameters
                 */
                contactHeader.setAddress(ocSleeSipProvider.getAddressFactory().createAddress(contactHeaderAddress));
                tracer.info("��������: " + contactHeadersStrings[i]);
                contactHeaders[i] = contactHeader;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        tracer.info("DEBUG: returning a list of the found contact headers: " + contactHeaders.length);
        return contactHeaders;
    }

}