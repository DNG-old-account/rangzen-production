package org.denovogroup.rangzen.beta.locationtracking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.beta.NetworkHandler;
import org.denovogroup.rangzen.beta.WifiStateReceiver;
import org.denovogroup.rangzen.ui.Opener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Liran on 8/31/2015.
 *
 * This service is the main handler for all location tracking events in the app, it will sample location updates
 * in a fixed interval and broadcast them to a server defined by the NetworkHandler class, every item is first
 * being saved into a local memory cache and then being sent to the server, if the sending process was successful
 * the item will be removed from the cache. This lazy sending mechanism ensure that all the data will be sent to
 * the server eventually.
 *
 * Since it is impossible to set exact time periods to receive location updates (the system may send a lot more
 * causing data overflow), a timer is responsible for sending the last received location in a fixed interval.
 * The system will only update this location reference but will not send it on its own.
 */
public class TrackingService extends Service implements LocationListener {

    private LocationManager locationManager;
    private Timer locationUpdateTimer = new Timer();

    private static final String LOG_TAG = "TrackingService";
    private static final int SECOND = 1000;
    private static final int UPDATE_TIME_INTERVAL = 10 * SECOND;
    private static final long FLUSH_PERIOD = 120 *SECOND;
    private static final float UPDATE_DISTANCE_INTERVAL = 1; // this is the real limit on location updates, min time is just a hint
    private static TrackedLocation lastLocationSent;
    private static TrackedLocation lastLocationUpdate;
    private static boolean isFlushing = false;
    private static long flushInterval = 0;

    public static WifiStateReceiver receiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "Tracking service created");

        receiver = new WifiStateReceiver(getApplicationContext());

        locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);

        locationManager.addGpsStatusListener(new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                switch(event){
                    case GpsStatus.GPS_EVENT_STARTED:
                        dismissNoGPSNotification(TrackingService.this);
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        showNoGPSNotification(TrackingService.this);
                        break;
                }
            }
        });

        /*Register for updates from the specified location provider, all incoming transmission will be handled
          by the service acting as the listener*/
        //TODO maybe instead of forcing GPS as a provider i should use get best provider
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME_INTERVAL, UPDATE_DISTANCE_INTERVAL, this);

        /*Since it is impossible to time when we will get updates from the location provider, we will force
        the rhythm at sampling and sending to server */
        locationUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                flushInterval += UPDATE_TIME_INTERVAL;
                /* Check if last update was already sent (i.e the device couldn't get another update by the time the timer interval
                 ended) and save it if not */
                if (lastLocationUpdate != null && (lastLocationSent == null || lastLocationUpdate.timestamp > lastLocationSent.timestamp)) {
                    saveToCache(lastLocationUpdate);
                }
                //try to send everything in local memory to the server
                flushCache();
            }
        }, 0, UPDATE_TIME_INTERVAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e){}
            receiver = null;
        }

        locationManager.removeUpdates(this);
        Log.d(LOG_TAG, "Tracking service stopped");

        locationUpdateTimer.cancel();
    }



    @Override
    public void onLocationChanged(Location location) {
        //update current location
        lastLocationUpdate = new TrackedLocation(location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if(status == LocationProvider.AVAILABLE){
            flushCache();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        flushCache();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /** Attempt to send data stored on cache to the server, this will clean any item which was succesfully sent
     *
     * @return true if all items have been sent and cache is now empty, false otherwise
     */
    private boolean flushCache(){
        if(!isFlushing && NetworkHandler.isEligableForSending && flushInterval > FLUSH_PERIOD){
            isFlushing = true;
            List<TrackedLocation> sendlist = new ArrayList<>();
            LocationCacheHandler cacheHandler = LocationCacheHandler.getInstance(getApplicationContext());
            Cursor cursor = cacheHandler.getLocations();
            if(cursor != null){
                cursor.moveToFirst();
                while(!cursor.isAfterLast()) {
                    TrackedLocation trackedLocation = new TrackedLocation(
                            cursor.getDouble(cursor.getColumnIndex(LocationCacheHandler.LATITUDE_COL)),
                            cursor.getDouble(cursor.getColumnIndex(LocationCacheHandler.LONGITUDE_COL)),
                            cursor.getLong(cursor.getColumnIndex(LocationCacheHandler.TIMESTAMP_COL))
                    );

                    sendlist.add(trackedLocation);
                    cursor.moveToNext();
                }

                cursor.close();

                NetworkHandler dbHandler = NetworkHandler.getInstance(getApplicationContext());

                int sentCount = dbHandler.sendLocations(sendlist);

                if(NetworkHandler.isNetworkConnected() && dbHandler != null) {
                    if (sentCount > 0) {
                        cacheHandler.removeLocations(sentCount);
                        lastLocationSent = sendlist.get(sendlist.size() - 1);
                    }
                }

                if(cacheHandler.getLocationsCount() <= 0) {
                    flushInterval = 0;
                    isFlushing = false;
                    return true;
                }
            }
            isFlushing = false;
        }
        return false;
    }

    /** Request the NetworkHandler to send the object to server
     *
     * @param trackedLocation the object to be sent
     * @return true is object has been sent succesfuly, false otherwise
     */
    private boolean sendToServer(TrackedLocation trackedLocation){
        NetworkHandler dbHandler = NetworkHandler.getInstance(getApplicationContext());
        if(NetworkHandler.isNetworkConnected() && trackedLocation != null && dbHandler != null){
            if(dbHandler.sendLocation(trackedLocation)){
                return true;
            }
        }
        return false;
    }

    /** Saves a tracking object to local storage
     *
     * @param trackedLocation to be saved into storage
     */
    private void saveToCache(TrackedLocation trackedLocation){
        if(trackedLocation != null) {
            LocationCacheHandler cacheHandler = LocationCacheHandler.getInstance(getApplicationContext());
            cacheHandler.insertLocation(trackedLocation);
        }
    }

    /** create and display a dialog prompting the user about the enabled
     * state of the bluetooth service.
     */
    private void showNoGPSNotification(Context context){
        if(context == null) return;

        int notificationId = R.string.dialog_no_gps_message;

        Intent notificationIntent = new Intent(context, Opener.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(context).setContentTitle(context.getText(R.string.dialog_no_gps_title))
                .setContentText(context.getText(R.string.dialog_no_gps_message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        mNotificationManager.notify(notificationId, notification);
    }

    /** dismiss the no bluetooth notification if showing
     */
    private void dismissNoGPSNotification(Context context){
        int notificationId = R.string.dialog_no_gps_message;

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }
}
