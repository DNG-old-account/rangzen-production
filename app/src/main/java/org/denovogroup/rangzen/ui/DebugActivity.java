package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.FriendStore;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.beta.locationtracking.TrackingService;

import java.util.Set;

/**
 * Created by Liran on 9/12/2015.
 * This class will hold various debug specific data which will only be visible during production.
 * During beta run this activity should'nt be reachable
 */
public class DebugActivity extends ActionBarActivity {

    private TextView myIdTv;
    private TextView myFriendsTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.debug_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        myIdTv = (TextView) findViewById(R.id.user_id);
        myFriendsTv = (TextView) findViewById(R.id.friends_id);

        myIdTv.setText("" + getMyId());
        myFriendsTv.setText(""+getMyFriendsIds());
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

        return myId;
    }

    public String getMyFriendsIds() {
        String friends = "";
        FriendStore store = new FriendStore(this, StorageBase.ENCRYPTION_DEFAULT);
        Set<String> friendsSet = store.getAllFriends();
        for(String str : friendsSet){
            friends += str+"\n";
        }

        return friends;
    }
}
