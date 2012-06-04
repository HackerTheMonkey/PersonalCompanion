package com.opencloud.demo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.LinearLayout.LayoutParams;

public class AboutSubActivity extends Activity
{   
    private int PORTRAIT_DESIRED_HEIGHT = 500;
    private int PORTRAIT_DESIRED_WIDTH = 400;
    private int LANDSCAPE_DESIRED_HEIGHT = 340;
    private int LANDSCAPE_DESIRED_WIDTH = 600;
    
    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);
        /**
         * Obtain an object representing the about button and assign it's onClick listener
         */
        Button aboutDismissButton = (Button) findViewById(R.id.aboutDismissButton);
        aboutDismissButton.setOnClickListener(new AboutDismissButtonOnClickListener(this));
        /**
         * Get the current orientation and size the views appropriately
         */
        Configuration currentConfig = getResources().getConfiguration();
        adaptToOrientation(currentConfig.orientation);
    }
    /**      
     * A static inner class for the about button OnClickListener
     */
    private static class AboutDismissButtonOnClickListener implements OnClickListener
    {
        private Context context = null;
        
        public AboutDismissButtonOnClickListener(Context context)
        {
            this.context = context;
        }
        
        @Override
        public void onClick(View view)
        {
            ((Activity)context).finish();
        }
        
    }
    /**
     * Here we need to override the onConfigurationChanged() method that will be called by the android
     * framework whenever the orientation of the device changes.     
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {        
        super.onConfigurationChanged(newConfig);
        /**
         * Detect the type of the orientation of the device, then respond to the
         * changes by updating the dimensions of the views appropriately.
         */
        adaptToOrientation(newConfig.orientation);
    }
    /**
     * This method will accept an integer argument indicating the type of the orientation
     * to adapt the display to, then it will change the LayoutParams of the displayed ScrollView
     * to match with the current orientation of the device.
     */
    private void adaptToOrientation(int currentOrientation)
    {
        /**
         * Obtain an object instance of the parent scroll view then get its
         * LayoutParameters
         */
        ScrollView scrollView = (ScrollView) findViewById(R.id.aboutScrollView);
        FrameLayout.LayoutParams layoutParameters = (android.widget.FrameLayout.LayoutParams) scrollView.getLayoutParams();
        /**
         * Check the current orientation and adapt accordingly.
         */
        if(currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            layoutParameters.width = LANDSCAPE_DESIRED_WIDTH;
            layoutParameters.height = LANDSCAPE_DESIRED_HEIGHT;
        }
        else if(currentOrientation == Configuration.ORIENTATION_PORTRAIT)
        {
            layoutParameters.width = PORTRAIT_DESIRED_WIDTH;
            layoutParameters.height = PORTRAIT_DESIRED_HEIGHT;
        }
    }
    
}
