package com.opencloud.demo;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

public class CreateRuleButtonOnClickListener implements OnClickListener, Runnable
{
    private Activity activity = null;
    private ProgressDialog progressDialog = null;
    private JSONObject configUpdateMessage = null;

    public CreateRuleButtonOnClickListener(Activity activity)
    {
        this.activity = activity;
    }

    @Override
    public void onClick(View clickedView)
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "The onCreate button has been clicked...");
        try
        {
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Validating the user input and creating a JSON Message POJO from the gathered user input...");
            configUpdateMessage = constructConfigUpdateMessage(clickedView);            
            /**
             * If the constructed JSON message is null, this means that the user input validation had failed for
             * some reason, hence this method should return without performing any further action.
             */
            if(configUpdateMessage == null)
            {
                Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "config_update_message is null, error during user input validation");                
                return;
            }
        }
        catch (JSONException ex)
        {
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Exception during JSON message construction: " + ex.getMessage());
        }
        showProgressBar();
        /**
         * Sending an HTTP PUT message need to be done from within a thread so
         * that the GUI elements navigation stays smooth and won't get affected
         * by the supposed to be a relatively long-running thread.
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Sending cmr_create_message HTTP-PUT to the LocationServerSBB...");
        startConfigUpdateThread();
        dismissProgressBarAndActivity();
    }

    private void startConfigUpdateThread()
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Thread started to send cmr_create_message to the LocationServerSBB...");
        Thread configUpdateThread = new Thread(this);
        configUpdateThread.start();
    }

    private void sendConfigurationUpdateMessage()
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Sending configuration update message over HTTP");
        /**
         * Create the HTTP Client
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Instantiating an HTTP client object, apache http client...");
        HttpClient httpClient = new DefaultHttpClient();
        try
        {
            /**
             * Creating an empty ByteArrayOutputStream
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "creating byteArrayOutputStream");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "byteArrayOutputStream has been created...");
            /**
             * Creating a new ObjectOutputStream
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "creating a new ObjectOutputStream...");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "ObjectOutputStream has been created...");
            /**
             * Writing the Message object into the the objectOutputStream
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Writing the JSON message into the created ObjectOutputStream");
            objectOutputStream.writeObject(configUpdateMessage.toString());            
            objectOutputStream.flush();
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "flushing the ObjectOutputStream...");
            /**
             * Convert the Message object into a ByteArray
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Converting the message into a ByteArray...");
            byte[] messageByteArray = byteArrayOutputStream.toByteArray();            
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Converting completed");
            /**
             * Construct a new HttpEntity and set its contents to the byteArray
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Constructing the HttpEntity...");
            ByteArrayEntity byteArrayEntity = new ByteArrayEntity(messageByteArray);
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "HttpEntity construction completed");
            /**
             * Create a new HttpPut that contains the created ByteArray
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Creating an HttpPut message...");
            HttpPut httpPut = new HttpPut(getLocationServerAddress());
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Setting the HttpEntity as the payload of the created HttpPut Message");
            httpPut.setEntity(byteArrayEntity);
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "HttpPut message is ready to be sent");
            /**
             * Turn off the Expect-Continue handshake header inclusion in the
             * request which cause the OC-HTTP-RA to respond back with a 417
             * Expectation Failed response.
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Turning-Off the expect-continue header automatic inclusion");
            httpClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
            /**
             * Send the created HttpPut
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Sending the HttpPut message to the LocationServerSBB...");
            httpClient.execute(httpPut);
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "HTTP PUT config update message has been sent to " + getLocationServerAddress());
            /**
             * Terminate the established HTTP Session, as we are not expecting
             * any response back from the LocationServerSbb, this is a one way
             * messaging pattern only.
             */
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "Terminating the established HTTP connection...");
            httpClient.getConnectionManager().shutdown();
            Log.d(this.getClass().getName(), MainActivity.LOG_PREFIX + "HTTP connection has been terminated...");
        }
        catch (Exception ex)
        {
            Log.e(this.getClass().getName(), MainActivity.LOG_PREFIX + ex.getMessage());
        }
    }

    private JSONObject constructConfigUpdateMessage(View view) throws JSONException
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Constructing the configuration update message (cmr_create_message)");
        /**
         * Create an instance of the Message object (as a JSONObject) to be
         * returned back to the calling subroutine.
         */
        JSONObject message = new JSONObject();
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Empty JSON message object has been created");
        /**
         * Set the message type to be of a cmr_create_message
         */
        message.put(MessageConstants.MESSAGE_TYPE, MessageConstants.MSG_TYPE_CMR_CREATE);
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Message type has been set to: " + MessageConstants.MSG_TYPE_CMR_CREATE);
        /**
         * Retrieve the address-of-record from the SharedPreferences and set it
         * accordingly in the constructed message. The address-of-record is
         * obtained from the settings page, a default value is also preset for
         * it.
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Retrieving the address-of-record");        
        message.put(MessageConstants.ADDRESS_OF_RECORD, getAddressOfRecord());
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "AOR: " + getAddressOfRecord());        
        /**
         * Collect the user input that is to be obtained from the edit call
         * management rules activity and convert them into a data format
         * appropriate to the corresponding fields in the Message JSON object,
         * then set these fields accordingly using the Message JSON object put
         * methods.
         */

        /**
         * Get the ruleName and set the corresponding field in the
         * JSON message object.
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Getting the ruleName value entered by the user...");
        String ruleName = ((EditText) activity.findViewById(R.id.editCallMgmtRuleNameTextEdit)).getText().toString();
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "ruleName: " + ruleName);
        /**
         * Validating and setting the ruleName
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "ruleName length: " + ruleName.length());
        if(ruleName == null || ruleName.length() == 0 || ruleName == "" || ruleName.equals(""))
        {
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "RuleName is null");
            FunkyToast.makeText(view.getContext(), "Please enter a name for the rule you are creating...", Toast.LENGTH_SHORT, "icon").show();
            return null;
        }
        else
        {
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Setting the ruleName");
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "RuleName: " + ruleName);
            message.put(MessageConstants.RULE_NAME, ruleName);
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "The ruleName property has been set in the JSON Message Object...");
        }
        /**
         * Get the ruleStartTimeStamp (which is a combination of the ruleStartDate and ruleFromTime) and then
         * set the corresponding fields in the JSON message object.
         */
        Date ruleStartTimeStamp = new Date();
        // Get the data from the DatePicker
        ruleStartTimeStamp.setYear(((DatePicker) activity.findViewById(R.id.editCallMgmtRuleFromDatePicker)).getYear() - 1900);
        ruleStartTimeStamp.setMonth(((DatePicker) activity.findViewById(R.id.editCallMgmtRuleFromDatePicker)).getMonth());
        ruleStartTimeStamp.setDate(((DatePicker) activity.findViewById(R.id.editCallMgmtRuleFromDatePicker)).getDayOfMonth());
        // Get the data from the TimePicker
        ruleStartTimeStamp.setHours(((TimePicker) activity.findViewById(R.id.editCallMgmtRuleFromTimePicker)).getCurrentHour());
        ruleStartTimeStamp.setMinutes(((TimePicker) activity.findViewById(R.id.editCallMgmtRuleFromTimePicker)).getCurrentMinute());
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "ruleStartTimeStamp: " + ruleStartTimeStamp);
        /**
         * Get the ruleEndTimeStamp (which is combination of the ruleEndDate and
         * ruleToTime) and set then set the appropriate attribute in the Message
         * object.
         */
        Date ruleEndTimeStamp = new Date();
        // Get the data from the DatePicker
        ruleEndTimeStamp.setYear(((DatePicker) activity.findViewById(R.id.editCallMgmtRuleToDatePicker)).getYear() - 1900);
        ruleEndTimeStamp.setMonth(((DatePicker) activity.findViewById(R.id.editCallMgmtRuleToDatePicker)).getMonth());
        ruleEndTimeStamp.setDate(((DatePicker) activity.findViewById(R.id.editCallMgmtRuleToDatePicker)).getDayOfMonth());
        // Get the data form the TimePicker
        ruleEndTimeStamp.setHours(((TimePicker) activity.findViewById(R.id.editCallMgmtRuleToTimePicker)).getCurrentHour());
        ruleEndTimeStamp.setMinutes(((TimePicker) activity.findViewById(R.id.editCallMgmtRuleToTimePicker)).getCurrentMinute());
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "ruleEndTimeStamp: " + ruleEndTimeStamp);
//        /**
//         * Here we need to compare that the start ruleStartTimeStamp is not happening after the ruleEndTimeStamp
//         * and they are both not null.
//         */        
//        if (ruleStartTimeStamp != null && ruleEndTimeStamp != null && ruleEndTimeStamp.after(ruleStartTimeStamp))
//        {
//            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Validation Successful: ruleEndTimeStamp");
//            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Validation Successful: ruleStartTimeStamp");
//            message.put(MessageConstants.RULE_START_TIMESTAMP, ruleStartTimeStamp);
//            message.put(MessageConstants.RULE_END_TIMESTAMP, ruleEndTimeStamp);
//        }
//        else
//        {
//            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Validation Unsuccessful: ruleEndTimeStamp");
//            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Validation Unsuccessful: ruleStartTimeStamp");
//            FunkyToast.makeText(view.getContext(), "Error setting rule time stamp information, please make sure that the end time starts after the start time", Toast.LENGTH_SHORT, "icon").show();
//            return null;
//        }       
        /**
         * Get the longitude, latitude an the proximity settings entered by the user
         * and set them appropriately in the JSON message object. These fields are optional, so there is
         * no need to validate the input at this time, it is the responsibility of the server side component to
         * take the appropriate action basing on the presence of the value of these fields.
         */
        String longitude = ((EditText) activity.findViewById(R.id.editCallMgmtRuleLongitudeTextEdit)).getText().toString();
        String latitude = ((EditText) activity.findViewById(R.id.editCallMgmtRuleLatitudeTextEdit)).getText().toString();
        int proximity = Integer.parseInt(((EditText) activity.findViewById(R.id.editCallMgmtRuleProximityTextEdit)).getText().toString());
        message.put(MessageConstants.LATITUDE, latitude);
        message.put(MessageConstants.LONGITUDE, longitude);
        message.put(MessageConstants.PROXIMITY, proximity);
        /**
         * Get the incoming numbers, these numbers are entered in a comma
         * separated format so they need to be converted into a proper String
         * array before setting them into the JSON message object.
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Obtaining the incoming numbers array");
        String[] incomingNumbers = ((EditText) activity.findViewById(R.id.incomingCallerNumberTextEdit)).getText().toString().split(",");
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "incomingNumbers length: " + ((EditText) activity.findViewById(R.id.incomingCallerNumberTextEdit)).getText().length());
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "incomingNumbers array length: " + incomingNumbers.length);
        /**
         * Create a JSONArray for the list of the incoming numbers and set them
         * into the message JSON object accordingly. The list of the incoming numbers is optional
         * so we will leave it to the server side to validate and take action if the incoming numbers
         * list is empty, which signifies that the rule should basically apply to all incoming numbers.
         */
        JSONArray jsonArray = new JSONArray();
        for (String incomingNumber : incomingNumbers)
        {
            jsonArray.put(incomingNumber);
        }
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "incomingNumbers JSON array length: " + jsonArray.length());
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Setting the incoming numbers array into the JSON message object.");
        message.put(MessageConstants.INCOMING_NUMBERS, jsonArray);
        /**
         * Get the call handling method and set the appropriate fields in the constructed
         * JSON array. The value of this property can't be null as there is a certain value
         * (the default value) that will always be selected if the user didn't select any alternative
         * call handling method.
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Getting the call handling method...");
        int callHandlingMethodId = ((Spinner) activity.findViewById(R.id.callHandlingMethodSpinner)).getSelectedItemPosition();
        String callHandlingMethod = null;

        if (callHandlingMethodId == 0)
        {
            callHandlingMethod = "ACCEPT";
        }
        else if (callHandlingMethodId == 1)
        {
            callHandlingMethod = "REJECT";
        }
        else if (callHandlingMethodId == 2)
        {
            callHandlingMethod = "VOICE_MAIL_REDIRECTION";
        }
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Setting the call handling method...");
        message.put(MessageConstants.CALL_HANDLING_METHOD, callHandlingMethod);
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "callHandlingMethod selected: " + callHandlingMethod);
        /**
         * Get the list of devices, these devices are entered in a comma
         * separated format so they need to be converted into a proper String
         * array before setting them into the message object. What applies to the list of
         * the incoming numbers applies to the device list as well.
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Getting the device's list...");
        String[] devicesList = ((EditText) activity.findViewById(R.id.deviceListTextEdit)).getText().toString().split(",");
        /**
         * Convert the list of the collected devices into a JSON array, then add
         * it into the JSON message object.
         */
        JSONArray deviceListJsonArray = new JSONArray();
        for (String device : devicesList)
        {
            deviceListJsonArray.put(device);
        }
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Setting the device's list...");
        message.put(MessageConstants.DEVICES_LIST, deviceListJsonArray);
        /**
         * Getting the ringingType ID from the user's input and set it accordingly
         * into the constructed JSON message object.
         */
        int callRingingTypeId = ((Spinner) activity.findViewById(R.id.callRingingTypeSpinner)).getSelectedItemPosition();
        String callRingingType = null;

        if (callRingingTypeId == 0)
        {
            callRingingType = "PARALLEL";
        }
        else if (callRingingTypeId == 1)
        {
            callRingingType = "SEQUENTIAL";
        }
        else if (callRingingTypeId == 2)
        {
            callRingingType = "HYBRID";
        }
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Call Ringing Type: " + callRingingType);
        message.put(MessageConstants.CALL_RINGING_TYPE, callRingingType);
        /**
         * Now return back the constructed message back to the method invoker.
         */
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "User input validation completed successfully, returning the constructed JSON message object...");
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "RULE_END_TIMESTAMP class type" + message.get(MessageConstants.RULE_END_TIMESTAMP).getClass().getName());
        return message;
    }

    private String getLocationServerAddress()
    {
        String ipAddress = activity.getSharedPreferences(SettingsSubActivity.SETTINGS_PAGE_SHARED_PREF_FILE, 0).getString(SettingsSubActivity.LOCATION_SERVER_SBB_IP_ADDRESS, SettingsSubActivity.DEFAULT_LOCATION_SERVER_IP_ADDRESS);
        String portNumber = activity.getSharedPreferences(SettingsSubActivity.SETTINGS_PAGE_SHARED_PREF_FILE, 0).getString(SettingsSubActivity.LOCATION_SERVER_SBB_PORT_NUMBER, SettingsSubActivity.DEFAULT_LOCATION_SERVER_PORT_NUMBER);
        return "http://" + ipAddress + ":" + portNumber;
    }

    private void dismissProgressBarAndActivity()
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "dismissProgressBar() has been called...");
        ProgressDialogDismisser progressDialogDismisser = new ProgressDialogDismisser(progressDialog, activity);
        Thread thread = new Thread(progressDialogDismisser);
        thread.start();
    }

    private void showProgressBar()
    {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("Please wait: The call management rule is being created...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private String getAddressOfRecord()
    {
        return activity.getSharedPreferences(SettingsSubActivity.SETTINGS_PAGE_SHARED_PREF_FILE, 0).getString(SettingsSubActivity.ADDRESS_OF_RECORD, SettingsSubActivity.DEFAULT_ADDRESS_OF_RECORD);
    }

    @Override
    public void run()
    {
        sendConfigurationUpdateMessage();
    }
}
