package com.opencloud.demo;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

public class PositionSelectionMapView extends MapView implements PositionSelectionDoubleTapListener
{
    private GestureDetector gestureDetector = null;
    private Context context = null;

    public PositionSelectionMapView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        registerDoubleTapEventsReception();
        this.context = context;
    }

    public PositionSelectionMapView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        registerDoubleTapEventsReception();
        this.context = context;
    }

    public PositionSelectionMapView(Context context, String apiKey)
    {
        super(context, apiKey);
        registerDoubleTapEventsReception();
        this.context = context;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        gestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);

    }

    private void registerDoubleTapEventsReception()
    {
        /**
         * Create a new instance of an OnDoubleTapGestureListener which is at
         * the same time a GestureListener. This is going to be passed to the
         * constructor of the GestureDetector when instantiating it.
         */
        PositionSelectionOnTouchListener positionSelectionOnTouchListener = new PositionSelectionOnTouchListener();
        /**
         * Create a new instance of the GestureDetector
         */
        gestureDetector = new GestureDetector(new OnGestureListenerImpl());
        /**
         * Register a double tap event listener for the sake of the reception of
         * such kind of events.
         */
        gestureDetector.setOnDoubleTapListener(positionSelectionOnTouchListener);
        /**
         * Register the callback interface for the
         * PositionSelectionOnTouchListener in order to propagate back the
         * user-selected longitude-latitude pairs
         */
        positionSelectionOnTouchListener.registerPositionSelectionDoubleTapListener(this);
    }

    @Override
    public void receiveMotionEvent(MotionEvent motionEvent)
    {
        try
        {
            Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "MotionEvent has been received...");
            /**
             * Obtain a Projection object from the MapView
             */
            Projection projection = getProjection();
            /**
             * Create a GeoPoint from the pixels X,Y coordinates
             */
            GeoPoint geoPoint = projection.fromPixels(new Float(motionEvent.getX()).intValue(), new Float(motionEvent.getY()).intValue());
            /**
             * Construct a proper Longitude/Latitude pairs from the created
             * GeoPoint
             */
            double longitude = geoPoint.getLongitudeE6() / 1E6;
            double latitude = geoPoint.getLatitudeE6() / 1E6;
            Geocoder geocoder = new Geocoder(context);
            Address address = geocoder.getFromLocation(latitude, longitude, 1).get(0);
            FunkyToast.makeText(context, "You have selected your position to be: " + address.getAddressLine(0) + "(" + address.getPostalCode() + ") : Longitude: " + longitude + ", Latitude: " + latitude, Toast.LENGTH_LONG, "google_maps_icon").show();
            /**
             * Obtain an object handle on the Lon & Lat EditTexts and fill them
             * up with the retreived longitude and latitude.
             */
            EditText latitudeEditText = (EditText) EditCallManagementRulesSubActivity.editCallManagementRulesSubActivity.findViewById(R.id.editCallMgmtRuleLatitudeTextEdit);
            EditText longitudeEditText = (EditText) EditCallManagementRulesSubActivity.editCallManagementRulesSubActivity.findViewById(R.id.editCallMgmtRuleLongitudeTextEdit);
            /**
             * Populate the text value of the above EditText(s) with the
             * corresponding values of the longitude and latitude
             */
            latitudeEditText.setText(Double.toString(latitude));
            longitudeEditText.setText(Double.toString(longitude));
            /**
             * Close the the currently visible activity that displaying this
             * view
             */
            PositionSelectionMapSubActivity.positionSelectionMapSubActivity.finish();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
