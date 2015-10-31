package org.denovogroup.rangzen.beta;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.ParseInstallation;

import org.denovogroup.rangzen.backend.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Liran on 9/1/2015.
 *
 * This class is intended for making database compatible json objects for the various event and statistic
 * reports available, note that this class does not handle the sending of the data, only the formatting
 * of it.
 *
 * for sending the data use the NetworkHandler class
 */
public class ReportsMaker {

    public static final String USERID_KEY = "Userid";
    public static final String TIMESTAMP_KEY = "Timestamp";
    public static final String TIME_KEY = "Time";
    public static final String EVENT_TAG_KEY = "Tag";
    public static final String EVENT_ACTION_KEY = "Action";
    public static final String EVENT_MESSAGE_KEY = "Message";
    public static final String EVENT_DEVICES_COUNT = "Devices_count";
    public static final String EVENT_DEVICES_IDS = "Devices_Ids";
    public static final String EVENT_CONNECTION_START_READABLE_KEY = "Readable_Connection_start";
    public static final String EVENT_CONNECTION_START_KEY = "Connection_start";
    public static final String EVENT_CONNECTION_DURATION_KEY = "Connection_duration";
    public static final String EVENT_CONNECTION_FINISH_KEY = "Connection_finish";
    public static final String EVENT_EXCHANGED_KEY = "Exchanged_count";
    public static final String EVENT_SUCCESSFUL_KEY = "Successful_count";
    public static final String EVENT_FAILED_KEY = "Failed_count";
    public static final String EVENT_ERRORS_KEY = "Errors";
    public static final String EVENT_MESSAGES_BEFORE_KEY = "MSG_Count_before";
    public static final String EVENT_MESSAGES_AFTER_KEY = "MSG_Count_after";

    public static class LogEvent{
        public static class event_tag{
            public static final String SOCIAL_GRAPH = "SOCIAL_GRAPH";
            public static final String MESSAGE = "MESSAGE";
            public static final String NETWORK = "NETWORK";
            public static final String UI = "UI";
        }

        public static class event_action{
            public static class Message{
                static final String EXCHANGE = "EXCHAGE";
                static final String REWEETED = "REWEETED";
                static final String POSTED = "POSTED";
                static final String USER_PRIORITY = "USER_PRIORITY";
                static final String DELETED = "DELETED";
                static final String SYSTEM_PRIORITY = "SYSTEM_PRIORITY";
            }

            public static class Network{
                static final String NETWORK_STATE = "NETWORK_STATE";
                static final String ERROR = "ERROR";
                static final String FOUND_DEVICE = "FOUND_DEVICE";
                static final String CONNECTED_DEVICE = "CONNECTED_DEVICE";
            }
        }
    }

    public static final String EVENT_PRIORITY_KEY = "Priority";
    public static final String EVENT_OLD_PRIORITY_KEY = "OldPriority";
    public static final String EVENT_SENDER_KEY = "Sender";
    public static final String EVENT_RECEIVER_KEY = "Receiver";
    public static final String EVENT_MESSAGE_ID_KEY = "Message_id";
    public static final String EVENT_MUTUAL_FRIENDS_KEY = "Mutual_friends";
    public static final String EVENT_NETWORK_TYPE_KEY = "Network_type";
    public static final String EVENT_NETWORK_STATE_KEY = "Network_state";
    public static final String EVENT_EXCHANGE_TIMES_KEY = "ExchangeTimes";

    public static final String AGGREGATE_FILE_NAME = "Saved_statistics";
    public static final String AGGREGATE_SEARCHES_KEY = "Searchs";
    public static final String AGGREGATE_HASHTAGS_KEY = "Hashtags";
    public static final String AGGREGATE_FRIENDS_VIA_QR_KEY = "Friends_via_QR";
    public static final String AGGREGATE_FRIENDS_VIA_BOOK_KEY = "Friends_via_Book";
    public static final String AGGREGATE_NOTIFICATIONS_KEY = "Notifications";
    public static final String AGGREGATE_TEXTS_KEY= "Texts";
    public static final String AGGREGATE_LOCATIONS_KEY = "Locations";
    public static final String AGGREGATE_PICTURES_KEY = "Pictures";

    private static Map<String,JSONObject> pendingReports = new HashMap<String, JSONObject>();


    /** add a report to backlog for later use, does not send the report
     *
     * @param report the report to add
     * @return the report id required to retrieve the report from the backlog
     */
    public static String prepReport(JSONObject report){
        Random r = new Random();
        String reportId = System.currentTimeMillis()+""+ r.nextInt(1000);
        pendingReports.put(reportId, report);
        return reportId;
    }

    /**retrieve a report from the backlog if exists
     * @param reportId the id of the report to return
     * @return
     */
    public static JSONObject getBacklogedReport(String reportId){
        return pendingReports.get(reportId);
    }

    /** change a property for a backloged report
     *
     * @param reportId the backlog id to edit
     * @param values a map of values to edit
     */
    public static void editReport(String reportId, Map<String,Object> values){
        JSONObject report = pendingReports.get(reportId);
        if(report != null) {
            for (Map.Entry entry : values.entrySet()) {
                try {
                    report.put((String) entry.getKey(), entry.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** remove a report from the backlog */
    public static void removeReport(String reportId){
        pendingReports.remove(reportId);
    }

    public static JSONObject getReceivedFriendsReport(long timestamp){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.SOCIAL_GRAPH);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getMessageExchangeReport(long timestamp, String sender, String receiver, String message,String messageId, double priority, float mutualFriends, String exchangeTimes){
        try{
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.EXCHANGE);
            testObject.put(EVENT_PRIORITY_KEY, priority);
            testObject.put(EVENT_SENDER_KEY, sender);
            testObject.put(EVENT_RECEIVER_KEY, receiver);
            testObject.put(EVENT_MESSAGE_KEY, message);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MUTUAL_FRIENDS_KEY, mutualFriends);
            testObject.put(EVENT_EXCHANGE_TIMES_KEY, exchangeTimes);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getMessageReweetedReport(long timestamp, String messageId, double priority, String message){
        try{
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.REWEETED);
            testObject.put(EVENT_PRIORITY_KEY, priority);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MESSAGE_KEY, message);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getMessagePostedReport(long timestamp, String messageId, double priority, String message){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.POSTED);
            testObject.put(EVENT_PRIORITY_KEY, priority);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MESSAGE_KEY, message);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getMessagePriorityChangedByUserReport(long timestamp, String messageId, double oldPriority, double newPriority, String message){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.USER_PRIORITY);
            testObject.put(EVENT_OLD_PRIORITY_KEY, oldPriority);
            testObject.put(EVENT_PRIORITY_KEY, newPriority);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MESSAGE_KEY, message);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getMessageDeletedReport(long timestamp, String messageId, double priority, String message){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.DELETED);
            testObject.put(EVENT_PRIORITY_KEY, priority);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MESSAGE_KEY, message);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getMessagePriorityChangedBySystemReport(long timestamp, String messageId, double oldPriority, double newPriority, String message){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.SYSTEM_PRIORITY);
            testObject.put(EVENT_OLD_PRIORITY_KEY, oldPriority);
            testObject.put(EVENT_PRIORITY_KEY, newPriority);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MESSAGE_KEY, message);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getNetworkStateChangedReport(long timestamp, String networkType, boolean isOn){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.NETWORK);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Network.NETWORK_STATE);
            testObject.put(EVENT_NETWORK_TYPE_KEY, networkType);
            testObject.put(EVENT_NETWORK_STATE_KEY, isOn ? "on" : "off");
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getNetworkErrorReport(long timestamp, String networkType, String message){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.NETWORK);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Network.ERROR);
            testObject.put(EVENT_NETWORK_TYPE_KEY, networkType);
            testObject.put(EVENT_MESSAGE_KEY, message);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getDiscoveredDeviceReport(long timestamp, String[] devicesIds){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.NETWORK);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Network.FOUND_DEVICE);
            testObject.put(EVENT_DEVICES_COUNT, (devicesIds != null) ? devicesIds.length : 0);
            testObject.put(EVENT_DEVICES_IDS, devicesIds);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getConnectedDeviceReport(long start, long finish, int successful, int failed, String errors, int msgBefore, int msgAfter){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.NETWORK);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Network.CONNECTED_DEVICE);
            testObject.put(EVENT_CONNECTION_START_READABLE_KEY, Utils.convertTimestampToDateString(start , null));
            testObject.put(EVENT_CONNECTION_START_KEY, start);
            testObject.put(EVENT_CONNECTION_FINISH_KEY, finish);
            testObject.put(EVENT_CONNECTION_DURATION_KEY, finish-start);
            testObject.put(EVENT_EXCHANGED_KEY, successful+failed);
            testObject.put(EVENT_SUCCESSFUL_KEY, successful);
            testObject.put(EVENT_FAILED_KEY, failed);
            testObject.put(EVENT_ERRORS_KEY, (errors!= null) ? errors : "");
            testObject.put(EVENT_MESSAGES_BEFORE_KEY, msgBefore);
            testObject.put(EVENT_MESSAGES_AFTER_KEY, msgAfter);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** return a ui statistic report that can be saved to the server
     *  normally you would rather use updateUiStatistic and getUiStatisticReport instead of this method.
     */
    private static JSONObject getUiEventReport(long timestamp, int searches, int hashtags, int addedFriendsViaQR, int addedFriendsViaBook, int texts, int locations, int pictures){
        try {
            String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, mThisDeviceUUID);
            testObject.put(TIMESTAMP_KEY, timestamp);
            testObject.put(TIME_KEY, Utils.convertTimestampToDateString(timestamp, null));
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.UI);
            testObject.put(AGGREGATE_SEARCHES_KEY, searches);
            testObject.put(AGGREGATE_HASHTAGS_KEY, hashtags);
            testObject.put(AGGREGATE_FRIENDS_VIA_QR_KEY, addedFriendsViaQR);
            testObject.put(AGGREGATE_FRIENDS_VIA_BOOK_KEY, addedFriendsViaBook);
            testObject.put(AGGREGATE_NOTIFICATIONS_KEY, texts + locations + pictures);
            testObject.put(AGGREGATE_TEXTS_KEY, texts);
            testObject.put(AGGREGATE_LOCATIONS_KEY, locations);
            testObject.put(AGGREGATE_PICTURES_KEY, pictures);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** adds the data supplied to the current track to be later retrieved using getUiStatisticReport.
     *
     * @param context
     * @param timestamp
     * @param searches amount of search events to add
     * @param hashtags amount of hashtags used to add
     * @param addedFriendsViaQR amount of friends added via barcode scanning
     * @param addedFriendsViaBook amount of friends added by using the address book
     * @param texts amount of text messages sent
     * @param locations amount of location messages sent
     * @param pictures amount of picture messages sent
     */
    public static void updateUiStatistic(Context context, long timestamp, int searches, int hashtags, int addedFriendsViaQR, int addedFriendsViaBook, int texts, int locations, int pictures){
        SharedPreferences pref = context.getSharedPreferences(AGGREGATE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if(pref.getLong(TIMESTAMP_KEY,-1) < timestamp){
            editor.putLong(TIMESTAMP_KEY, timestamp);
        }
        editor.putInt(AGGREGATE_SEARCHES_KEY, searches + pref.getInt(AGGREGATE_SEARCHES_KEY, 0));
        editor.putInt(AGGREGATE_HASHTAGS_KEY, hashtags + pref.getInt(AGGREGATE_HASHTAGS_KEY, 0));
        editor.putInt(AGGREGATE_FRIENDS_VIA_QR_KEY, addedFriendsViaQR + pref.getInt(AGGREGATE_FRIENDS_VIA_QR_KEY, 0));
        editor.putInt(AGGREGATE_FRIENDS_VIA_BOOK_KEY, addedFriendsViaBook + pref.getInt(AGGREGATE_FRIENDS_VIA_BOOK_KEY, 0));
        editor.putInt(AGGREGATE_TEXTS_KEY, texts + pref.getInt(AGGREGATE_TEXTS_KEY, 0));
        editor.putInt(AGGREGATE_LOCATIONS_KEY, locations + pref.getInt(AGGREGATE_LOCATIONS_KEY, 0));
        editor.putInt(AGGREGATE_PICTURES_KEY, pictures + pref.getInt(AGGREGATE_PICTURES_KEY, 0));
        editor.commit();
    }

    /** create a formatted report for sending from the current locally saved statistic file. */
    public static JSONObject getUiStatisticReport(Context context){
        SharedPreferences pref = context.getSharedPreferences(AGGREGATE_FILE_NAME, Context.MODE_PRIVATE);
        return getUiEventReport(pref.getLong(TIMESTAMP_KEY, 0),
                pref.getInt(AGGREGATE_SEARCHES_KEY, 0),
                pref.getInt(AGGREGATE_HASHTAGS_KEY, 0),
                pref.getInt(AGGREGATE_FRIENDS_VIA_QR_KEY, 0),
                pref.getInt(AGGREGATE_FRIENDS_VIA_BOOK_KEY, 0),
                pref.getInt(AGGREGATE_TEXTS_KEY, 0),
                pref.getInt(AGGREGATE_LOCATIONS_KEY, 0),
                pref.getInt(AGGREGATE_PICTURES_KEY, 0));
    }

    public static void clearUiStatistic(Context context){
        SharedPreferences pref = context.getSharedPreferences(AGGREGATE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.commit();
    }
}
