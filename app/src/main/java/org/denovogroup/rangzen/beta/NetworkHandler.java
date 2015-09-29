package org.denovogroup.rangzen.beta;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.parse.SendCallback;

import org.denovogroup.rangzen.backend.Utils;
import org.denovogroup.rangzen.beta.locationtracking.TrackedLocation;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.internal.util.collections.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Liran on 8/31/2015.
 *
 * This Class is responsible for sending requests to the database server, at the time of writing
 * the database is parse which handle all the broadcasting logic on its own.
 *
 */
public class NetworkHandler {

    private static NetworkHandler instance;
    private static Context context;

    //general keys
    private static final String USERID_KEY = "Userid";
    private static final String TIMESTAMP_KEY = "Timestamp";
    private static final String TIME_KEY = "Time";

    //Location tracking keys
    private static final String LONGITUDE_KEY = "Longitude";
    private static final String LATITUDE_KEY = "Latitude";

    private static boolean isSendingPinnedInBackground = false;
    public static boolean isEligableForSending = false;

    private static int[] updateSchedule = null;

    private static Timer updateScheduleTimer;

    public static NetworkHandler getInstance(){
        if(instance == null || context == null){
            return null;
        }
        return instance;
    }

    public static NetworkHandler getInstance(final Context ctx){
        if(instance == null){
            instance = new NetworkHandler();
        }
        if(updateScheduleTimer == null){
            updateScheduleTimer = new Timer();
            updateScheduleTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if ((updateSchedule != null && updateSchedule.length > 0 && updateSchedule[0] > -1)) {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(System.currentTimeMillis());
                        isEligableForSending = false;

                        for (int i : updateSchedule) {
                            if (i == c.get(Calendar.HOUR_OF_DAY)) {
                                isEligableForSending = true;
                                break;
                            }
                        }

                        if (isEligableForSending && NetworkHandler.getInstance() != null && context != null) {
                            NetworkHandler.getInstance().sendUiStatisticReport(ReportsMaker.getUiStatisticReport(context));
                        }
                    } else if (context != null) {
                        getUpdateSchedule(context);
                    }
                }
            }, 0, 60 * 1000);
        }
        context = ctx;
        getUpdateSchedule(ctx);
        return instance;
    }

    /** check if the device is currently connected to an internet service such as WiFi and GSM
     *
     * @return true is connected, false otherwise
     */
    public static boolean isNetworkConnected(){
        if(context != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Service.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        }
        return false;
    }

    /** Send multiple locations data to the server
     *
     * @param trackedLocations list of objects to be sent
     * @return true if object was sent and received, false otherwise
     */
    public int sendLocations(List<TrackedLocation> trackedLocations){
        String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());

        if(trackedLocations != null){
            List<ParseObject> sendList = new ArrayList<>();
            for(TrackedLocation trackedLocation : trackedLocations){
                ParseObject testObject = new ParseObject("LocationTracking");
                testObject.put(USERID_KEY, mThisDeviceUUID);
                testObject.put(LONGITUDE_KEY, trackedLocation.longitude);
                testObject.put(LATITUDE_KEY, trackedLocation.latitude);
                testObject.put(TIMESTAMP_KEY, trackedLocation.timestamp);
                testObject.put(TIME_KEY, Utils.convertTimestampToDateString(trackedLocation.timestamp));

                sendList.add(testObject);

                long sizeInBytes = getObjectSizeInBytes(sendList);

                if(sizeInBytes >= 128000){
                    sendList.remove(sendList.size()-1);
                    break;
                }
            }

            ParseObject.saveAllInBackground(sendList);
            return sendList.size();
        }
        return -1;
    }

    /** Send single location data to the server
     *
     * @param trackedLocation the object to be sent
     * @return true if object was sent and received, false otherwise
     */
    public boolean sendLocation(TrackedLocation trackedLocation){
        String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());

        if(trackedLocation != null){
            ParseObject testObject = new ParseObject("LocationTracking");
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(LONGITUDE_KEY, trackedLocation.longitude);
            testObject.put(LATITUDE_KEY, trackedLocation.latitude);
            testObject.put(TIMESTAMP_KEY, trackedLocation.timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(trackedLocation.timestamp));
            testObject.saveInBackground();
            return true;
        }
        return false;
    }

    /** send an event report to the server, even though input is JSONObject you should use
     * ReportsMaker class to create a properly formatted report before using this method
     *
     * @param report json object retreived from ReportsMaker class
     * @return true if the report was sent and received, false otherwise
     */
    public boolean sendEventReport(JSONObject report){

        sendPinnedReports();

        if(report != null && isEligableForSending){
            try {
                //Convert to parse object
                ParseObject testObject = new ParseObject(report.getString(ReportsMaker.EVENT_TAG_KEY));
                Iterator<?> keys = report.keys();
                while(keys.hasNext()) {
                    String key = (String)keys.next();
                    if(report.get(key) != null){
                        if(report.get(key) instanceof Object[]){
                            try {
                                String[] str = (String[]) report.get(key);
                                testObject.put(key, Arrays.asList(str));
                            } catch (ClassCastException e){}
                        } else {
                            testObject.put(key, report.get(key));
                        }
                    }
                }
                //this will make sure the report is saved into local cache until sent to parse
                if(NetworkHandler.getInstance() != null && NetworkHandler.getInstance().isNetworkConnected() && isEligableForSending){
                    testObject.saveInBackground();
                } else {
                    testObject.pinInBackground(testObject.getClassName());
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /** send a ui event report to the server, even though input is JSONObject you should use
     * ReportsMaker class to create a properly formatted report before using this method.
     * this method will also clear the current data from the ui report after succesful sending
     *
     * @param report json object retreived from ReportsMaker class
     * @return true if the report was sent and received, false otherwise
     */
    public boolean sendUiStatisticReport(JSONObject report){

        final long availableReportTimestamp = report.optLong(ReportsMaker.TIMESTAMP_KEY, 0);

        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(availableReportTimestamp);

        Calendar c2 = Calendar.getInstance();

        if(report != null && isEligableForSending && c1.get(Calendar.DAY_OF_MONTH) != c2.get(Calendar.DAY_OF_MONTH)){
            try {
                //Convert to parse object
                ParseObject testObject = new ParseObject(report.getString(ReportsMaker.EVENT_TAG_KEY));
                Iterator<?> keys = report.keys();
                while(keys.hasNext()) {
                    String key = (String)keys.next();
                    if(report.get(key) != null){
                        if(report.get(key) instanceof Object[]){
                            try {
                                String[] str = (String[]) report.get(key);
                                testObject.put(key, Arrays.asList(str));
                            } catch (ClassCastException e){}
                        } else {
                            testObject.put(key, report.get(key));
                        }
                    }
                }
                //this will make sure the report is saved into local cache until sent to parse
                if(NetworkHandler.getInstance() != null && NetworkHandler.getInstance().isNetworkConnected() && isEligableForSending && context != null){
                    testObject.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            ReportsMaker.clearUiStatistic(context);
                        }
                    });
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /** Run over the local parse datastore and tries to send the items in it, cleaning any item which has been sent */
    private void sendPinnedReports(){
        if(!isEligableForSending || isSendingPinnedInBackground) return;

        isSendingPinnedInBackground = true;
        String[] querytypes = {ReportsMaker.LogEvent.event_tag.MESSAGE,
                ReportsMaker.LogEvent.event_tag.NETWORK,
                ReportsMaker.LogEvent.event_tag.SOCIAL_GRAPH,
                ReportsMaker.LogEvent.event_tag.UI};

        for(String querytype : querytypes){
            ParseQuery<ParseObject> query = ParseQuery.getQuery(querytype);
            final String queryType = querytype;
            query.fromLocalDatastore().findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(final List<ParseObject> list, ParseException e) {
                    if(list != null && e == null && isNetworkConnected()) {
                        //if is the last type to be sent mark the sending as successful
                        if(queryType.equals(ReportsMaker.LogEvent.event_tag.UI)) isSendingPinnedInBackground = false;
                        ParseObject.saveAllInBackground(list, new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                ParseObject.unpinAllInBackground(list);
                            }
                        });
                    } else if(e != null){
                        isSendingPinnedInBackground = false;
                    }
                }
            });
        }
    }

    private long getObjectSizeInBytes(Object obj) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(obj);
            long bytesRead = b.toByteArray().length;
            o.close();
            b.close();
            return bytesRead;
        } catch (IOException e){
            e.printStackTrace();
            return 0;
        }
    }

    private static void getUpdateSchedule(Context context){
        SharedPreferences prefs = context.getSharedPreferences("schedule", Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("schedule", new HashSet<String>());

        if(set.isEmpty()){
            updateSchedule = new int[]{-1};
        } else {
            updateSchedule = new int[set.size()];

            int i =0;
            for(String s: set){
                updateSchedule[i] = Integer.valueOf(s);
                i++;
            }
        }
    }
}
