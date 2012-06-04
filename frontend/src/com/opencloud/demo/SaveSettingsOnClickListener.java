package com.opencloud.demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class SaveSettingsOnClickListener implements OnClickListener
{
    private Activity activity = null;
    private ProgressDialog progressDialog = null;

    public SaveSettingsOnClickListener(Activity activity)
    {
        this.activity = activity;
    }

    @Override
    public void onClick(View arg0)
    {
        /**
         * Need to display a progress bar indicating the start of the settings
         * storing process.
         */
        showProgressBar();
        /**
         * Start storing the input field values into the appropriate
         * SharedPreferences.
         */
        storeValues();
        /**
         * Dismiss the progress bar as well as the currently showing activity to
         * indicate the completion of the storing operation.
         */
        dismissProgressBarAndActivity();
    }

    private void dismissProgressBarAndActivity()
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "dismissProgressBar() has been called...");
        ProgressDialogDismisser progressDialogDismisser = new ProgressDialogDismisser(progressDialog, activity);
        Thread thread = new Thread(progressDialogDismisser);
        thread.start();
    }

    private void storeValues()
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "storeValues() has been called...");
        /**
         * Get the SharedPreferences file handle
         */
        SharedPreferences settings = activity.getSharedPreferences(SettingsSubActivity.SETTINGS_PAGE_SHARED_PREF_FILE, 0);
        /**
         * Obtain object instances on all the settings' page elements, retrieve
         * their values and store the obtained values into the SharedPreferences
         */
        SharedPreferences.Editor settingsEditor = settings.edit();
        /**
         * Update the shared preferences
         */
        settingsEditor.putString(SettingsSubActivity.LOCATION_SERVER_SBB_IP_ADDRESS, ((EditText) activity.findViewById(R.id.enterServerIPAddressEditText)).getText().toString());
        settingsEditor.putString(SettingsSubActivity.LOCATION_SERVER_SBB_PORT_NUMBER, ((EditText) activity.findViewById(R.id.enterPortNumberEditText)).getText().toString());
        settingsEditor.putString(SettingsSubActivity.ADDRESS_OF_RECORD, ((EditText) activity.findViewById(R.id.settingsAddressOfRecordTextEdit)).getText().toString());
        /**
         * Commit the changes
         */
        settingsEditor.commit();
    }

    private void showProgressBar()
    {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("Please wait: the changes you have made are being saved");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

}
