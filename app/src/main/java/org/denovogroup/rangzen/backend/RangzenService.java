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
package org.denovogroup.rangzen.backend;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.app.Service;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.objects.RangzenMessage;
import org.denovogroup.rangzen.ui.MainActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.System;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * Core service of the Rangzen app. Started at startup, remains alive
 * indefinitely to perform the background tasks of Rangzen.
 */
public class RangzenService extends Service {
    /** The running instance of RangzenService. */
    protected static RangzenService sRangzenServiceInstance;

    /** For app-local broadcast and broadcast reception. */
    private LocalBroadcastManager mLocalBroadcastManager;

    /** Executes the background thread periodically. */
    private ScheduledExecutorService mScheduleTaskExecutor;

    /** Cancellable scheduling of backgroundTasks. */
    private ScheduledFuture mBackgroundExecution;

    /** Cancellable scheduling of cleanup. */
    private ScheduledFuture mCleanupExecution;

    /** Handle to app's PeerManager. */
    private PeerManager mPeerManager;

    /** The time at which this instance of the service was started. */
    private Date mStartTime;

    /** Random number generator for picking random peers. */
    private Random mRandom = new Random();

    /** The number of times that backgroundTasks() has been called. */
    private int mBackgroundTaskRunCount = 0;

    /** Handle to Rangzen key-value storage provider. */
    private StorageBase mStore;

    /** Storage for friends. */
    private FriendStore mFriendStore;

    /** Wifi Direct Speaker used for Wifi Direct name based RSVP. */
    private WifiDirectSpeaker mWifiDirectSpeaker;

    /** Whether we're attempting a connection to another device over BT. */
    private boolean connecting = false;

    /** The BluetoothSpeaker for the app. */
    private static BluetoothSpeaker mBluetoothSpeaker;

    /** Message store. */
    private MessageStore mMessageStore; 
    /** Ongoing exchange. */
    private Exchange mExchange;

    /** Socket over which the ongoing exchange is taking place. */
    private BluetoothSocket mSocket;

    /** When announcing our address over Wifi Direct name, prefix this string to our MAC. */
    public final static String RSVP_PREFIX = "RANGZEN-";

    /** Key into storage to store the last time we had an exchange. */
    private static final String LAST_EXCHANGE_TIME_KEY = 
                            "org.denovogroup.rangzen.LAST_EXCHANGE_TIME_KEY";

    /** Time to wait between exchanges, in milliseconds. */
    public static int TIME_BETWEEN_EXCHANGES_MILLIS;

    /** Android Log Tag. */
    private final static String TAG = "RangzenService";

    private static final int NOTIFICATION_ID = R.string.unread_notification_title;
    private static final int RENAME_DELAY = 1000;
    private static final String DUMMY_MAC_ADDRESS = "02:00:00:00:00:00";
    private static final int BACKOFF_FOR_ATTEMPT_MILLIS = 10 * 1000;
    private static final int BACKOFF_MAX = BACKOFF_FOR_ATTEMPT_MILLIS * (int)Math.pow(2,9);

    private static final boolean USE_MINIMAL_LOGGING = true;

    /**
     * Called whenever the service is requested to start. If the service is
     * already running, this does /not/ create a new instance of the service.
     * Rather, onStartCommand is called again on the existing instance.
     * 
     * @param intent
     *            The intent passed to startService to start this service.
     * @param flags
     *            Flags about the request to start the service.
     * @param startid
     *            A unique integer representing this request to start.
     * @see android.app.Service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.i(TAG, "RangzenService onStartCommand.");

        // Returning START_STICKY causes Android to leave the service running
        // even when the foreground activity is closed.
        return START_STICKY;
    }

    /**
     * Called the first time the service is started.
     * 
     * @see android.app.Service
     */
    @Override
    public void onCreate() {
        Log.i(TAG, "RangzenService created.");

        sRangzenServiceInstance = this;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mPeerManager = PeerManager.getInstance(this);
        mBluetoothSpeaker = new BluetoothSpeaker(this, mPeerManager);
        mPeerManager.setBluetoothSpeaker(mBluetoothSpeaker);

        mStartTime = new Date();

        mStore = new StorageBase(this, StorageBase.ENCRYPTION_DEFAULT);
        mFriendStore = FriendStore.getInstance(this);

        mWifiDirectSpeaker = WifiDirectSpeaker.getInstance();
        mWifiDirectSpeaker.init(this,
                mPeerManager,
                mBluetoothSpeaker,
                new WifiDirectFrameworkGetter());

        mMessageStore = MessageStore.getInstance(this);

        setWifiDirectFriendlyName();
        mWifiDirectSpeaker.setmSeekingDesired(true);

        // Schedule the background task thread to run occasionally.
        mScheduleTaskExecutor = Executors.newScheduledThreadPool(1);
        // TODO(lerner): Decide if 1 second is an appropriate time interval for
        // the tasks.
        mBackgroundExecution = mScheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                backgroundTasks();
            }
        }, 0, 1, TimeUnit.SECONDS);

        mCleanupExecution = mScheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                cleanupMessageStore();
            }
        }, 0, 5, TimeUnit.MINUTES);

        TIME_BETWEEN_EXCHANGES_MILLIS = SecurityManager.getCurrentProfile(this).getCooldown() * 1000;
    }

    /**
     * Called when the service is destroyed.
     */
    public void onDestroy() {
      mBackgroundExecution.cancel(true);
        SharedPreferences pref = getSharedPreferences(MainActivity.PREF_FILE, Context.MODE_PRIVATE);
        if(pref.contains(MainActivity.WIFI_NAME) && mWifiDirectSpeaker != null){
            Log.d(TAG, "Restoring wifi name");
            mWifiDirectSpeaker.setWifiDirectUserFriendlyName(pref.getString(MainActivity.WIFI_NAME, ""));
        }

        mWifiDirectSpeaker.dismissNoWifiNotification();
        mBluetoothSpeaker.dismissNoBluetoothNotification();
    }


    /**
     * Check whether we can connect, according to our policies.
     * Currently, checks that we've waited TIME_BETWEEN_EXCHANGES_MILLIS 
     * milliseconds since the last exchange and that we're not already connecting.
     *
     * @return Whether or not we're ready to connect to a peer.
     */
    private boolean readyToConnect() {
      long now = System.currentTimeMillis();
      long lastExchangeMillis = mStore.getLong(LAST_EXCHANGE_TIME_KEY, -1);

        boolean timeSinceLastOK;
      if (lastExchangeMillis == -1) {
        timeSinceLastOK = true;
      } else if (now - lastExchangeMillis < TIME_BETWEEN_EXCHANGES_MILLIS) {
        timeSinceLastOK = false;
      } else {
        timeSinceLastOK = true;
      }
        if(!USE_MINIMAL_LOGGING) {
            Log.v(TAG, "Ready to connect? " + (timeSinceLastOK && !getConnecting()));
            Log.v(TAG, "Connecting: " + getConnecting());
            Log.v(TAG, "timeSinceLastOK: " + timeSinceLastOK);
        }
      return timeSinceLastOK && !getConnecting(); 
    }

    /**
     * Set the time of the last exchange, kept in storage, to the current time.
     */
    private void setLastExchangeTime() {
        if(!USE_MINIMAL_LOGGING) Log.i(TAG, "Setting last exchange time");
      long now = System.currentTimeMillis();
      mStore.putLong(LAST_EXCHANGE_TIME_KEY, now);
    }

    /**
     * Method called periodically on a background thread to perform Rangzen's
     * background tasks.
     */
    public void backgroundTasks() {
        if(!USE_MINIMAL_LOGGING) Log.v(TAG, "Background Tasks Started");

        if(isAppInForeground()){
            cancelUnreadMessagesNotification();
        }
        // TODO(lerner): Why not just use mPeerManager?
        PeerManager peerManager = PeerManager.getInstance(getApplicationContext());
        peerManager.tasks();
        mBluetoothSpeaker.tasks();
        mWifiDirectSpeaker.tasks();

        List<Peer> peers = peerManager.getPeers();
        // TODO(lerner): Don't just connect all willy-nilly every time we have
        // an opportunity. Have some kind of policy for when to connect.
        if (peers.size() > 0 && readyToConnect() ) {
            if(SecurityManager.getCurrentProfile(this).isRandomExchange()) {
                Peer selectedPeer = peers.get(mRandom.nextInt(peers.size()));
                peers.clear();
                peers.add(selectedPeer);
            }
            for(Peer peer : peers) {
                try {
                    Log.d(TAG,"Checking peer:"+peer);
                    if (peerManager.thisDeviceSpeaksTo(peer)) {
                        // Connect to the peer, starting an exchange with the peer once
                        // connected. We only do this if thisDeviceSpeaksTo(peer), which
                        // checks whether we initiate conversations with this peer or
                        // it initiates with us (a function of our respective addresses).


                        //optimize connection using history tracker
                        ExchangeHistoryTracker.ExchangeHistoryItem historyItem = ExchangeHistoryTracker.getInstance().getHistoryItem(peer.address);
                        boolean hasHistory = historyItem != null;
                        boolean storeVersionChanged = false;
                        boolean waitedMuch = false;

                        if (hasHistory) {
                            storeVersionChanged = !historyItem.storeVersion.equals(MessageStore.getInstance(RangzenService.this).getStoreVersion());
                            waitedMuch = historyItem.lastExchangeTime + Math.min(
                                    Math.pow(2 , historyItem.attempts) * BACKOFF_FOR_ATTEMPT_MILLIS, BACKOFF_MAX) < System.currentTimeMillis();
                        }

                        if (!hasHistory || storeVersionChanged || waitedMuch) {
                            Log.d(TAG, "Can connect with peer: " + peer);
                            connectTo(peer);
                        } else {
                            Log.d(TAG, "Backoff from peer: " + peer +
                                    " [previously interacted:" + hasHistory + ", store ready:" + storeVersionChanged + " ,backoff timeout:" + waitedMuch + "]");
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "No such algorithm for hashing in thisDeviceSpeaksTo!? " + e);
                    return;
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Unsupported encoding exception in thisDeviceSpeaksTo!?" + e);
                    return;
                }
            }
        } else {
          Log.v(TAG, String.format("Not connecting (%d peers, ready to connect is %s)", peers.size(), readyToConnect()));
        }
        mBackgroundTaskRunCount++;

        // Log.v(TAG, "Background Tasks Finished");
    }

    /**
     * Connect to the peer via Bluetooth. Upon success, start an exchange with
     * the peer. If we're already connecting to someone, this method returns
     * without doing anything.
     *
     * @param peer The peer we want to talk to.
     */
    public void connectTo(Peer peer) {
      if (getConnecting()) {
        Log.w(TAG, "connectTo() not connecting to " + peer + " -- already connecting to someone");
        return;
      }

      Log.i(TAG, "connecting to " + peer);
      // TODO(lerner): Why not just use mPeerManager?
      PeerManager peerManager = PeerManager.getInstance(this);

      // This gets reset to false once an exchange is complete or when the 
      // connect call below fails. Until then, no more connections will be
      // attempted. (One at a time now!)
      setConnecting(true);

      Log.i(TAG, "Starting to connect to " + peer.toString());
      // The peer connection callback (defined elsewhere in the class) takes
      // the connect bluetooth socket and uses it to create a new Exchange.
      mBluetoothSpeaker.connect(peer, mPeerConnectionCallback);
    }

    /**
     * Handles connection to a peer by taking the connected bluetooth socket and
     * using it in an Exchange.
     */
    /*package*/ PeerConnectionCallback mPeerConnectionCallback = new PeerConnectionCallback() {
      @Override
      public void success(BluetoothSocket socket) {
        Log.i(TAG, "Callback says we're connected to " + socket.getRemoteDevice().toString());
        if (socket.isConnected()) {
          mSocket = socket;
          Log.i(TAG, "Socket connected, attempting exchange");
          try {
            mExchange = new CryptographicExchange(
                    RangzenService.this,
                    socket.getRemoteDevice().getAddress(),
                socket.getInputStream(),
                socket.getOutputStream(),
                true,
                FriendStore.getInstance(RangzenService.this),
                MessageStore.getInstance(RangzenService.this),
                RangzenService.this.mExchangeCallback);
            (new Thread(mExchange)).start();
          } catch (IOException e) {
            Log.e(TAG, "Getting input/output stream from socket failed: " + e);
            Log.e(TAG, "Exchange not happening.");
            RangzenService.this.cleanupAfterExchange();
          }
        } else {
          Log.w(TAG, "But the socket claims not to be connected!");
          RangzenService.this.cleanupAfterExchange();
        }
      }
      @Override
      public void failure(String reason) {
        Log.i(TAG, "Callback says we failed to connect: " + reason);
        RangzenService.this.cleanupAfterExchange();
      }
    };

    /**
     * Cleans up sockets and connecting state after an exchange, including recording
     * that an exchange was just attempted, that we're no longer currently connecting,
     * closing sockets and setting socket variables to null, etc.
     *
     * Is also used after a Bluetooth connection failure to cleanup.
     */
    /* package */ void cleanupAfterExchange() {
      setConnecting(false);
      setLastExchangeTime();
      try {
        if (mSocket != null) {
          mSocket.close();
        }
      } catch (IOException e) {
        Log.w(TAG, "Couldn't close bt socket: " + e);
      }
      try { 
        if (mBluetoothSpeaker.mSocket != null) {
          mBluetoothSpeaker.mSocket.close();
        }
      } catch (IOException e) {
        Log.w(TAG, "Couldn't close bt socket in BTSpeaker: " + e);
      }
      mSocket = null;
      mBluetoothSpeaker.mSocket = null;
    }

    /**
     * Passed to an Exchange to be called back to when the exchange completes.
     * Performs the integration of the information received from the exchange -
     * adds new messages to the message store, weighting their priorities
     * based upon the friends in common.
     */
    /* package */ ExchangeCallback mExchangeCallback = new ExchangeCallback() {
      @Override
      public void success(Exchange exchange) {
          boolean hasNew = false;
        List<RangzenMessage> newMessages = exchange.getReceivedMessages();
        int friendOverlap = exchange.getCommonFriends();
        Log.i(TAG, "Got " + newMessages.size() + " messages in exchangeCallback");
        Log.i(TAG, "Got " + friendOverlap + " common friends in exchangeCallback");
          Set<String> myFriends = mFriendStore.getAllFriends();
        for (RangzenMessage message : newMessages) {
          double stored = mMessageStore.getTrust(message.text);
          double remote = message.trust;
          double newTrust = Exchange.newPriority(remote, stored, friendOverlap, myFriends.size());
          try {
            if (mMessageStore.containsOrRemoved(message.text)){
                //update existing message priority unless its marked as removed by user
                mMessageStore.updateTrust(message.text, newTrust, true);
            } else {
                hasNew = true;

                mMessageStore.addMessage(RangzenService.this, message.messageid, message.text, newTrust, message.priority, message.pseudonym, message.timestamp ,true, message.timebound, message.getLocation(), message.parent, false, SecurityManager.getCurrentProfile(RangzenService.this).getMinContactsForHop(), message.hop, exchange.toString(), message.bigparent);
                //mark this message as unread
                mMessageStore.setRead(message.text, false);
            }
          } catch (IllegalArgumentException e) {
            Log.e(TAG, String.format("Attempted to add/update message %s with trust (%f/%f)" +
                                    ", %d friends, %d friends in common",
                                    message.text, newTrust, message.priority,
                                    myFriends.size(), friendOverlap));
          }
        }

          if(hasNew){
              mMessageStore.updateStoreVersion();
              ExchangeHistoryTracker.getInstance().incrementExchangeCount();
              ExchangeHistoryTracker.getInstance().updateHistory(RangzenService.this, exchange.getPeerAddress());
              if(isAppInForeground()) {
                  Intent intent = new Intent();
                  intent.setAction(MessageStore.NEW_MESSAGE);
                  getApplicationContext().sendBroadcast(intent);
              } else {
                  showUnreadMessagesNotification();
              }
          } else if(ExchangeHistoryTracker.getInstance().getHistoryItem(exchange.getPeerAddress()) != null){
              // Has history, should increment the attempts counter
              ExchangeHistoryTracker.getInstance().updateAttemptsHistory(exchange.getPeerAddress());
              Log.d(TAG,"Exchange finished without receiving new messages, back-off timeout increased to:"+
                    Math.min(BACKOFF_MAX , Math.pow(2, ExchangeHistoryTracker.getInstance().getHistoryItem(exchange.getPeerAddress()).attempts) * BACKOFF_FOR_ATTEMPT_MILLIS));
          } else {
              // No history file, create one
              Log.d(TAG, "Exchange finished without receiving new messages from new peer, creating history track");
              ExchangeHistoryTracker.getInstance().updateHistory(RangzenService.this, exchange.getPeerAddress());
          }

        RangzenService.this.cleanupAfterExchange();
      }

      @Override
      public void failure(Exchange exchange, String reason) {
        Log.e(TAG, "Exchange failed, reason: " + reason);
        RangzenService.this.cleanupAfterExchange();
      }

        @Override
        public void recover(Exchange exchange, String reason) {
            Log.e(TAG, "Exchange failed but data can be recovered, reason: " + reason);
            boolean hasNew = false;
            List<RangzenMessage> newMessages = exchange.getReceivedMessages();
            int friendOverlap = Math.max(exchange.getCommonFriends(), 0);
            Log.i(TAG, "Got " + newMessages.size() + " messages in exchangeCallback");
            Log.i(TAG, "Got " + friendOverlap + " common friends in exchangeCallback");
            if(newMessages != null) {
                for (RangzenMessage message : newMessages) {
                    Set<String> myFriends = mFriendStore.getAllFriends();
                    double stored = mMessageStore.getTrust(message.text);
                    double remote = message.priority;
                    double newTrust = Exchange.newPriority(remote, stored, friendOverlap, myFriends.size());
                    try {
                        if (mMessageStore.containsOrRemoved(message.text)){
                            //update existing message priority unless its marked as removed by user
                            mMessageStore.updateTrust(message.text, newTrust, true);
                        } else {
                            hasNew = true;
                            mMessageStore.addMessage(RangzenService.this, message.messageid, message.text, newTrust, message.priority, message.pseudonym, message.timestamp ,true, message.timebound, message.getLocation(), message.parent, false, SecurityManager.getCurrentProfile(RangzenService.this).getMinContactsForHop(), message.hop, exchange.toString(), message.bigparent);
                            //mark this message as unread
                            mMessageStore.setRead(message.text, false);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, String.format("Attempted to add/update message %s with trust (%f/%f)" +
                                        ", %d friends, %d friends in common",
                                message.text, newTrust, message.priority,
                                myFriends.size(), friendOverlap));
                    }
                }
            }

            if(hasNew){
                mMessageStore.updateStoreVersion();
                ExchangeHistoryTracker.getInstance().updateHistory(RangzenService.this, exchange.getPeerAddress());
                if(isAppInForeground()) {
                    Intent intent = new Intent();
                    intent.setAction(MessageStore.NEW_MESSAGE);
                    getApplicationContext().sendBroadcast(intent);
                } else {
                    showUnreadMessagesNotification();
                }
            } else {
                ExchangeHistoryTracker.getInstance().updateAttemptsHistory(exchange.getPeerAddress());
            }

            RangzenService.this.cleanupAfterExchange();
        }
    };

    /**
     * Check whether any network connection (Wifi/Cell) is available according
     * to the OS's connectivity service.
     * 
     * @return True if any network connection seems to be available, false
     *         otherwise.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Return true if Bluetooth is turned on, false otherwise.
     * 
     * @return Whether Bluetooth is enabled.
     */
    private boolean isBluetoothOn() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return false;
        } else {
            return adapter.isEnabled();
        }
    }

    /**
     * Return the number of times that background tasks have been executed since
     * the service was started.
     * 
     * @return The number of times backgroundTasks() has been called.
     */
    public int getBackgroundTasksRunCount() {
        return mBackgroundTaskRunCount;
    }
  
    /**
     * Get the time at which this instance of the service was started.
     * 
     * @return A Date representing the time at which the service was started.
     */
    public Date getServiceStartTime() {
        return mStartTime;
    }

    /** Synchronized accessor for connecting. */
    private synchronized boolean getConnecting() {
      return connecting;
    }

    /** Synchronized setter for connecting. */
    private synchronized void setConnecting(boolean connecting) {
      this.connecting = connecting;
    }

    /**
     * This method has to be implemented on a service, but I haven't written the
     * service with binding in mind. Unsure what would happen if it were used
     * this way.
     * 
     * @param intent
     *            The intent used to bind the service (passed to
     *            Context.bindService(). Extras included in the intent will not
     *            be visible here.
     * @return A communication channel to the service. This implementation just
     *         returns null.
     * @see android.app.Service
     * 
     */
    @Override
    public IBinder onBind(Intent intent) {
        // This service is not meant to be used through binding.
        return null;
    }

    /** This method check how many unread messages there are and display a notification
     * visible from the recent task pull down menu. this method should be called only
     * be called when the app itself is either closed or in the background.
     */
    private void showUnreadMessagesNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this , 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(getString(R.string.unread_notification_title));
        builder.setContentText(MessageStore.getInstance(this).getUnreadCount() + " " + getString(R.string.unread_notification_content));

        // create large icon
        Resources res = this.getResources();
        BitmapDrawable largeIconDrawable;
        if(Build.VERSION.SDK_INT >= 21){
            largeIconDrawable = (BitmapDrawable) res.getDrawable(R.mipmap.ic_launcher, null);
        } else {
            largeIconDrawable = (BitmapDrawable) res.getDrawable(R.mipmap.ic_launcher);
        }
        Bitmap largeIcon = largeIconDrawable.getBitmap();

        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        largeIcon = Bitmap.createScaledBitmap(largeIcon, width, height, false);

        builder.setLargeIcon(largeIcon);
        builder.setAutoCancel(true);
        builder.setTicker(getText(R.string.unread_notification_content));
        builder.setSmallIcon(R.mipmap.blank_pixel);
        builder.setDefaults(Notification.DEFAULT_SOUND);

        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void cancelUnreadMessagesNotification(){
        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nManager.cancel(NOTIFICATION_ID);
    }

    /** Check if the app have a living instance in the foreground
     *
     * @return true if the app is active and in the foreground, false otherwise
     */
    public boolean isAppInForeground() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().contains("org.denovogroup.rangzen");
    }

    /** retrieve the bluetooth MAC address from the bluetooth speaker and set the WifiDirectSpeaker
     * friendly name accordingly.
     */
    private void setWifiDirectFriendlyName(){
        String btAddress = mBluetoothSpeaker.getAddress();
        if(mWifiDirectSpeaker != null) {

            SharedPreferences pref = getSharedPreferences(MainActivity.PREF_FILE, Context.MODE_PRIVATE);
            if(!pref.contains(MainActivity.WIFI_NAME)){
                String oldName = BluetoothAdapter.getDefaultAdapter().getName();
                pref.edit().putString(MainActivity.WIFI_NAME, oldName).commit();
            }

            if(Build.VERSION.SDK_INT >= 23){
                btAddress = SecurityManager.getStoredMAC(this);
            }

            mWifiDirectSpeaker.setWifiDirectUserFriendlyName(RSVP_PREFIX + btAddress);
            if (btAddress.equals(DUMMY_MAC_ADDRESS) || btAddress.equals("")) {
                Log.w(TAG, "Bluetooth speaker provided a dummy/blank bluetooth" +
                        " MAC address (" + btAddress + ") scheduling device name change.");
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setWifiDirectFriendlyName();
                    }
                }, RENAME_DELAY);
            }
        }
    }

    private void cleanupMessageStore(){
        SecurityProfile currentProfile = SecurityManager.getCurrentProfile(this);
            MessageStore.getInstance(this).deleteOutdatedOrIrrelevant(currentProfile);
    }
}
