package com.opencloud.demo;

import android.util.Log;
import android.view.MotionEvent;
import android.view.GestureDetector.OnDoubleTapListener;

public class PositionSelectionOnTouchListener implements OnDoubleTapListener
{
    private PositionSelectionDoubleTapListener positionSelectionDoubleTapListener = null;

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent)
    {
        Log.i(this.getClass().getName(), MainActivity.LOG_PREFIX + "Double tap event detected...");
        positionSelectionDoubleTapListener.receiveMotionEvent(motionEvent);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent)
    {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent)
    {
        return false;
    }

    public void registerPositionSelectionDoubleTapListener(PositionSelectionDoubleTapListener doubleTapListener)
    {
        this.positionSelectionDoubleTapListener = doubleTapListener;
    }

}