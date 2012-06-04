package com.opencloud.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

public class ShowFriendRequestNotificationSubActivity extends Activity
{
    public static final String NO_PENDING_FRIEND_REQUESTS = "No pending friend requests.";

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_notification_layout);
        ListView notificationsListView = (ListView) findViewById(R.id.notificationsListView);
    }
}