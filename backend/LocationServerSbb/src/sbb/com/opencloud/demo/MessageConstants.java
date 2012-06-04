package com.opencloud.demo;

public interface MessageConstants
{
    /**
     * Here are the various JSON object message keys.
     */
    public final static String MESSAGE_TYPE = "message_type";
    public final static String ADDRESS_OF_RECORD = "address_of_record";
    public final static String RULE_NAME = "rule_name";
    public final static String RULE_START_TIMESTAMP = "rule_start_timestamp";
    public final static String RULE_END_TIMESTAMP = "rule_end_timestamp";
    public final static String LONGITUDE = "longitude";
    public final static String LATITUDE = "latitude";
    public final static String PROXIMITY = "proximity";
    public final static String INCOMING_NUMBERS = "incoming_numbers";
    public final static String CALL_HANDLING_METHOD = "call_handling_method";
    public final static String DEVICES_LIST = "devices_list";
    public final static String CALL_RINGING_TYPE = "call_ringing_type";
    public final static String LOCATION_UPDATE_TIMESTAMP = "location_update_timestamp";
    /**
     * Set the various message types in here
     */
    public static final String MSG_TYPE_LOCATION_UPDATE = "location_update_message";
    public static final String MSG_TYPE_LOCATION_QUERY = "location_query_message";
    public static final String MSG_TYPE_CONFIG_UPDATE = "config_update_message";
    public static final String MSG_TYPE_CMR_CREATE = "cmr_create_message";
}