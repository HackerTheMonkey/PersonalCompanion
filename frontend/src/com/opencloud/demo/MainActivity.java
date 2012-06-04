package com.opencloud.demo;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ListView;

public class MainActivity extends ListActivity
{
    /**
     * This is a log prefix used to identify specific application log messages.
     */
    public static final String LOG_PREFIX = "RHINO COMPANION LOGGING: ";
    /**
     * This is the name of the SharedPreferences that are used by the application to
     * make sure that initial location updates are done only once per application installation life-time.
     */
    private static final String LOCATION_UPDATED_STATUS = "location_update_status";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(this.getClass().getName(),MainActivity.LOG_PREFIX + "Starting the MainActivity...");        
        super.onCreate(savedInstanceState);               
        /**
         * Get an instance of the ListView that is filling the currently displaying ListActivity
         */
        ListView parentListView = getListView();
        /**
         * Add a Header to the ListView displayed on this Activity, the header view is set to not
         * be selectable, however it affects the number of items in the ListView
         */
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(this.LAYOUT_INFLATER_SERVICE);
        parentListView.addHeaderView(layoutInflater.inflate(R.layout.header_layout, null), null, false);
        /**
         * Set the list adapter of the main ListView
         */
        setListAdapter(new CustomListAdapter(this, ListViewMenuItemsFactory.getMenuItems(), ListViewMenuItemsFactory.getMenuItemsDescriptions(), ListViewMenuItemsFactory.getMenuItemsIcons()));
        /**
         * Set the onItemClickListner
         */
        parentListView.setOnItemClickListener(new TapEventDispatcher(this));
        /**
         * Before exiting the onCreate() method, the activity will register for
         * receiving location updates from the LocationManager system service.
         * This need to be done only once upon the first launch of the
         * application, later on, the registration for location updates will take place
         * upon machine startup.
         */        
        if (!isLocationUpdateInitiated())
        {
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Initiating location updates...");
            initiateLocationUpdate();
        }
    }

    private void initiateLocationUpdate()
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Initiating location update...");
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener(this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Location updates has been registered...");
        /**
         * Set the LOCATION_UPDATED_STATUS boolean flag to true to aid in that
         * the registration for location update requests is done only once
         * during the first time the application is run.
         */
        setLocationUpdateStatus(true);
    }

    private boolean isLocationUpdateInitiated()
    {
        SharedPreferences settings = getSharedPreferences(LOCATION_UPDATED_STATUS, 0);
        return settings.getBoolean(LOCATION_UPDATED_STATUS, false);
    }

    private void setLocationUpdateStatus(boolean status)
    {
        SharedPreferences settings = getSharedPreferences(LOCATION_UPDATED_STATUS, 0);
        SharedPreferences.Editor editor = settings.edit().putBoolean(LOCATION_UPDATED_STATUS, status);
        editor.commit();
    }
    
    /**
     * Here is an inner class that contain a static utility methods that is
     * used to retrieve an Array of menu items to display on the main ListView
     */
    public static class ListViewMenuItemsFactory
    {
        public static String[] getMenuItems()
        {
            return new String[] {
                                    "Add Rule", 
                                    "List Rules", 
                                    "Show Nearby Friends", 
                                    "Add Friend", 
                                    "Remove Friend", 
                                    "Friend Requests", 
                                    "Check Balance", 
                                    "Top Up", 
                                    "Settings", 
                                    "About"
                                };
        }
        
        public static String[] getMenuItemsDescriptions()
        {
            return new String[]{
                                    "Add a new call management rule",
                                    "List the available call management rules",
                                    "Show the friends closest to your current location",
                                    "Send a new friend request",
                                    "Remove a friend from your social network",
                                    "Check the pending friends requests",
                                    "Check your mobile account balance",
                                    "Top up your mobile account balance",
                                    "Edit the network settings",
                                    "About the RhinoCompanion"     
                               };
        }
        
        public static String[] getMenuItemsIcons()
        {
            return new String[]{
                                    "add_rule",
                                    "list_rules",
                                    "show_nearby_friends",
                                    "add_friend",
                                    "remove_friend",
                                    "friend_requests",
                                    "check_balance",
                                    "top_up",
                                    "settings",
                                    "about"
                               };
        }
    }
};