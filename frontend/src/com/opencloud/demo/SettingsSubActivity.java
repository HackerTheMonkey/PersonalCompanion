package com.opencloud.demo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SettingsSubActivity extends Activity
{
    protected static SettingsSubActivity settingsSubActivity = null;
    /**
     * The following constants represent the names of the SharedPreferences that
     * are used to store the text values of the settings page input fields.
     */
    protected static final String SETTINGS_PAGE_SHARED_PREF_FILE = "settings_page_shared_pref_file";
    protected static final String LOCATION_SERVER_SBB_IP_ADDRESS = "location_server_sbb_ip_address";
    protected static final String LOCATION_SERVER_SBB_PORT_NUMBER = "location_server_sbb_port_number";
    protected static final String ADDRESS_OF_RECORD = "address_of_record";
    /**
     * The following private constants expressing the default values to be used
     * for the settings page input fields.
     */
    protected static final String DEFAULT_LOCATION_SERVER_IP_ADDRESS = "144.82.82.115";
    protected static final String DEFAULT_LOCATION_SERVER_PORT_NUMBER = "8000";
    protected static final String DEFAULT_ADDRESS_OF_RECORD = "sip:1243@144.82.82.115:5060";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        /**
         * Populate the settings page view(s) with the stored/default values
         * extracted from the SharedPreferences
         */
        populateValues();
        /**
         * Register an onClickListener for the SaveSettings button.
         */
        registerSaveButtonOnClickListener(this);
        /**
         * Register an onClickListener for the Cancel button, the cancel button
         * will simply call the finish() method on this Activity object when its
         * clicked.
         */
        registerCancelButtonOnClickListener(this);
    }

    private void registerSaveButtonOnClickListener(Activity activity)
    {
        /**
         * Obtain an instance handler on the SaveSettings button.
         */
        Button saveSettingsButton = (Button) findViewById(R.id.saveSettingsButton);
        /**
         * Register the listener
         */
        saveSettingsButton.setOnClickListener(new SaveSettingsOnClickListener(activity));
    }
    
    private void registerCancelButtonOnClickListener(Activity activity)
    {
        /**
         * Obtain an instance handler on the Cancel button.
         */
        Button saveSettingsButton = (Button) findViewById(R.id.cancelSettingsButton);
        /**
         * Register the listener
         */
        saveSettingsButton.setOnClickListener(new CancelButtonOnClickListener(activity));
    }

    private void populateValues()
    {
        /**
         * Get the SharedPreferences file handle
         */
        SharedPreferences settings = getSharedPreferences(SETTINGS_PAGE_SHARED_PREF_FILE, 0);
        /**
         * Obtain object instances on all the settings' page elements and set
         * their text property to the value retrieved from the corresponding
         * entry in the SharedPreferences
         */
        ((EditText) findViewById(R.id.enterServerIPAddressEditText)).setText(settings.getString(LOCATION_SERVER_SBB_IP_ADDRESS, DEFAULT_LOCATION_SERVER_IP_ADDRESS));
        ((EditText) findViewById(R.id.enterPortNumberEditText)).setText(settings.getString(LOCATION_SERVER_SBB_PORT_NUMBER, DEFAULT_LOCATION_SERVER_PORT_NUMBER));
        ((EditText) findViewById(R.id.settingsAddressOfRecordTextEdit)).setText(settings.getString(ADDRESS_OF_RECORD, DEFAULT_ADDRESS_OF_RECORD));
    }
    
    private static class CancelButtonOnClickListener implements OnClickListener
    {
        private Activity activity = null;
        
        public CancelButtonOnClickListener(Activity activity)
        {
            this.activity = activity;
        }

        @Override
        public void onClick(View view)
        {         
            activity.finish();
        }
    }

}
