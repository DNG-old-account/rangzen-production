/*
 * Copyright (c) 2014, De Novo Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.denovogroup.rangzen.ui;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;
import org.denovogroup.rangzen.ui.FragmentOrganizer.FragmentType;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class is the manager of all of the fragments that are clickable in the
 * sidebar. The sidebar itself is also programmed in this class and pulls new
 * activities or switches main fragment views.
 */
public class Opener extends ActionBarActivity implements OnItemClickListener {

    private DrawerLayout mDrawerLayout;
    private ListView mListView;
    private ActionBarDrawerToggle mDrawerListener;
    private SidebarListAdapter mSidebarAdapter;
    private Button newMessagesNotification;
    private static TextView mCurrentTextView;
    private static boolean mHasStored = false;
    private static boolean mFirstTime = true;
    private static final String TAG = "Opener";
    private static final int MAX_NEW_MESSAGES_DISPLAY = 99;
    private Menu menu;
    private MenuItem pendingNewMessagesMenuItem;

    // Create reciever object
    private BroadcastReceiver receiver = new MessageEventReceiver();

    // Set When broadcast event will fire.
    private IntentFilter filter = new IntentFilter(MessageStore.NEW_MESSAGE);

    private final static int Message = 20;
    private final static int Search = 30;

    /** Initialize the contents of the activities menu. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        this.menu = menu;

        pendingNewMessagesMenuItem = menu.findItem(R.id.new_post);

        //Setup the search view
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        setSearhView(searchView);
        menu.findItem(R.id.advanced).setVisible(false);

        //get any hashtag passed data from previous click events
        Uri data = getIntent().getData();
        getIntent().setData(null);
        if(data != null) searchHashTagFromClick(data, searchItem);
        return true;
    }

    /**
     * This method initializes the listView of the drawer layout and the layout
     * itself.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Intent.ACTION_SEND.equals(getIntent().getAction()) && "text/plain".equals(getIntent().getType())){
            Intent intent = new Intent(this, PostActivity.class);
            intent.putExtra(PostActivity.MESSAGE_BODY, getIntent().getStringExtra(Intent.EXTRA_TEXT));
            startActivityForResult(intent,Message);

        }

        setContentView(R.layout.drawer_layout);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        // activityRootView = drawerLayout;
        mListView = (ListView) findViewById(R.id.drawerList);
        mSidebarAdapter = new SidebarListAdapter(this);
        mListView.setAdapter(mSidebarAdapter);
        mListView.setOnItemClickListener(this);
        mDrawerListener = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.open, R.string.close) {

            @Override
            public boolean onOptionsItemSelected(MenuItem item) {
                if(item.getItemId() == R.id.advanced){
                    Intent intent = new Intent(Opener.this, SearchActivity.class);
                    startActivityForResult(intent, Search);
                }
                return super.onOptionsItemSelected(item);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                InputMethodManager inputMethodManager = (InputMethodManager) getApplication()
                        .getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getWindow()
                        .getCurrentFocus().getWindowToken(), 0);
            }

        };
        newMessagesNotification = (Button) findViewById(R.id.new_post_notification);
        newMessagesNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyDataSetChanged(null);
                //mark all the messages as read
                MessageStore.getInstance(Opener.this).setAllAsRead();
                setPendingUnreadMessagesDisplay();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerListener);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);

        //start Rangzen service if necessary
        SharedPreferences pref = getSharedPreferences(PreferencesActivity.PREF_FILE, Context.MODE_PRIVATE);
        if(pref.getBoolean(KillswitchFragment.IS_APP_ENABLED, true)){
            Intent startServiceIntent = new Intent(this, RangzenService.class);
            startService(startServiceIntent);
        }
    }

    /**
     * This allows the icon that shows a DrawerLayout is present to retract when
     * the layout disappears.
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerListener.syncState();
        switchToFeed();
    }

    /**
     * Switches current open fragment to feed fragment. Call this method after
     * closing an activity.
     */
    public void switchToFeed() {
        Log.d("Opener", "Switching to feed fragment.");
        Fragment needAdd = new FeedFragment();
        Bundle b = new Bundle();
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.replace(R.id.mainContent, needAdd);

        ft.commitAllowingStateLoss();
        mFirstTime = false;
        selectItem(0);
    }

    /**
     * This lets the DrawerListener know that it will display itself when the
     * icon in the ActionBar is clicked.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerListener.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.new_post) {
            notifyDataSetChanged(null);
            //mark all the messages as read
            MessageStore.getInstance(this).setAllAsRead();
            setPendingUnreadMessagesDisplay();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This handles orientation changes.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerListener.onConfigurationChanged(newConfig);
    }

    /**
     * This displays the correct fragment at item position, and it also closes
     * the navigation drawer when an option has been chosen.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (position != 1) {
            selectItem(position);
        }
        showFragment(position);
        mDrawerLayout.closeDrawers();
    }

    /**
     * This highlights the option that was just chosen and makes the title of
     * the page the fragment that was chosen.
     */
    public void selectItem(int position) {
        mListView.setItemChecked(position, true);
        if (position != 2) {
            String[] sidebar = getResources().getStringArray(R.array.sidebar);
            setTitle(sidebar[position]);
        }
    }

    /**
     * Sets the title located in the ActionBar to be the title of the chosen
     * fragment.
     * 
     * @param title
     *            The title of the page the user has chosen to navigate to.
     */
    public void setTitle(String title) {
        if(getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }

    public void makeTitleBold(int position) {
        View view = mListView.getChildAt(position);
        if (mCurrentTextView == null) {
            mCurrentTextView = (TextView) view.findViewById(R.id.textView1);
        }
        mCurrentTextView.setTypeface(null, Typeface.NORMAL);
        mCurrentTextView.setTextSize(17);
        mCurrentTextView = (TextView) view.findViewById(R.id.textView1);

        mCurrentTextView.setTypeface(null, Typeface.BOLD);
        mCurrentTextView.setTextSize(19);
    }

    /**
     * Called whenever any activity launched from this activity exits. For
     * example, this is called when returning from the QR code activity,
     * providing us with the QR code (if any) that was scanned.
     *
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.i(TAG, "Got activity result back in Opener!");

        if(resultCode == RESULT_OK) {
            switch( requestCode){
                case Message:
                    notifyDataSetChanged(null);
                    break;
                case Search:
                    String extra = intent.getStringExtra(SearchActivity.SEARCH_EXTRA);

                    MenuItem menuItem = menu.findItem(R.id.search);
                    SearchView searchView = (SearchView) menuItem.getActionView();
                    menuItem.expandActionView();
                    searchView.setQuery(extra, true);
                    searchView.clearFocus();
                    break;
            }
        }
    }

    /**
     * This handles the instantiation of all of the fragments when they are
     * chosen. This also transforms the title of the item in the list to bold
     * and a larger font.
     * 
     * @param position
     *            The specific position in the ListView of the current fragment
     *            chosen.
     */
    public void showFragment(int position) {
        Fragment needAdd = null;
        switch(position){
            case 0:
                needAdd = new FeedFragment();
                break;
            case 1:
                Intent postIntent = new Intent();
                postIntent.setClass(this, PostActivity.class);
                startActivityForResult(postIntent, Message);
                return;
            case 2:
                Intent addFriendIntent = new Intent();
                addFriendIntent.setClass(this, PreferencesActivity.class);
                addFriendIntent.setAction(PreferencesActivity.ACTION_ADD_FRIEND);
                startActivity(addFriendIntent);
                return;
            case 3:
                Intent settingsIntent = new Intent(this, PreferencesActivity.class);
                startActivity(settingsIntent);
                return;
            default:
                needAdd = new FragmentOrganizer();
                Bundle b = new Bundle();
                b.putSerializable("whichScreen", FragmentType.SECONDABOUT);
                needAdd.setArguments(b);
                break;
        }
        makeTitleBold(position);
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft.replace(R.id.mainContent, needAdd);

        if (!mFirstTime) {
            Log.d("Opener", "added to backstack");
            ft.addToBackStack(null);
        }
        mFirstTime = false;
        ft.commit();
    }

    public SidebarListAdapter getSidebarAdapter() {
        return mSidebarAdapter;
    }

    /**
     * Overriden in order to unregister the receiver, if this is not done then
     * the app crashes.
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        Log.i(TAG, "Unregistered receiver");
    }

    /**
     * Creates a new instance of the MessageEventReceiver and registers it every
     * time that the app is available/brought to the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
        Log.i(TAG, "Registered receiver");

        if(!Utils.isBluetoothEnabled()){
            showNoBluetoothDialog();
        } else if(! Utils.isWifiEnabled(this)){
            showNoWifiDialog();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mark all the messages as read
        MessageStore.getInstance(this).setAllAsRead();
    }

    /**
     * This is the broadcast receiver object that I am registering. I created a
     * new class in order to override onReceive functionality.
     * 
     * @author jesus
     * 
     */
    public class MessageEventReceiver extends BroadcastReceiver {

        /**
         * When the receiver is activated then that means a message has been
         * added to the message store, (either by the user or by the active
         * services). The reason that the instanceof check is necessary is
         * because there are two possible routes of activity:
         * 
         * 1) The previous/current fragment viewed could have been the about
         * fragment, if it was then the focused fragment is not a
         * ListFragmentOrganizer and when the user returns to the feed then the
         * feed will check its own data set and not crash.
         * 
         * 2) The previous/current fragment is the feed, it needs to be notified
         * immediately that there was a change in the underlying dataset.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            setPendingUnreadMessagesDisplay();
        }
    }

    /**
     * Request currently displayed fragment to refresh its view if its utilizing the Refreshable interface
     * @param items List of objects to be used by the refreshed adapter or null to invoke default list of items
     */
    private void notifyDataSetChanged(Cursor items) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(
                R.id.mainContent);
        if (fragment instanceof Refreshable) {
            ((Refreshable) fragment).refreshView(items, false);
        }
    }

    /** create and display a dialog prompting the user about the enabled
     * state of the bluetooth service.
     */
    private void showNoBluetoothDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_bluetooth);
        builder.setTitle(R.string.dialog_no_bluetooth_title);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(!Utils.isWifiEnabled(Opener.this)){
                    showNoWifiDialog();
                }
            }
        });
        builder.setPositiveButton(R.string.turn_on, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                if(btAdapter != null){
                    btAdapter.enable();
                }
                dialog.dismiss();
                if(!Utils.isWifiEnabled(Opener.this)){
                    showNoWifiDialog();
                }
            }
        });
        builder.setMessage(R.string.dialog_no_bluetooth_message);
        builder.create();
        builder.show();
    }

    /** create and display a dialog prompting the user about the enabled
     * state of the wifi service.
     */
    private void showNoWifiDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_wifi);
        builder.setTitle(R.string.dialog_no_wifi_title);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.turn_on, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                if(wifiManager != null){
                    wifiManager.setWifiEnabled(true);
                }
                dialog.dismiss();
            }
        });
        builder.setMessage(R.string.dialog_no_wifi_message);
        builder.create();
        builder.show();
    }

    public void setSearhView(SearchView searchView){

        searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Fragment frag = getSupportFragmentManager().findFragmentById(R.id.mainContent);
                if (frag instanceof FeedFragment) {
                    ((FeedFragment) frag).setAddButtonVisible(!hasFocus);
                }
            }
        });

        //Define on close listener which support pre-honycomb devices as well with the app compat
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                menu.findItem(R.id.advanced).setVisible(true);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                //reset the list to its normal state
                menu.findItem(R.id.advanced).setVisible(false);
                notifyDataSetChanged(null);
            }
        });

        //Define the search procedure
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Fragment frag = getSupportFragmentManager().findFragmentById(R.id.mainContent);

                if (frag != null && frag instanceof FeedFragment) {
                    ((FeedFragment) frag).setQuery(query, false);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onQueryTextSubmit(newText);
                return false;
            }
        });
    }

    /** Set the the Actionbar search view with the hashtag supplied in the provided Uri and run the
     * default search method.
     *
     * @param data The Uri which contain the hashtag as the last part of the Uri
     * @param menuItem The menu item hosting the searchView to use
     */
    private void searchHashTagFromClick(Uri data, MenuItem menuItem){

        SearchView searchView = (SearchView) menuItem.getActionView();
        menuItem.expandActionView();
        String query = data.toString().substring(data.toString().indexOf("#"), data.toString().length()-1);
        searchView.setQuery(query, true);
        searchView.clearFocus();
    }

    /** set a notification at the actionbar letting the user know new unread messages are waiting,
     */
    private void setPendingUnreadMessagesDisplay(){
        long unreadCount = MessageStore.getInstance(Opener.this).getUnreadCount();
        if(pendingNewMessagesMenuItem != null){
            if(unreadCount > 0) {
                String countString = (unreadCount <= MAX_NEW_MESSAGES_DISPLAY) ? Long.toString(unreadCount) : "+"+MAX_NEW_MESSAGES_DISPLAY;
                /*pendingNewMessagesMenuItem.setTitle(countString + " " + getString(R.string.new_post));
                pendingNewMessagesMenuItem.setVisible(true);*/

                newMessagesNotification.setText(countString + " " + ((unreadCount > 1) ? getString(R.string.new_posts) : getString(R.string.new_post)));
                newMessagesNotification.setVisibility(View.VISIBLE);
            } else {
                newMessagesNotification.setVisibility(View.GONE);
                pendingNewMessagesMenuItem.setVisible(false);
            }

        }
    }
}
