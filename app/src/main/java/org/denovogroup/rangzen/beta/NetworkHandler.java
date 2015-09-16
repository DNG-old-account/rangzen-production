package org.denovogroup.rangzen.beta;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;

import org.denovogroup.rangzen.backend.Utils;
import org.denovogroup.rangzen.beta.locationtracking.TrackedLocation;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
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

    public static NetworkHandler getInstance(){
        if(instance == null || context == null){
            return null;
        }
        return instance;
    }

    public static NetworkHandler getInstance(Context ctx){
        if(instance == null){
            instance = new NetworkHandler();
        }
        context = ctx;
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

    /** Send location data to the server
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
            try {
                testObject.save();
                return true;
            } catch (ParseException e) {
                e.printStackTrace();
            }
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

        if(report != null){
            try {
                //Convert to parse object
                ParseObject testObject = new ParseObject(report.getString(ReportsMaker.EVENT_TAG_KEY));
                Iterator<?> keys = report.keys();
                while(keys.hasNext()) {
                    String key = (String)keys.next();
                    if(report.get(key) != null){
                        if(report.get(key) instanceof Object[]){
                            try {
                                String[] strarr = (String[]) report.get(key);
                                testObject.put(key, Arrays.asList(strarr));
                            } catch (ClassCastException e){}
                        } else {
                            testObject.put(key, report.get(key));
                        }
                    }
                }
                //this will make sure the report is saved into local cache until sent to parse
                testObject.saveEventually();
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
