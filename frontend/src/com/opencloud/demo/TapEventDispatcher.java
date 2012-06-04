package com.opencloud.demo;

import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class TapEventDispatcher implements OnItemClickListener
{
        private Context context = null;
        
        public TapEventDispatcher(Context context)
        {
            this.context = context;
        }
        
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            Intent intent = null;
            switch (position)
            {
                case 1:// Add Rule
                    intent = new Intent(context, EditCallManagementRulesSubActivity.class);                    
                    break;
                case 2:// List Rules
                    intent = new Intent(context, ListCallManagementRulesSubActivity.class);                    
                    break;
                case 3:// Show NearBy Friends
                    intent = new Intent(context, ShowNearbyFriendsSubActivity.class);
                    //FunkyToast.makeText(context, "There are currently 0 nearby friends in your current location...", Toast.LENGTH_SHORT, "icon").show();
                    break;
                case 4:// Add Friend
                    intent = new Intent(context, AddFriendSubActivity.class);
                    break;
                case 5:// Remove Friend
                    intent = new Intent(context, RemoveFriendSubActivity.class);                    
                    break;
                case 6:// Friend Requests
                    //intent = new Intent(context, ShowFriendRequestNotificationSubActivity.class);
                    FunkyToast.makeText(context, "Currently, there are no pending companionship requests...", Toast.LENGTH_SHORT, "icon").show();
                    break;
                case 7:// Check Balance
                    /**
                     * Here we need to generated some random numbers (doubles) to mock the balance
                     * that the user have, this will look like a balance inquiry message has been sent from
                     * the device to the network. The maximum balance allowed by the network is 500£
                     */                                        
                    FunkyToast.makeText(context, "\n\nYour current balance is " + new Random().nextInt(500) + " £\n\n", Toast.LENGTH_LONG, "icon").show();
                    break;
                case 8:// Top Up
                    FunkyToast.makeText(context, "This feature is currently under development", Toast.LENGTH_SHORT, "icon").show();
                    break;
                case 9:// Settings
                    intent = new Intent(context, SettingsSubActivity.class);
                    break;
                case 10:// About
                    intent = new Intent(context, AboutSubActivity.class);
                    break;
                default:                    
            }
            /**
             * Start the appropriate activity basing on the user's selection.
             */
            if(intent != null)
            {
                context.startActivity(intent);
            }
        }
 
}
