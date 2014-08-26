package org.dwallach.calwatch;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.dwallach.calwatch.proto.WireEvent;
import org.dwallach.calwatch.proto.WireEventList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.squareup.wire.Wire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.wearable.Wearable.API;
import static com.google.android.gms.wearable.Wearable.DataApi;

/**
 * This class pairs up with WearSender
 * Created by dwallach on 8/25/14.
 *
 */
public class WearReceiver extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private List<EventWrapper> eventList = null;
    private int maxLevel = 0;
    private boolean showSeconds = true;
    private int faceMode = ClockFace.FACE_TOOL;
    private GoogleApiClient mGoogleApiClient = null;
    private static WearReceiver singleton;

    public WearReceiver() {
        super();
        singleton = this;
    }

    public static WearReceiver getSingleton() {
        return singleton;
    }

    public boolean getShowSeconds() { return showSeconds; }
    public int getFaceMode() { return faceMode; }
    public int getMaxLevel() { return maxLevel; }

    public List<EventWrapper> getEventList() {
        return eventList;
    }

    private void newEventBytes(byte[] eventBytes) {
        Wire wire = new Wire();
        WireEventList wireEventList = null;

        try {
            wireEventList = (WireEventList) wire.parseFrom(eventBytes, WireEventList.class);
        } catch (IOException ioe) {
            Log.e("WearReceiver", "parse failure on protobuf: " + ioe.toString());
            return;
        }

        ArrayList<EventWrapper> results = new ArrayList<EventWrapper>();

        for (WireEvent wireEvent : wireEventList.events) {
            results.add(new EventWrapper(wireEvent));

            if (wireEvent.maxLevel > this.maxLevel)
                this.maxLevel = wireEvent.maxLevel;
        }

        eventList = results;
        Log.v("WearReceiver", "new calendar event list, " + results.size() + " entries");
    }



    //
    // Official documentation: https://developer.android.com/training/wearables/data-layer/events.html
    // Very, very helpful: http://www.doubleencore.com/2014/07/create-custom-ongoing-notification-android-wear/
    //


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v("WearReceiver", "data changed!");

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        if (!mGoogleApiClient.isConnected()) {
            Log.v("WearReceiver", "reconnecting GoogleApiClient?!");
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e("WearReceiver", "Service failed to connect to GoogleApiClient.");
                return;
            }
        }

        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if ("/calwatch".equals(path)) {
                    // Get the data out of the event
                    DataMapItem dataMapItem =
                            DataMapItem.fromDataItem(event.getDataItem());
                    DataMap dataMap = dataMapItem.getDataMap();

                    if(dataMap.containsKey("Events")) {
                        byte[] eventBytes = dataMap.getByteArray("Events");
                        newEventBytes(eventBytes);
                    }

                    if(dataMap.containsKey("ShowSeconds")) {
                        showSeconds = dataMap.getBoolean("ShowSeconds");
                        Log.v("WearReceiver", "showSeconds updated: " + Boolean.toString(showSeconds));
                    }

                    if(dataMap.containsKey("FaceMode")) {
                        faceMode = dataMap.getInt("FaceMode");
                        Log.v("WearReceiver", "faceMode updated: " + faceMode);
                    }
                } else {
                    Log.v("WearReceiver", "received data on weird path: "+ path);
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v("WearReceiver", "onCreate!");
        if(mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
            Log.v("WearReceiver", "Google API connected! Hopefully.");
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v("WearReceiver", "onConnected!");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.v("WearReceiver", "suspended connection!");
        mGoogleApiClient.disconnect();
        mGoogleApiClient = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the next section.

        Log.v("WearReceiver", "lost connection!");
        mGoogleApiClient.disconnect();
        mGoogleApiClient = null;
    }

    public void onPeerConnected(Node peer) {
        Log.v("WearReceiver", "phone is connected!, "+peer.getDisplayName());
    }

    public void onPeerDisconnected(Node peer) {
        Log.v("WearReceiver", "phone is disconnected!, "+peer.getDisplayName());
    }
}
