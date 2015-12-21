package org.denovogroup.rangzen.ui;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by Liran on 9/1/2015.
 */
public class RangzenApplication extends Application {

    private static String TAG = "RangzenApplication";
    private static  Context context;

    @Override
    public final void onCreate() {
        super.onCreate();
        context = this;
    }

    /**
     * This is part of the multidexing support for pre-lolipop devices
     * it may removed once multidexing no longer reuqired
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static Context getContext(){
        return context;
    }
}
