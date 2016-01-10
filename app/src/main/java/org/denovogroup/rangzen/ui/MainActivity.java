package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Liran on 12/27/2015.
 *
 * The main activity used by the app for most actions and navigational purposes
 */
public class MainActivity extends AppCompatActivity implements DrawerActivityHelper, FeedFragment.FeedFragmentCallbacks{

    public static final String TAG = "MainActivity";

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
            startActivityForResult(intent,FeedFragment.REQ_CODE_MESSAGE);
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

            switch(v.getId()){
                case R.id.drawer_menu_share_app:
                case R.id.drawer_menu_export_feed:
                case R.id.drawer_menu_offline_mode:
                case R.id.drawer_menu_reset:
                    // dont change activated state
                    break;
                default:
                    int childcount = drawerMenu.getChildCount();
                    for(int i=0; i<childcount;i++){
                        View child = drawerMenu.getChildAt(i);
                        child.setActivated(child == v);
                    }
                    break;
            }

            Fragment frag = null;

            switch (v.getId()){
                case R.id.drawer_menu_contact:
                    frag = new ContactFragment();
                    break;
                case R.id.drawer_menu_export_feed:
                    exportFeed();
                    break;
                case R.id.drawer_menu_feed:
                    frag = new FeedFragment();
                    break;
                case R.id.drawer_menu_info:
                    //TODO
                    break;
                case R.id.drawer_menu_profile:
                    frag = new ProfileFragment();
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
                    frag = new SettingsFragment();
                    break;
                case R.id.drawer_menu_share_app:
                    //TODO
                    break;
                case R.id.drawer_menu_starred:
                    frag = new StarredFragment();
                    break;
            }

            if(frag != null &&
                    getSupportFragmentManager().findFragmentById(contentHolder.getId()).getClass() != frag.getClass()) {

                drawerLayout.closeDrawers();

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
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentById(contentHolder.getId());
        if(fragment != null && fragment instanceof ContactFragment){
            fragment.onActivityResult(requestCode,resultCode,data);
        }
    }

    public Toolbar getToolbar(){
        return toolbar;
    }

    @Override
    public ActionBarDrawerToggle getDrawerToggle(){
        return drawerToggle;
    }

    @Override
    public void onFeedItemExpand(String messageId) {
        Fragment fragment = new ExpandedMessageFragment();
        Bundle args = new Bundle();
        args.putString(ExpandedMessageFragment.MESSAGE_ID_KEY ,messageId);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(contentHolder.getId(), fragment, null).addToBackStack(null).commit();
    }

    private void exportFeed(){

        AsyncTask<Void, Void, Boolean> exportTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                Cursor cursor = MessageStore.getInstance(MainActivity.this).getMessagesCursor(false, false,1000);
                cursor.moveToFirst();

                int messageColIndex = cursor.getColumnIndex(MessageStore.COL_MESSAGE);
                int timestampColIndex = cursor.getColumnIndex(MessageStore.COL_TIMESTAMP);
                int trustColIndex = cursor.getColumnIndex(MessageStore.COL_TRUST);
                int likesColIndex = cursor.getColumnIndex(MessageStore.COL_LIKES);
                int pseudoColIndex = cursor.getColumnIndex(MessageStore.COL_PSEUDONYM);

                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+File.separator+"Rangzen exports");
                if(!dir.exists()){
                    dir.mkdirs();
                }

                File file = new File(dir, "export"+Utils.convertTimestampToDateStringCompact(false, System.currentTimeMillis())+".csv");

                try {
                    FileWriter fos = new FileWriter(file);
                    BufferedWriter bos = new BufferedWriter(fos);

                    SecurityProfile profile = SecurityManager.getCurrentProfile(MainActivity.this);

                    final String newLine = System.getProperty("line.separator");

                    String colTitlesLine = ""
                            + (profile.isTimestamp() ? "\""+MessageStore.COL_TIMESTAMP+ "\"," : "")
                            + "\""+MessageStore.COL_TRUST+"\","
                            + "\""+MessageStore.COL_LIKES+"\","
                            + (profile.isPseudonyms() ? "\""+MessageStore.COL_PSEUDONYM +"\"," : "")
                            + "\"" + MessageStore.COL_MESSAGE+"\"";

                    bos.write(colTitlesLine);
                    bos.write(newLine);

                    while(!cursor.isAfterLast()){
                        String line = ""
                                + (profile.isTimestamp() ? "\""+(Utils.convertTimestampToDateStringCompact(false,cursor.getLong(timestampColIndex))+ "\",") : "")
                                + "\""+(cursor.getFloat(trustColIndex)*100)+"\","
                                + "\""+(cursor.getInt(likesColIndex))+"\","
                                + (profile.isPseudonyms() ? "\""+(cursor.getString(pseudoColIndex)) +"\"," : "")
                                + "\"" + formatMessageForCSV(cursor.getString(messageColIndex))+"\"";
                        bos.write(line);
                        bos.write(newLine); //due to a bug in windows notepad text will be displayed as a long string instead of multiline, this is a note-pad specific problem
                        cursor.moveToNext();
                    }
                    cursor.close();
                    bos.flush();
                    bos.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            AlertDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.export);
                builder.setMessage(R.string.export_message);
                builder.setCancelable(false);
                ProgressBar bar = new ProgressBar(MainActivity.this);
                builder.setView(bar);
                dialog = builder.show();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if(dialog != null) dialog.dismiss();
                Toast.makeText(MainActivity.this, aBoolean ? getString(R.string.export_successful) : getString(R.string.export_failed), Toast.LENGTH_LONG).show();
            }
        };

        exportTask.execute();
    }

    private String formatMessageForCSV(String rawString){
        String csvString = rawString.replaceAll("[\r\n\"]", " ");
        return csvString;
    }
}