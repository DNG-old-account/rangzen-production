package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.SaveCallback;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.FriendStore;
import org.denovogroup.rangzen.backend.Peer;
import org.denovogroup.rangzen.backend.PeerManager;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.backend.Utils;
import org.denovogroup.rangzen.beta.NetworkHandler;
import org.denovogroup.rangzen.beta.ReportsMaker;
import org.denovogroup.rangzen.beta.locationtracking.LocationCacheHandler;
import org.denovogroup.rangzen.beta.locationtracking.TrackingService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Liran on 9/12/2015.
 * This class will hold various debug specific data which will only be visible during production.
 * During beta run this activity should'nt be reachable
 */
public class DebugActivity extends ActionBarActivity {

    public static final String PREF_FILE = "debug_prefs";
    public static final String IS_UNRESTRICTED_KEY = "IsUnrestricted";
    public static final String TRACK_LOCATION_KEY = "TrackLocation";

    private static final int ID_LENGTH = 8;

    private TextView appVersionTv;
    private TextView myIdTv;
    private TextView myFriendsTv;
    private TextView updateHoursTv;
    private TextView connectionsTv;
    private TextView pendingUpdatesTv;
    private TextView locationHistoryTv;
    private Timer refreshTimer;
    private CheckBox trackLocation;
    private CheckBox unrestrictedNetwork;

    private int pending_locations = 0;
    private int pending_messages = 0;
    private int pending_network = 0;
    private int pending_graph = 0;
    private int pending_ui = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.debug_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        appVersionTv = (TextView) findViewById(R.id.app_ver);
        myIdTv = (TextView) findViewById(R.id.user_id);
        myFriendsTv = (TextView) findViewById(R.id.friends_id);
        connectionsTv = (TextView) findViewById(R.id.connections);
        updateHoursTv = (TextView) findViewById(R.id.hours);
        pendingUpdatesTv = (TextView) findViewById(R.id.updates);
        locationHistoryTv = (TextView) findViewById(R.id.location_history);
        trackLocation = (CheckBox) findViewById(R.id.trackLocation);
        unrestrictedNetwork = (CheckBox) findViewById(R.id.unrestrictedNetwork);

        boolean isTracking = getSharedPreferences(PREF_FILE, MODE_PRIVATE).getBoolean(TRACK_LOCATION_KEY, true);
        trackLocation.setChecked(isTracking);
        trackLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(TRACK_LOCATION_KEY, isChecked);
                editor.commit();

                if(isChecked){
                    //start tracking service
                    Intent trackingServiceIntent = new Intent(DebugActivity.this, TrackingService.class);
                    DebugActivity.this.startService(trackingServiceIntent);
                } else {
                    //turn off tracking service if running
                    Intent trackingServiceIntent = new Intent(DebugActivity.this, TrackingService.class);
                    DebugActivity.this.stopService(trackingServiceIntent);
                }
            }
        });

        boolean isUnrestricted  = getSharedPreferences(PREF_FILE, MODE_PRIVATE).getBoolean(IS_UNRESTRICTED_KEY, false);
        unrestrictedNetwork.setChecked(isUnrestricted);
        unrestrictedNetwork.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(IS_UNRESTRICTED_KEY, isChecked);
                editor.commit();

                NetworkHandler.getInstance(DebugActivity.this);
            }
        });


        String version = "0.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(),0).versionName +" ("+getPackageManager().getPackageInfo(getPackageName(),0).versionCode+")";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        appVersionTv.setText(version);
        myIdTv.setText("" + getMyId());
        myFriendsTv.setText("" + getMyFriendsIds());

        List<Peer> peers = PeerManager.getInstance(this).getPeers();
        String connections = "";

        if(peers != null){
            for(Peer peer : peers){
                connections += peer.toString()+"\n";
            }
        }

        connectionsTv.setText(connections);
        updateHoursTv.setText(""+getUpdateHours());

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLocationHistory();
            }
        });
    }

    public String getMyId() {
        FriendStore store = new FriendStore(this, StorageBase.ENCRYPTION_DEFAULT);
        String myId = store.getPublicDeviceIDString();

        return myId.substring(myId.length()-1-ID_LENGTH);
    }

    public String getMyFriendsIds() {
        String friends = "";
        FriendStore store = new FriendStore(this, StorageBase.ENCRYPTION_DEFAULT);
        Set<String> friendsSet = store.getAllFriends();

        if(friendsSet.isEmpty()) {
            friends = "You dont have any friends yet :(";
        } else {
            for (String str : friendsSet) {
                friends += str.substring(str.length() - 1 - ID_LENGTH) + "\n";
            }
        }

        return friends;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<Peer> peers = PeerManager.getInstance(DebugActivity.this).getPeers();
                String connections = "";

                if (peers == null || peers.isEmpty()) {
                    connections = "No connected peers yet";
                } else {
                    for (Peer peer : peers) {
                        connections += peer.toString() + "\n";
                    }
                }
                final String finalConnections = connections;

                final String pendingUpdates = getPendingUpdates();

                final String locationHistory = getLocationHistory();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectionsTv.setText(finalConnections);
                        updateHoursTv.setText("" + getUpdateHours());
                        pendingUpdatesTv.setText(pendingUpdates);
                        locationHistoryTv.setText(locationHistory);
                    }
                });
            }
        }, 1000, 1000);
    }

    public String getUpdateHours() {
        String updateSchedule;
        SharedPreferences prefs = this.getSharedPreferences("schedule", Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("schedule", new HashSet<String>());

        if(set.isEmpty()){
            updateSchedule = "Never";
        } else {
            updateSchedule = "";
            for(String s: set){
                updateSchedule += s+",";
            }
        }

        return updateSchedule;
    }

    public String getPendingUpdates() {

        String result = "";

        Cursor locations = LocationCacheHandler.getInstance(this).getLocations();

        pending_locations = locations.getCount();
        if(pending_locations > 0){
            result += "Locations: "+pending_locations;
            locations.moveToLast();
            result +=" (from: "+ Utils.convertTimestampToDateString(locations.getLong(locations.getColumnIndex(LocationCacheHandler.TIMESTAMP_COL)), TimeZone.getDefault().getID());
            locations.moveToFirst();
            result +=" to: "+ Utils.convertTimestampToDateString(locations.getLong(locations.getColumnIndex(LocationCacheHandler.TIMESTAMP_COL)), TimeZone.getDefault().getID()) + ")\n";
        }

        pending_messages = 0;
        pending_network = 0;
        pending_graph = 0;
        pending_ui = 0;

        final String[] querytypes = {ReportsMaker.LogEvent.event_tag.MESSAGE,
                ReportsMaker.LogEvent.event_tag.NETWORK,
                ReportsMaker.LogEvent.event_tag.SOCIAL_GRAPH,
                ReportsMaker.LogEvent.event_tag.UI};

        for(String querytype : querytypes){
            ParseQuery<ParseObject> query = ParseQuery.getQuery(querytype);
            final String queryType = querytype;
            try {
                List<ParseObject> list = query.fromLocalDatastore().find();
                if(list != null) {

                    int dirtyCount = 0;
                    for(ParseObject obj : list){
                        if(obj.isDirty()) dirtyCount++;
                    }

                    if(queryType.equals(ReportsMaker.LogEvent.event_tag.MESSAGE)){
                        pending_messages = dirtyCount;
                        if(pending_messages > 0) result += "Messages: "+pending_messages+"\n";
                    } else if(queryType.equals(ReportsMaker.LogEvent.event_tag.NETWORK)) {
                        pending_network = dirtyCount;
                        if(pending_network > 0) result += "Network: "+pending_network+"\n";
                    } else if(queryType.equals(ReportsMaker.LogEvent.event_tag.SOCIAL_GRAPH)) {
                        pending_graph = dirtyCount;
                        if(pending_graph > 0) result += "Social graph: "+pending_graph+"\n";
                    } else if(queryType.equals(ReportsMaker.LogEvent.event_tag.UI)) {
                        pending_ui = dirtyCount;
                        if(pending_ui > 0) result += "UI: "+pending_ui+"\n";
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        result = "Total: "+(pending_graph+pending_messages+pending_network+pending_locations+pending_ui)+"\n"+result;

        return result;
    }

    @Override
    protected void onPause(){
        super.onPause();
        refreshTimer.cancel();
    }

    private String getLocationHistory(){

        int second = 1000;
        int minute = 60 * second;
        int hour = 60 * minute;

        String log = "";

        SharedPreferences history = getSharedPreferences(TrackingService.HISTORY_FILE_NAME,Context.MODE_PRIVATE);
        if(history != null && !history.getAll().isEmpty()){
            Map<String, Integer> items = (Map<String, Integer>) history.getAll();

            for(Map.Entry entry : items.entrySet()){
                String date = (String)entry.getKey();
                try {
                    JSONObject data = new JSONObject((String)entry.getValue());
                    Calendar calFrom = Calendar.getInstance();
                    calFrom.setTimeInMillis(data.optLong(TrackingService.HISTORY_FROM_KEY,0));
                    calFrom.set(Calendar.MILLISECOND, 0);

                    Calendar calTo = Calendar.getInstance();
                    calTo.setTimeInMillis(data.optLong(TrackingService.HISTORY_TO_KEY, 0));
                    calTo.set(Calendar.MILLISECOND, 0);


                    long duration = calTo.getTimeInMillis() - calFrom.getTimeInMillis();

                    int hours = (int) Math.floor(duration/hour);
                    int minutes = (int) Math.floor((duration-hours*hour)/minute);
                    int seconds = (int) Math.floor((duration-hours*hour-minutes*minute)/second);

                    int sampled = data.getInt(TrackingService.HISTORY_SAMPLED_KEY);
                    int discarded = data.getInt(TrackingService.HISTORY_DISCARDED_KEY);

                    String logEntry = date+"\n"
                                        +"Total sampled: "+sampled+"\n"
                                        +"Discarded: "+discarded+"\n"
                                        +"Time collected(hh:mm:ss): "+hours+":"+minutes+":"+seconds+"\n";

                    log = log.concat(logEntry+"\n");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            log = "history is empty";
        }

        return log;
    }

    private void clearLocationHistory(){
        SharedPreferences history = getSharedPreferences("location_history",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = history.edit();
        editor.clear();
        editor.commit();
    }
}