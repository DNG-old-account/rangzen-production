package org.denovogroup.rangzen.backend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Liran on 1/5/2016.
 */
public class WifiDirectReceiver extends BroadcastReceiver {

    public WifiDirectReceiver() {
        //default constructor so it may be declared statically in the manifest for better chance of intercepting
        //the transmiation
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiDirectSpeaker.getInstance().onReceive(context, intent);
    }
}
