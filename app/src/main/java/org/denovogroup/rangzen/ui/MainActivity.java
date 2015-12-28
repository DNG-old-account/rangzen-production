package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.uiold.Opener;
import org.denovogroup.rangzen.uiold.PostActivity;

/**
 * Created by Liran on 12/27/2015.
 *
 * The main activity used by the app for most actions and navigational purposes
 */
public class MainActivity extends AppCompatActivity implements DrawerActivityHelper{

    public static final String TAG = "MainActivity";
    public static final int REQ_CODE_MESSAGE = 100;
    public static final int REQ_CODE_SEARCH = 101;

    //setting key
    public static final String PREF_FILE = "settings";
    public static final String IS_APP_ENABLED = "isEnabled";

    int selectedDrawerItem = R.id.drawer_menu_feed;

    DrawerLayout drawerLayout;
    ViewGroup drawerMenu;
    ActionBarDrawerToggle drawerToggle;
    View contentHolder;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        toolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setTitleTextColor(Color.WHITE);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        initDrawerMenu();

        contentHolder = findViewById(R.id.mainContent);

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction().replace(contentHolder.getId(), new FeedFragment()).commit();

            View selectedItem = drawerMenu.findViewById(selectedDrawerItem);
            if(selectedItem != null) selectedItem.setActivated(true);
        }

        if(Intent.ACTION_SEND.equals(getIntent().getAction()) && "text/plain".equals(getIntent().getType())){
            Intent intent = new Intent(this, PostActivity.class);
            intent.putExtra(PostActivity.MESSAGE_BODY, getIntent().getStringExtra(Intent.EXTRA_TEXT));
            startActivityForResult(intent,REQ_CODE_MESSAGE);
        }

        //start Rangzen service if necessary
        SharedPreferences pref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        if(pref.getBoolean(IS_APP_ENABLED, true)){
            Intent startServiceIntent = new Intent(this, RangzenService.class);
            startService(startServiceIntent);
        }
    }

    private void initDrawerMenu(){
        if(drawerLayout == null) return;
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open,R.string.close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                View selectedItem = findViewById(selectedDrawerItem);
                if(selectedItem != null) selectedItem.setActivated(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };

        drawerMenu = (ViewGroup) findViewById(R.id.drawer_menu);

        int childcount = drawerMenu.getChildCount();
        for(int i=0; i<childcount; i++){
            View v = drawerMenu.getChildAt(i);
            if(v instanceof TextView) v.setOnClickListener(drawerMenuClickListener);
        }

        SharedPreferences pref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        ((Switch)drawerMenu.findViewById(R.id.drawer_menu_offline_mode)).setChecked(pref.getBoolean(IS_APP_ENABLED, true));

        ((Switch)drawerMenu.findViewById(R.id.drawer_menu_offline_mode)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences pref = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
                pref.edit().putBoolean(IS_APP_ENABLED, isChecked).commit();

                Intent serviceIntent = new Intent(MainActivity.this, RangzenService.class);
                if (isChecked) {
                    startService(serviceIntent);
                } else {
                    stopService(serviceIntent);
                }
            }
        });
    }

    /** A click listener to handle all drawer menu items clicks */
    private OnClickListener drawerMenuClickListener = new OnClickListener(){
        @Override
        public void onClick(View v) {
            int childcount = drawerMenu.getChildCount();
            for(int i=0; i<childcount;i++){
                View child = drawerMenu.getChildAt(i);
                child.setActivated(child == v);
            }

            Fragment frag = null;

            switch (v.getId()){
                case R.id.drawer_menu_contact:
                    //TODO
                    break;
                case R.id.drawer_menu_export_feed:
                    //TODO
                    break;
                case R.id.drawer_menu_feed:
                    frag = new FeedFragment();
                    break;
                case R.id.drawer_menu_info:
                    //TODO
                    break;
                case R.id.drawer_menu_profile:
                    //TODO
                    break;
                case R.id.drawer_menu_reset:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setIcon(R.drawable.ic_dialog_alert_holo_light)
                            .setTitle(R.string.reset_dialog_title)
                            .setMessage(R.string.reset_app_message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this)
                                            .setIcon(R.drawable.ic_dialog_alert_holo_light)
                                            .setTitle(R.string.confirm_reset_dialog_title)
                                            .setMessage(R.string.confirm_reset_app_message)
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //reset app
                                                    MessageStore.getInstance(MainActivity.this).purgeStore();
                                                    FriendStore.getInstance(MainActivity.this).purgeStore();
                                                    org.denovogroup.rangzen.backend.SecurityManager.getInstance().clearProfileData(MainActivity.this);

                                                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(intent);
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    builder2.show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                    builder.show();
                    break;
                case R.id.drawer_menu_settings:
                    //TODO
                    break;
                case R.id.drawer_menu_share_app:
                    //TODO
                    break;
                case R.id.drawer_menu_starred:
                    //TODO
                    break;
            }

            if(frag != null &&
                    getSupportFragmentManager().findFragmentById(contentHolder.getId()).getClass() != frag.getClass()) {

                Spinner spinner = (Spinner) toolbar.findViewById(R.id.sortSpinner);
                if(spinner != null) spinner.setVisibility(View.INVISIBLE);
                getSupportFragmentManager().beginTransaction().replace(contentHolder.getId(), frag).commit();
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            //open or close drawer menu
            return true;
        }

        // Handle your other action bar items
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Got activity result");

        if(resultCode == RESULT_OK){
            switch(requestCode){
                case REQ_CODE_MESSAGE:
                    //TODO
                    break;
                case REQ_CODE_SEARCH:
                    //TODO
                    break;
            }
        }
    }

    public Toolbar getToolbar(){
        return toolbar;
    }

    @Override
    public ActionBarDrawerToggle getDrawerToggle(){
        return drawerToggle;
    }
}
