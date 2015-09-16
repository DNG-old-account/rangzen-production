package org.denovogroup.rangzen.beta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import org.json.JSONObject;

/**
 * Created by Liran on 9/8/2015.
 */
public class WifiStateReceiver extends BroadcastReceiver {

    public WifiStateReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);

        boolean isEnabled = (state == WifiManager.WIFI_STATE_ENABLED) ? true : false;
        JSONObject report = ReportsMaker.getNetworkStateChangedReport(System.currentTimeMillis(), "WIFI", isEnabled);
        NetworkHandler.getInstance(context).sendEventReport(report);
    }
}
