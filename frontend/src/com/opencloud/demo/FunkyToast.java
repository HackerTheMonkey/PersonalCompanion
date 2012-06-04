package com.opencloud.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Nicer toast.. shows also the app logo! It has the methods with the same
 * signature as {@Toast}
 */
public class FunkyToast
{    
    public static Toast makeText(Context context, CharSequence text, int duration, String imageResourceName)
    {
        /**
         * Get the LayoutInflater system service
         */
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        /**
         * Obtain an inflated view of the funky_toast_layout, this serves as a temporary view that will be used to obtain
         * the root of the funky_toast_layout.
         */
        View tempLayout = inflater.inflate(R.layout.funky_toast_layout, null);
        ViewGroup rootViewGroup = (ViewGroup) tempLayout.findViewById(R.id.funky_toast_layout_root);
        /**
         * Here we need to inflate the funky_toast_layout but this time we will be specifying the root ViewGroup of the
         * generated view hierarchy.
         */
        ViewGroup toastLayout = (ViewGroup) inflater.inflate(R.layout.funky_toast_layout, rootViewGroup);
        /**
         * Change the image source of the ImageView of this custom toast
         * as per the passed imageResourceName parameter.
         */
        ImageView logoImage = (ImageView) toastLayout.findViewById(R.id.funkyToastLogoImage);
        int iconResourceId = context.getResources().getIdentifier(context.getPackageName() + ":drawable/" + imageResourceName, null, null);
        logoImage.setImageResource(iconResourceId);
        /**
         * Set the text of the Toast message as per the passed in
         * text
         */
        TextView toastText = (TextView) toastLayout.findViewById(R.id.funkyToastLogoText);
        toastText.setText(text);
        /**
         * Create a new toast, set its position and its content view
         */
        Toast toast = new Toast(context);        
        toast.setDuration(Toast.LENGTH_LONG);
        //toast.setMargin(0.25f, 0.15f);        
        toast.setView(toastLayout);
        /**
         * return back the generated toast.
         */
        return toast;
    }
    
}
