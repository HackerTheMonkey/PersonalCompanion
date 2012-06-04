package com.opencloud.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class AddFriendSubActivity extends Activity
{
    private Toast toast = null;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_friend_menu_layout);
        toast = Toast.makeText(getBaseContext(), "NOT IMPLEMENTED!", Toast.LENGTH_LONG);
        toast.show();
    }

    protected void onDestroy()
    {
        super.onDestroy();
        toast.cancel();
    }
}
