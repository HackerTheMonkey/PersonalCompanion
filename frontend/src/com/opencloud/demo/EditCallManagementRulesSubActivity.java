package com.opencloud.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

public class EditCallManagementRulesSubActivity extends Activity
{

    /**
     * This static variable will enable other parts of the application from
     * directly obtaining an access to the instance of this activity to interact
     * with it synchronously.
     */
    protected static EditCallManagementRulesSubActivity editCallManagementRulesSubActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        /**
         * Set the content view to the desired layout
         */
        setContentView(R.layout.edit_call_mgmt_rules_layout);
        /**
         * Adjust some relative positions of some views on a RelativeLayout
         * prior to setting the content view.
         */
        adjustRelativePositions();
        /**
         * Here we need to register an event listener that will be listening for
         * click/tap events over the MAP button.
         */
        registerMapShowButtonOnClickListener();
        /**
         * Set a self reference to this activity
         */
        this.editCallManagementRulesSubActivity = this;
        /**
         * Register a listener to be called back when the Cancel button is being
         * clicked/tapped on.
         */
        registerCancelButtonOnClickListener();
        /**
         * Register a listener to be called back when the Create button is being
         * clicked/tapped on.
         */
        registerCreateButtonClickListener();
    }

    private void registerCreateButtonClickListener()
    {
        /**
         * Create a new listener instance
         */
        CreateRuleButtonOnClickListener createRuleButtonOnClickListener = new CreateRuleButtonOnClickListener(this);
        /**
         * Get an instance on the Create button, then register the previously
         * created listener
         */
        Button createButton = (Button) findViewById(R.id.createRuleBtn);
        createButton.setOnClickListener(createRuleButtonOnClickListener);
    }

    private void registerCancelButtonOnClickListener()
    {
        /**
         * Create a new instance of the Cancel button listener
         */
        EditCallMgmtCancelBtnOnClickListener editCallMgmtCancelBtnOnClickListener = new EditCallMgmtCancelBtnOnClickListener();
        /**
         * Obtain a new object instance on the cancel Button
         */
        Button cancelButton = (Button) findViewById(R.id.cancelCreationRuleBtn);
        cancelButton.setOnClickListener(editCallMgmtCancelBtnOnClickListener);
    }

    private void adjustRelativePositions()
    {
        /**
         * Fill in the call handling selection options from the configured
         * string array
         */
        Spinner callHandlingMethodSpinner = (Spinner) findViewById(R.id.callHandlingMethodSpinner);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.callHandlingOptionsStringArray, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        callHandlingMethodSpinner.setAdapter(arrayAdapter);
        /**
         * Fill in the call ringing selection options from the configured string
         * array
         */
        Spinner callRingingTypeSpinner = (Spinner) findViewById(R.id.callRingingTypeSpinner);
        ArrayAdapter<CharSequence> ringingTypeArrayAdapter = ArrayAdapter.createFromResource(this, R.array.callRingingOptionsStringArray, android.R.layout.simple_spinner_item);
        ringingTypeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        callRingingTypeSpinner.setAdapter(ringingTypeArrayAdapter);
    }

    private void registerMapShowButtonOnClickListener()
    {
        /**
         * Obtain an object handle on the map button
         */
        ImageButton mapButton = (ImageButton) findViewById(R.id.editCallMgmtRuleShowMapBtn);
        /**
         * Use the obtained handle to register for the ClickEventListener
         */
        mapButton.setOnClickListener(new MapButtonOnClickListener(this));
    }

}
