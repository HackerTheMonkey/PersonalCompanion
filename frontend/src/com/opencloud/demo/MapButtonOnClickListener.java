package com.opencloud.demo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class MapButtonOnClickListener implements OnClickListener
{
    private Context context = null;

    public MapButtonOnClickListener(Context context)
    {
        this.context = context;
    }

    @Override
    public void onClick(View v)
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "The map button has been clicked...");
        /**
         * Start the MapActivity
         */
        Intent intent = new Intent(context, PositionSelectionMapSubActivity.class);        
        context.startActivity(intent);
    }

}
