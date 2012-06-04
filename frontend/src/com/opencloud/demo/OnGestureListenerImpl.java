package com.opencloud.demo;

import android.view.MotionEvent;

public class OnGestureListenerImpl implements android.view.GestureDetector.OnGestureListener
{

    @Override
    public boolean onDown(MotionEvent motionEvent)
    {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent01, MotionEvent motionEvent02, float arg2, float arg3)
    {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent)
    {

    }

    @Override
    public boolean onScroll(MotionEvent motionEvent01, MotionEvent motionEvent02, float arg2, float arg3)
    {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent)
    {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0)
    {
        return false;
    }

}
