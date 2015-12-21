package org.denovogroup.rangzen.ui;

import android.app.Application;
import android.content.Context;

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

    public static Context getContext(){
        return context;
    }
}
