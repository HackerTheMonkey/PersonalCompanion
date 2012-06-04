package com.opencloud.demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Looper;

public class ProgressDialogDismisser implements Runnable
{
    private ProgressDialog progressDialog = null;
    private Activity activity = null;

    public ProgressDialogDismisser(ProgressDialog progressDialog, Activity activity)
    {
        this.progressDialog = progressDialog;
        this.activity = activity;
    }

    @Override
    public void run()
    {
        Looper.prepare();
        for (int i = 0 ; i < 101 ; i++)
        {
            try
            {
                Thread.sleep(i);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            progressDialog.setProgress(i);
        }
        progressDialog.dismiss();
        activity.finish();
    }

}
