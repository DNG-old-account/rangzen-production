package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.FriendStore;
import org.denovogroup.rangzen.backend.Peer;
import org.denovogroup.rangzen.backend.PeerManager;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.beta.NetworkHandler;
import org.denovogroup.rangzen.beta.locationtracking.TrackingService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private Timer connectionsTimer;
    private CheckBox trackLocation;
    private CheckBox unrestrictedNetwork;

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
        connectionsTimer = new Timer();
        connectionsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<Peer> peers = PeerManager.getInstance(DebugActivity.this).getPeers();
                String connections = "";

                if(peers == null || peers.isEmpty()) {
                    connections = "No connected peers yet";
                } else {
                    for(Peer peer : peers){
                        connections += peer.toString()+"\n";
                    }
                }
                final String finalConnections = connections;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectionsTv.setText(finalConnections);
                        updateHoursTv.setText(""+getUpdateHours());
                    }
                });
            }
        }, 1000,1000);
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

    @Override
    protected void onPause(){
        super.onPause();
        connectionsTimer.cancel();
    }
}
