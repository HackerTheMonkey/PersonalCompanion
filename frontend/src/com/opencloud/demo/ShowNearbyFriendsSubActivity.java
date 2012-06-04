package com.opencloud.demo;

import android.os.Bundle;
import android.view.GestureDetector;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;

public class ShowNearbyFriendsSubActivity extends MapActivity
{

    private MapController mapController = null;
    protected GestureDetector gestureDetector = null;
    private final int DEFAULT_ZOOM_LEVEL = 17;
    /**
     * This static variable will enable other parts of the application from
     * directly obtaining an access to the instance of this activity to interact
     * with it synchronously.
     */
    protected static ShowNearbyFriendsSubActivity showNearbyFriendsMapSubActivity = null;

    @Override
    protected boolean isRouteDisplayed()
    {
        return false;
    }

    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        /**
         * Set the content view of the activity to the appropriate layout
         */
        setContentView(R.layout.show_nearby_friends_layout);
        /**
         * Obtain an object handle on the displayed Google map
         */
        PositionSelectionMapView mapView = (PositionSelectionMapView) findViewById(R.id.nearbyFriendsMapView);        
        /**
         * Obtain a MapController and use it to control the the displayed map
         */
        mapController = mapView.getController();
        /**
         * Enable built-in zoom controls to be displayed on the map
         */
        mapView.setBuiltInZoomControls(true);
        /**
         * Set the initial zoom level
         */
        mapController.setZoom(DEFAULT_ZOOM_LEVEL);
        /**
         * Make a reference to this activity publicly available, i.e. a self
         * reference
         */
        this.showNearbyFriendsMapSubActivity = this;
    }
}
