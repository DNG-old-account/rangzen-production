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

    /** Convert a timestemp in milliseconds into a human readable string format
     *
     * @param milli time in milliseconds to be converted
     * @return human readable string representation of the time stamp using UTC timezone
     * and formatted as yyyy-MM-dd HH:mm:ss
     */
    public static String convertTimestampToDateString(long milli){
        Date date = new Date(milli);
        SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sourceFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sourceFormat.format(date);
    }

    /** convert numeric value (assumed to represent dp size units) into px units
     * based on the device's screen density.
     * @param dp value in dp to be converted
     * @param context the context to get device settings with
     * @return value in px after conversion from dp
     */
    public static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int px = Math.round(dp * density);
        return px;
    }

    /** convert numeric value (assumed to represent px size units) into dp units
     * based on the device's screen density.
     * @param px value in dp to be converted
     * @param context the context to get device settings with
     * @return value in dp after conversion from px
     */
    public static int pxToDp(int px, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int dp = Math.round(px / density);
        return dp;
    }
}
