package com.opencloud.demo;

import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;

public class PositionSelectionMapSubActivity extends MapActivity
{

    private Context context = this;
    private MapController mapController = null;
    private final String toastMessage = "The entered address/postcode can not be found...";
    protected GestureDetector gestureDetector = null;
    private final int DEFAULT_ZOOM_LEVEL = 17;
    /**
     * This static variable will enable other parts of the application from
     * directly obtaining an access to the instance of this activity to interact
     * with it synchronously.
     */
    protected static PositionSelectionMapSubActivity positionSelectionMapSubActivity = null;

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
        setContentView(R.layout.position_selection_map_layout);
        /**
         * Obtain an object handle on the displayed Google map
         */
        PositionSelectionMapView mapView = (PositionSelectionMapView) findViewById(R.id.positionSelectionMapView);
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
         * Register for an onClick event listener for the post code location on
         * click event.
         */
        registerPostcodeButtonClickListener();
        /**
         * Make a reference to this activity publicly available, i.e. a self
         * reference
         */
        this.positionSelectionMapSubActivity = this;
    }

    private void registerPostcodeButtonClickListener()
    {
        /**
         * Obtain an object handle on the find postcode button
         */
        Button findPostcodeButton = (Button) findViewById(R.id.updatePostcodeButton);
        /**
         * Register an onClick event listener
         */
        findPostcodeButton.setOnClickListener(new View.OnClickListener()
        {
            /**
             * Upon clicking on this button, the address/postcode value entered
             * into the text area will be grabbed and geocoded into a
             * corresponding geographical coordinates and then to center the map
             * around it.
             */
            @Override
            public void onClick(View clickedView)
            {
                /**
                 * Get an object handle on the postcode text edit and get the
                 * value entered in there.
                 */
                EditText postcodeEditText = (EditText) findViewById(R.id.enterPostcodeEditText);
                String addressOrPostCode = postcodeEditText.getText().toString();
                /**
                 * Convert the entered address into a geocoded address in order
                 * to extract the longitude and the latitude out of it.
                 */
                Geocoder geocoder = new Geocoder(context);
                if (addressOrPostCode != null)
                {
                    try
                    {
                        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "entered address: " + addressOrPostCode);
                        List<Address> addressesList = geocoder.getFromLocationName(addressOrPostCode, 1);
                        if (addressesList.size() != 0)
                        {
                            Address address = addressesList.get(0);
                            /**
                             * If the entered text represent an existing
                             * address, then centre the map around it.
                             * otherwise, output a Toast message notifying the
                             * user that the address can't be obtained.
                             */
                            if (address != null)
                            {
                                /**
                                 * Animate to the entered location
                                 */
                                Double longitude = address.getLongitude() * 1E6;
                                Double latitude = address.getLatitude() * 1E6;
                                GeoPoint geoPoint = new GeoPoint(latitude.intValue(), longitude.intValue());
                                mapController.animateTo(geoPoint);
                            }
                            else
                            {
                                FunkyToast.makeText(context, toastMessage, Toast.LENGTH_SHORT, "google_maps_icon").show();
                                postcodeEditText.setText("");
                            }
                        }
                        else
                        {
                            FunkyToast.makeText(context, toastMessage, Toast.LENGTH_SHORT, "google_maps_icon").show();
                            postcodeEditText.setText("");
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        FunkyToast.makeText(context, toastMessage, Toast.LENGTH_SHORT, "google_maps_icon").show();
                        postcodeEditText.setText("");
                    }
                }
            }
        });
    }
}
