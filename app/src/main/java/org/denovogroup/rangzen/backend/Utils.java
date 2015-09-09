package org.denovogroup.rangzen.backend;

import android.content.Context;
import android.util.DisplayMetrics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Liran on 9/2/2015.
 */
public class Utils {

    public static String convertTimestampToDateString(long milli){
        Date date = new Date(milli);
        SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sourceFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sourceFormat.format(date);
    }

    public static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int px = Math.round(dp * density);
        return px;
    }

    public static int pxToDp(int px, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int dp = Math.round(px / density);
        return dp;
    }
}
