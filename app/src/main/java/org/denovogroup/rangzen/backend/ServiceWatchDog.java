package org.denovogroup.rangzen.backend;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.apache.log4j.Logger;
import org.denovogroup.rangzen.ui.MainActivity;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by Liran on 1/22/2016.
 *
 * A class used to monitor the health state of the RangzenService and restart it if it cannot
 * function anymore
 */
public class ServiceWatchDog {

    Timer timer;

    private WeakReference<RangzenService> serviceWeakReference;

    private Date lastExchange;

    private final static String TAG = "ServiceWatchDog";
    private static final Logger log = Logger.getLogger(TAG);

    private static ServiceWatchDog instance;

    private static final int WAIT_BEFORE_RESTART = 5000;
    private static final long SUSSPECIOUS_TIME_BETWEEN_EXCHANGES = TimeUnit.MINUTES.toMillis(20);

    public static ServiceWatchDog getInstance(){
        if(instance == null){
            instance = new ServiceWatchDog();
        }
        return instance;
    }

    private ServiceWatchDog() {
    }

    public void init(RangzenService service){
        serviceWeakReference = new WeakReference<>(service);
    }

    public void notifyLastExchange(){

        if(timer != null){
            timer.cancel();
        }
        else{
            timer = new Timer();
        }
        lastExchange = new Date();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    restartService();
                } catch (Exception e){}
            }
        }, SUSSPECIOUS_TIME_BETWEEN_EXCHANGES);
    }

    /** notify big error to the watchdog so it may help recover */
    public void notifyError(Exception e){
        restartService();
    }


    private void restartService(){
        log.debug("attempting recovery");

        if(serviceWeakReference == null){
            log.error("Watchdog service reference not been initialized");
            return;
        }

        RangzenService service = serviceWeakReference.get();

        if(service == null){
            log.error("Watchdog cannot recover service, service reference is null");
            return;
        }

        if(!service.getSharedPreferences(MainActivity.PREF_FILE, Context.MODE_PRIVATE).getBoolean(MainActivity.IS_APP_ENABLED, true)){
            service.stopSelf();
            return;
        }

        AlarmManager alarmManager = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);

        Intent restartIntent = new Intent(service.getApplicationContext(), RangzenService.class);
        PendingIntent pendingRestart = PendingIntent.getService(service.getApplicationContext(), -1, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + WAIT_BEFORE_RESTART, pendingRestart);

        log.debug("restarting service");
        service.stopSelf();
    }

    /** notify the WatchDog that controlled service stop had occurred to prevent timed checkups
     * from happening
     */
    public void notifyServiceDestroy(){
        if(timer != null) timer.cancel();
        serviceWeakReference = null;
    }
}
