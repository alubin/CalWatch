/*
 * CalWatch
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/calwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/calwatch/licensing.html
 */
package org.dwallach.calwatch;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Switch;

import java.util.Observable;
import java.util.Observer;


public class PhoneActivity extends Activity implements Observer {
    private final static String TAG = "PhoneActivity";

    private RadioButton toolButton, numbersButton, liteButton;
    private MyViewAnim clockView;
    private Switch secondsSwitch, dayDateSwitch;

    private ClockState clockState;
    private boolean disableUICallbacks = false;

    private ClockState getClockState() {
        if(clockState == null) {
            Log.v(TAG, "reconnecting clock state");
            clockState = ClockState.getSingleton();
            clockState.addObserver(this);
        }
        return clockState;
    }

    public PhoneActivity() {
        super();
    }

    //
    // this will be called, eventually, from whatever feature is responsible for
    // restoring saved user preferences
    //
    private void setFaceModeUI(int mode, boolean showSeconds, boolean showDayDate) {
        Log.v(TAG, "setFaceModeUI");
        if(toolButton == null || numbersButton == null || liteButton == null || secondsSwitch == null || dayDateSwitch == null) {
            Log.v(TAG, "trying to set UI mode without buttons active yet");
            return;
        }

        disableUICallbacks = true;

        try {
            switch (mode) {
                case ClockState.FACE_TOOL:
                    toolButton.performClick();
                    break;
                case ClockState.FACE_NUMBERS:
                    numbersButton.performClick();
                    break;
                case ClockState.FACE_LITE:
                    liteButton.performClick();
                    break;
                default:
                    Log.v(TAG, "bogus face mode: " + mode);
                    break;
            }

            secondsSwitch.setChecked(showSeconds);
            dayDateSwitch.setChecked(showDayDate);
        } catch(Throwable throwable) {
            // probably a called-from-wrong-thread-exception, we'll just ignore it
            Log.v(TAG, "ignoring exception while updating button state");
        }

        disableUICallbacks = false;
    }

    private void getFaceModeFromUI() {
        Log.v(TAG, "getFaceModeFromUI");
        int mode = -1;

        if(toolButton == null || numbersButton == null || liteButton == null || secondsSwitch == null || dayDateSwitch == null) {
            Log.v(TAG, "trying to get UI mode without buttons active yet");
            return;
        }

        if(toolButton.isChecked())
            mode = ClockState.FACE_TOOL;
        else if(numbersButton.isChecked())
            mode = ClockState.FACE_NUMBERS;
        else if(liteButton.isChecked())
            mode = ClockState.FACE_LITE;
        else Log.v(TAG, "no buttons are selected? weird.");

        boolean showSeconds = secondsSwitch.isChecked();
        boolean showDayDate = dayDateSwitch.isChecked();

        if(mode != -1) {
            getClockState().setFaceMode(mode);
        }
        getClockState().setShowSeconds(showSeconds);
        getClockState().setShowDayDate(showDayDate);

        getClockState().pingObservers(); // we only need to do this once, versus a whole bunch of times when it was happening internally
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Create!");

    }

    private void activitySetup() {
        Log.v(TAG, "And in the beginning ...");

        VersionWrapper.logVersion(this);

        getClockState(); // initialize it, if it's not already here

        BatteryWrapper.init(this);

        setContentView(R.layout.activity_phone);

        // Core UI widgets: find 'em
        liteButton = (RadioButton) findViewById(R.id.liteButton);
        toolButton = (RadioButton) findViewById(R.id.toolButton);
        numbersButton = (RadioButton) findViewById(R.id.numbersButton);
        clockView = (MyViewAnim) findViewById(R.id.surfaceView);
        secondsSwitch = (Switch) findViewById(R.id.showSeconds);
        dayDateSwitch = (Switch) findViewById(R.id.showDayDate);
//        clockView.setSleepInEventLoop(true);

        Log.v(TAG, "registering callback");

        View.OnClickListener myListener = new View.OnClickListener() {
            public void onClick(View v) {
                if(!disableUICallbacks)
                    getFaceModeFromUI();
                if(clockView != null)
                    clockView.redrawClockSlow("click listener");
            }
        };

        liteButton.setOnClickListener(myListener);
        toolButton.setOnClickListener(myListener);
        numbersButton.setOnClickListener(myListener);
        secondsSwitch.setOnClickListener(myListener);
        dayDateSwitch.setOnClickListener(myListener);

        WatchCalendarService.kickStart(this);  // bring it up, if it's not already up

        PreferencesHelper.loadPreferences(this);

        onResume();
        Log.v(TAG, "activity setup complete");
    }

    protected void onStop() {
        super.onStop();
        Log.v(TAG, "Stop!");

        // perhaps incorrect assumption: if our activity is being killed, onStop will happen beforehand,
        // so we'll deregister our clockState observer, allowing this Activity object to become
        // garbage. A new one will be created if the activity ever comes back to life, which
        // will call getClockState(), which will in turn resurrect observer. Setting clockState=null
        // means that, even if this specific Activity object is resurrected from the dead, we'll
        // just reconnect it the next time somebody internally calls getClockState(). No harm, no foul.

        // http://developer.android.com/reference/android/app/Activity.html

        try {
            LockWrapper.lock();           // locking so we wait until a redraw is finished

            if (clockView != null)
                clockView.stop();

            getClockState().deleteObserver(this);
            clockState = null;
            watchFaceRunning = false;
            killAlarm();
        } finally {
            LockWrapper.unlock();
        }
    }

    protected void onStart() {
        super.onStart();
        Log.v(TAG, "Start!");

        watchFaceRunning = true;
        activitySetup();
    }

    protected void onResume() {
        super.onResume();
        Log.v(TAG, "Resume!");
        if(clockView != null) {
            clockView.redrawClockSlow("activity:onResume");
            clockView.resumeMaxHertz();
        }
        watchFaceRunning = true;
        initAlarm();
    }

    protected void onPause() {
        super.onPause();
        Log.v(TAG, "Pause!");

        try {
            LockWrapper.lock();        // locking so we wait until a redraw is finished

            if (clockView != null)
                clockView.pause();

            watchFaceRunning = false;
            killAlarm();
        } finally {
            LockWrapper.unlock();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.phone, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable observable, Object data) {
        // somebody changed *something* in the ClockState, causing us to get called
        Log.v(TAG, "Noticed a change in the clock state; saving preferences");
        setFaceModeUI(getClockState().getFaceMode(), getClockState().getShowSeconds(), getClockState().getShowDayDate());
        PreferencesHelper.savePreferences(this);
    }


    //
    // from here on down, this is a stripped version of the alarm management we do on the watch, notably removing all of
    // the state changes that are delivered by the undocumented apis
    //

    private AlarmManager alarmManager;
    private boolean watchFaceRunning = true, alarmSet = false;
    private BroadcastReceiver tickReceiver;
    private static final String ACTION_KEEP_WATCHFACE_AWAKE = "intent.action.keep.watchface.awake";
    private PendingIntent pendingIntent = null;

    private PendingIntent getPendingIntent() {
        if(pendingIntent == null && clockView != null)
            pendingIntent =  PendingIntent.getBroadcast(clockView.getContext(), 0, new Intent(ACTION_KEEP_WATCHFACE_AWAKE), 0);
        return pendingIntent;
    }

    private void initAlarm() {
//        Log.v(TAG, "initAlarm");
        if (alarmManager == null) {
            Log.v(TAG, "initializing second-scale alarm");

            if(clockView == null) {
                Log.e(TAG, "oops, no clockView");
            } else {
                alarmManager = (AlarmManager) clockView.getContext().getSystemService(Context.ALARM_SERVICE);

                // every five seconds, we'll redraw the minute hand while sleeping; this gives us 12 ticks per minute, which should still look smooth
                // while otherwise saving lots of power
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, 5000, getPendingIntent());
                alarmSet = true;
            }
        }

        if (!alarmSet) {
            Log.e(TAG, "failed to initialize alarm");
        }

        watchFaceRunning = true;

        // Create a broadcast receiver to handle change in time
        // Source: http://sourabhsoni.com/how-to-use-intent-action_time_tick/
        // Also: https://github.com/twotoasters/watchface-gears/blob/master/library/src/main/java/com/twotoasters/watchface/gears/widget/Watch.java

        // Note that we don't strictly need this stuff, since we're running a whole separate thread to do the graphics, but this
        // still serves a purpose. If that thread isn't working, this will still work and we'll get at least *some* updates
        // on the screen, albeit far less frequent.
        if (tickReceiver == null) {
            tickReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String actionString = intent.getAction();

                    if (!watchFaceRunning) {
                        // received a timer event but we're not supposed to be doing graphics right now,
                        // so just quietly return

                        return;
                    }

                    if (actionString.equals(Intent.ACTION_TIME_CHANGED) || actionString.equals(Intent.ACTION_TIME_TICK) || actionString.equals(ACTION_KEEP_WATCHFACE_AWAKE)) {
                        if (clockView == null) {
                            Log.v(TAG, actionString + " received, but can't redraw");
                        } else {
//                            Log.v(TAG, actionString + " received, redrawing");
                            clockView.redrawClockSlow("tickReceiver:" + actionString);
                        }
                        initAlarm(); // just in case it's not set up properly
                    } else {
                        Log.e(TAG, "Unknown intent received: " + intent.toString());
                    }
                }
            };

            //Register the broadcast receiver to receive TIME_TICK
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(ACTION_KEEP_WATCHFACE_AWAKE);

            registerReceiver(tickReceiver, filter);
        }
    }

    private void killAlarm() {
        Log.v(TAG, "killAlarm");

        if(pendingIntent != null) {
            alarmSet = false;
            alarmManager.cancel(pendingIntent);
            pendingIntent = null;
            alarmManager = null;
        }

        if(tickReceiver != null) {
            unregisterReceiver(tickReceiver);
            tickReceiver = null;
        }

        watchFaceRunning = false;
    }
}
