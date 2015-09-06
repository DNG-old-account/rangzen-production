package org.denovogroup.rangzen.backend;

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
}
