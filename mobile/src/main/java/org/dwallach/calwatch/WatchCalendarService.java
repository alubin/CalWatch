package org.dwallach.calwatch;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import java.util.Observable;
import java.util.Observer;

public class WatchCalendarService extends Service {
    private static WatchCalendarService singletonService;
    private WearSender wearSender;
    private ClockFaceStub clockFaceStub;
    private CalendarFetcher calendarFetcher;

    public WatchCalendarService() {
        Log.v("WatchCalendarService", "starting calendar fetcher");
        singletonService = this;

        wearSender = new WearSender();
        calendarFetcher = new CalendarFetcher(this); // automatically allocates a thread and runs

        calendarFetcher.addObserver(new Observer() {
                                        @Override
                                        public void update(Observable observable, Object data) {
                                            calHandler();
                                        }
                                    });

        clockFaceStub = new ClockFaceStub();
    }

    public ClockFaceStub getClockFace() {
        return clockFaceStub;
    }

    public static WatchCalendarService getSingletonService() {
        return singletonService;
    }

    public void savePreferences() {
        Log.v("WatchCalendarService", "savePreferences");
        SharedPreferences prefs = getSharedPreferences("org.dwallach.calwatch.prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("showSeconds", clockFaceStub.getShowSeconds());
        editor.putInt("faceMode", clockFaceStub.getFaceMode());

        if(!editor.commit())
            Log.v("WatchCalendarService", "savePreferences commit failed ?!");

        wearSender.store(clockFaceStub);
        wearSender.sendNow(false);
    }

    public void loadPreferences() {
        Log.v("WatchCalendarService", "loadPreferences");

        if(clockFaceStub == null) {
            Log.v("WatchCalendarService", "loadPreferences has no clock to put them in");
            return;
        }

        PhoneActivity phoneActivity = PhoneActivity.getSingletonActivity();

        SharedPreferences prefs = getSharedPreferences("org.dwallach.calwatch.prefs", MODE_PRIVATE);
        boolean showSeconds = prefs.getBoolean("showSeconds", true);
        int faceMode = prefs.getInt("faceMode", ClockFaceStub.FACE_TOOL);

        clockFaceStub.setFaceMode(faceMode);
        clockFaceStub.setShowSeconds(showSeconds);

        wearSender.store(clockFaceStub);
        wearSender.sendNow(false);

        if(phoneActivity != null) {
            if (phoneActivity.toggle == null || phoneActivity.toolButton == null || phoneActivity.numbersButton == null || phoneActivity.liteButton == null) {
                Log.v("WatchCalendarService", "loadPreferences has no widgets to update");
                return;
            }

            phoneActivity.toggle.setChecked(showSeconds);
            phoneActivity.setFaceModeUI(faceMode);
        }
    }

    // this is called when there's something new from the calendar DB; we'll be running
    // on the calendar's thread, not the UI thread
    private void calHandler() {
        if(wearSender == null) {
            Log.v("WatchCalendarService", "no wear sender?!");
            return;
        }
        wearSender.store(clockFaceStub);
        wearSender.store(calendarFetcher.getContent().getWireEvents());
        wearSender.sendNow(true);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
