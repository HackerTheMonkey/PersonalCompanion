package com.opencloud.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

public class ActionRebootBroadcastReceiver extends BroadcastReceiver
{
    private Context context = null;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        this.context = context;
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "The device has been rebooted. Starting the Location Update Service...");
        initiateLocationUpdate();
    }

    private void initiateLocationUpdate()
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Initiating location update...");
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener(context);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Location updates has been registered...");
    }
}