package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.FriendStore;
import org.denovogroup.rangzen.backend.Peer;
import org.denovogroup.rangzen.backend.PeerManager;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.beta.locationtracking.TrackingService;

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

    private static final int ID_LENGTH = 8;

    private TextView appVersionTv;
    private TextView myIdTv;
    private TextView myFriendsTv;
    private TextView connectionsTv;
    private Timer connectionsTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.debug_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        appVersionTv = (TextView) findViewById(R.id.app_ver);
        myIdTv = (TextView) findViewById(R.id.user_id);
        myFriendsTv = (TextView) findViewById(R.id.friends_id);
        connectionsTv = (TextView) findViewById(R.id.connections);

        String version = "0.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
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
    }

    public void showServiceDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("During the beta Rangzen will need to monitor your location at all time, please keep the tracking service running until the end of the beta")
                .setTitle("Disclaimer");
        builder.setCancelable(false);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //start tracking service
                Intent trackingServiceIntent = new Intent(DebugActivity.this, TrackingService.class);
                DebugActivity.this.startService(trackingServiceIntent);
            }
        });
        builder.setNegativeButton("Dont track", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //turn off tracking service if running
                Intent trackingServiceIntent = new Intent(DebugActivity.this, TrackingService.class);
                DebugActivity.this.stopService(trackingServiceIntent);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
        for(String str : friendsSet){
            friends += str.substring(str.length()-1-ID_LENGTH)+"\n";
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

                if(peers != null){
                    for(Peer peer : peers){
                        connections += peer.toString()+"\n";
                    }
                }
                final String finalConnections = connections;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectionsTv.setText(finalConnections);
                    }
                });
            }
        }, 1000,1000);
    }

    @Override
    protected void onPause(){
        super.onPause();
        connectionsTimer.cancel();
    }
}
