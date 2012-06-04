package com.opencloud.demo.onenumber.location;

import com.opencloud.demo.MessageConstants;
import com.opencloud.slee.resources.http.HttpRequest;
import com.opencloud.slee.services.location.CallManagementRule;
import com.opencloud.slee.services.location.LocationDbProfileLocalInterface;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Date;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.profile.ProfileLocalObject;
import javax.slee.profile.ProfileTable;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;
import json.org.JSONArray;
import json.org.JSONException;
import json.org.JSONObject;

/**
 *
 * @author Hasanein.Khafaji
 */
public abstract class LocationServerSbb implements Sbb
{

    /**
     * Private variables definition
     */
    private SbbContext sbbContext = null;
    private AlarmFacility alarmFacility = null;
    private ProfileFacility profileFacility = null;
    private TimerFacility timerFacility = null;
    private ActivityContextNamingFacility acNamingFacility = null;
    private NullActivityContextInterfaceFactory nullActivityContextInterfaceFactory = null;
    private NullActivityFactory nullActivityFactory = null;
    private ServiceActivityFactory serviceActivityFactory = null;
    private ServiceActivityContextInterfaceFactory serviceActivityContextInterfaceFactory = null;
    private Tracer tracer = null;
    private boolean debugEnabled = false;
    private final String LOCATION_DATABASE_PROFILE_TABLE_NAME = "LocationProfileTable";
    /**
     * Set the SbbContext and get the tracer and the facilities
     * provided by the JSLEE Container
     * @param sc
     */
    public void setSbbContext(SbbContext sbbContext)
    {
        this.sbbContext = sbbContext;
        /**
         * get the tracer
         */
        tracer = sbbContext.getTracer(sbbContext.getSbb().getName());
        tracer.info("The SbbContext has been set...");
        /**
         * Query the JNDI for the JSLEE-provided facilities
         */
        try
        {
            tracer.info("Looking up the JNDI for the JSLEE-provided facilities...");
            /**
             * Set a JNDI initial context to start the lookup
             * operation from
             */
            InitialContext initialContext = new InitialContext();
            /**
             * Starting from the initial context, query for the
             * desired JSLEE-injected facilities
             */
            alarmFacility = (AlarmFacility) initialContext.lookup(alarmFacility.JNDI_NAME);
            profileFacility = (ProfileFacility) initialContext.lookup(profileFacility.JNDI_NAME);
            timerFacility = (TimerFacility) initialContext.lookup(timerFacility.JNDI_NAME);
            acNamingFacility = (ActivityContextNamingFacility) initialContext.lookup(acNamingFacility.JNDI_NAME);
            nullActivityContextInterfaceFactory = (NullActivityContextInterfaceFactory) initialContext.lookup(nullActivityContextInterfaceFactory.JNDI_NAME);
            nullActivityFactory = (NullActivityFactory) initialContext.lookup(nullActivityFactory.JNDI_NAME);
            serviceActivityFactory = (ServiceActivityFactory) initialContext.lookup(serviceActivityFactory.JNDI_NAME);
            serviceActivityContextInterfaceFactory = (ServiceActivityContextInterfaceFactory) initialContext.lookup(serviceActivityContextInterfaceFactory.JNDI_NAME);

            tracer.info("The facilities have been successfully obtained from the JNDI...");
        }
        catch (NamingException ex)
        {
            tracer.severe(ex.getMessage());
        }
    }

    public void sbbActivate()
    {
        //tracer.info("SbbActivate method has been called...");
    }

    public void sbbCreate() throws CreateException
    {
        if (debugEnabled)
        {
            tracer.info("SbbCreate method has been called...");
        }
    }

    public void sbbExceptionThrown(Exception excptn, Object o, ActivityContextInterface aci)
    {
        if (debugEnabled)
        {
            tracer.info("SbbExceptionThrown method has been called...");
        }
    }

    public void sbbLoad()
    {
        if (debugEnabled)
        {
            tracer.info("SbbLoad method has been called...");
        }
    }

    public void sbbPassivate()
    {
        if (debugEnabled)
        {
            tracer.info("SbbPassivate method has been called...");
        }
    }

    public void sbbPostCreate() throws CreateException
    {
        if (debugEnabled)
        {
            tracer.info("SbbPostCreate method has been called...");
        }
    }

    public void sbbRemove()
    {
        if (debugEnabled)
        {
            tracer.info("SbbRemove method has been called...");
        }
    }

    public void sbbRolledBack(RolledBackContext rbc)
    {
        if (debugEnabled)
        {
            tracer.info("SbbRolledBack method has been called...");
        }
    }

    public void sbbStore()
    {
        if (debugEnabled)
        {
            tracer.info("SbbStore method has been called...");
        }
    }

    public void unsetSbbContext()
    {
        if (debugEnabled)
        {
            tracer.info("Clearing the SbbContext...");
        }
        sbbContext = null;
        if (debugEnabled)
        {
            tracer.info("The SbbContext has been cleared out...");
        }

        if (debugEnabled)
        {
            tracer.info("Clearing the JSLEE-provided facilities...");
        }
        clearFacilities();
        if (debugEnabled)
        {
            tracer.info("The JSLEE-provided facilities have been cleared out.");
        }
    }

    private void clearFacilities()
    {
        alarmFacility = null;
        profileFacility = null;
        timerFacility = null;
        acNamingFacility = null;
        nullActivityContextInterfaceFactory = null;
        nullActivityFactory = null;
        serviceActivityFactory = null;
        serviceActivityContextInterfaceFactory = null;
    }

    public void onPostRequest(HttpRequest httpRequest, ActivityContextInterface aci)
    {
        try
        {
            tracer.info("HTTP POST event has been received....");
            /**
             * Construct a ByteArrayInputStream using the contents of the received
             * HttpPost request (the request entity).
             */
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(httpRequest.getContent());
            /**
             * Create a new ObjectInputStream using the previously created ByteArrayInputStream
             */
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            /**
             * Read the sent message via the objectInputStream
             */
            JSONObject message = (JSONObject) objectInputStream.readObject();
            /**
             * Here we need to look to directly access the LocationDatabase
             * profile table and search for a profile with the same name as the
             * AOR contained in the received message. In the case where there is
             * no profile with such a name, then we simply need to quit the process
             * of location update/query with the appropriate response to the
             * requestor, if needed.
             */
            tracer.info("Looking up profile for the address of record received in the HTTP location update message...");
            LocationDbProfileLocalInterface profile = lookupProfile(message.getString(MessageConstants.ADDRESS_OF_RECORD));
            if (profile != null)
            {
                /**
                 * Update/Query the location information into the location database profile
                 * depending on the message type being processed.
                 */
                tracer.info("Profile has been found, updating location information...");
                if(message.getString(MessageConstants.MESSAGE_TYPE).equals(MessageConstants.MSG_TYPE_LOCATION_UPDATE))
                {
                    storeLocationInformation(profile, message.getString(MessageConstants.LONGITUDE), message.getString(MessageConstants.LATITUDE), message.getString(MessageConstants.LOCATION_UPDATE_TIMESTAMP));
                }
                else if(message.getString(MessageConstants.MESSAGE_TYPE).equals(MessageConstants.MSG_TYPE_LOCATION_QUERY))
                {
                    /**
                     * Query the profile for the required location information
                     * and respond back to the requesting client.
                     */
                    tracer.info("Location query request has been received...");
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void onGetRequest(HttpRequest httpRequest, ActivityContextInterface aci)
    {
        tracer.info("HTTP GET event has been received....");
    }

    public void onDeleteRequest(HttpRequest httpRequest, ActivityContextInterface aci)
    {
        tracer.info("HTTP DELETE event has been received....");
    }

    public void onPutRequest(HttpRequest httpRequest, ActivityContextInterface aci)
    {
        try
        {
            tracer.info("HTTP PUT event has been received....");
            /**
             * Construct a ByteArrayInputStream using the contents of the received
             * HttpPut request (the request entity).
             */
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(httpRequest.getContent());
            /**
             * Create a new ObjectInputStream using the previously created ByteArrayInputStream
             */
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            /**
             * Read the sent message via the objectInputStream, here we are expecting to receive
             * a String message that is encoded as a JSONObject, from which we will construct a JSON
             * Object.
             */       
            String strMessage = (String) objectInputStream.readObject();
            JSONObject jsonMessage = new JSONObject(strMessage);
            if(jsonMessage != null)
            {
                tracer.info("JSONMessage has been obtained");
                tracer.info("JSON Message Type: " + jsonMessage.getString(MessageConstants.MESSAGE_TYPE));
                tracer.info("CMR RuleName: " + jsonMessage.getString(MessageConstants.RULE_NAME));
            }
            tracer.info("Looking up profile for the address of record received in the HTTP configuration update message...");
            LocationDbProfileLocalInterface profile = lookupProfile(jsonMessage.getString(MessageConstants.ADDRESS_OF_RECORD));            
            /**
             * Here we need to check if there exist a profile with the same name as the
             * AOR, if yes then we need to create the CMR, otherwise we should
             * skip the CMT creation operation as the associated profile does
             * not exist.
             */
            if(profile != null)
            {
                tracer.info("the obtained profile is not null");
                if(jsonMessage.getString(MessageConstants.MESSAGE_TYPE).equals(MessageConstants.MSG_TYPE_CMR_CREATE))
                {
                    tracer.info("Creating a new Call Management Rule (CMR)");
                    /**
                     * Create or update a CMR. Here we shall simply pass the created CMR
                     * to the addCMR() and it will take care of checking whether the
                     * CMR already exist or not, i.e. an update or create operation.
                     */
                    tracer.info("Processing " + MessageConstants.MSG_TYPE_CMR_CREATE + "...");
                    profile.addCMR(obtainCmrFromMessage(jsonMessage));
                }
                else
                {
                    /**
                     * Here we need to have the implementation for the rest
                     * of the message types, i.e. the queryAll and query single
                     * CMR.
                     */
                    tracer.info("Something else has been received, i.e message type is not the same, somehow....");
                }
            }
            else
            {
                tracer.info("There is no profile exist for the AOR: " + jsonMessage.getString(MessageConstants.ADDRESS_OF_RECORD));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void storeLocationInformation(LocationDbProfileLocalInterface profile,String longitude, String latitude, String updateTimeStamp)
    {
        tracer.info("Setting the location information for the device with AOR: " + profile.getProfileName() + ", Location Information: Longitude=" + longitude + ", Latitude=" + latitude + ", UpdateTimeStamp=" + updateTimeStamp);
        profile.setLatitude(latitude);
        profile.setLongitude(longitude);
        profile.setTimestamp(updateTimeStamp);
    }

    /**
     * This method will search for the given profile table for the existence
     * of a profile that match the passed in address of record (AOR), if it does
     * not find that specific profile, then it would simply return null.
     * @param addressOfRecord
     * @return
     */
    private LocationDbProfileLocalInterface lookupProfile(String addressOfRecord)
    {
        tracer.info("Looking up profile for the address of record: " + addressOfRecord);
        LocationDbProfileLocalInterface aorProfile = null;
        try
        {
            ProfileTable locationProfileTable = profileFacility.getProfileTable(LOCATION_DATABASE_PROFILE_TABLE_NAME);
            if (locationProfileTable != null) // If the ProfileTable exists
            {
                ProfileLocalObject profileLocalObject = locationProfileTable.find(addressOfRecord);
                if(profileLocalObject != null)
                {
                    tracer.info("Profile has been found...");
                    aorProfile = (LocationDbProfileLocalInterface) profileLocalObject;
                }
            }
            else
            {
                tracer.severe("ProfileTable with the name: LocationProfileTable does not exist");
                tracer.severe("Please contact the JSLEE container administrator to resolve the issue...");
                /**
                 * We need to design an exception to be thrown at this point.
                 */
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return aorProfile;
    }

    private CallManagementRule obtainCmrFromMessage(JSONObject message)
    {
        CallManagementRule callManagementRule = new CallManagementRule();
        tracer.info("Creating a new empty CallManagementRule");
        try
        {
            tracer.info("Populating the CallManagementRule with the contents of the received JSON message");
            tracer.info("callHandlingMethod: " + message.getString(MessageConstants.CALL_HANDLING_METHOD));
            callManagementRule.setCallHandlingMethod(message.getString(MessageConstants.CALL_HANDLING_METHOD));
            /**
             * Get the JSON Array device list and convert it into a normal String array
             */
            JSONArray devicesListJsonArray = message.getJSONArray(MessageConstants.DEVICES_LIST);            
            String devicesList[] = new String[devicesListJsonArray.length()];
            for(int i =0 ; i < devicesList.length ; i++)
            {
                devicesList[i] = devicesListJsonArray.getString(i);
            }
            callManagementRule.setDevicesList(devicesList);
            /**
             * Get the JSONArray of the incomingNumbers, convert them into a normal String array
             * and then set that into the callManagementRule.
             */
            JSONArray incomingNumbersJsonArray = message.getJSONArray(MessageConstants.INCOMING_NUMBERS);
            String incomingNumbers[] = new String[incomingNumbersJsonArray.length()];
            for(int i = 0 ; i < incomingNumbers.length ; i++)
            {
                incomingNumbers[i] = incomingNumbersJsonArray.getString(i);
            }                        
            callManagementRule.setIncomingNumbers(incomingNumbers);
            /**
             * Set the latitude
             */
            callManagementRule.setLatitude(message.getString(MessageConstants.LATITUDE));            
            tracer.info("latitude: " + message.getString(MessageConstants.LATITUDE));
            /**
             * Set the longitude
             */
            callManagementRule.setLongitude(message.getString(MessageConstants.LONGITUDE));
            tracer.info("longitude: " + message.getString(MessageConstants.LONGITUDE));
            /**
             * Set the proximity
             */
            callManagementRule.setProximity(message.getInt(MessageConstants.PROXIMITY));
            tracer.info("proximity: " + message.getInt(MessageConstants.PROXIMITY));
            /**
             * Set the ringing type
             */
//            callManagementRule.setRingingType(message.getString(MessageConstants.CALL_RINGING_TYPE));
//            tracer.info("callRingingType: " + message.getString(MessageConstants.CALL_RINGING_TYPE));
            /**
             * Set the rule end timestamp.
             */
            callManagementRule.setRuleEndTimeStamp((Date)message.get(MessageConstants.RULE_END_TIMESTAMP));
            tracer.info("ruleEndTimeStamp: " + message.getString(MessageConstants.RULE_END_TIMESTAMP));
            /**
             * Set the rule name
             */
            callManagementRule.setRuleName(message.getString(MessageConstants.RULE_NAME));
            tracer.info("RuleName: " + message.getString(MessageConstants.RULE_NAME));
            /**
             * Set the rule start timestamp
             */
            callManagementRule.setRuleStartTimeStamp((Date)message.get(MessageConstants.RULE_START_TIMESTAMP));
        }
        catch(JSONException ex)
        {
            ex.printStackTrace();
        }
        return callManagementRule;
    }

}