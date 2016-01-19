package org.denovogroup.rangzen.ui;

import android.app.Application;
import android.content.Context;

import org.denovogroup.rangzen.backend.ConfigureLog4J;
import org.denovogroup.rangzen.backend.Log4JExceptionHandler;

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

        ConfigureLog4J.configure(false);
        Thread.setDefaultUncaughtExceptionHandler(new Log4JExceptionHandler(Thread.getDefaultUncaughtExceptionHandler()));

    }

    public static Context getContext(){
        return context;
    }
}
