package org.denovogroup.rangzen.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by Liran on 9/16/2015.
 *
 * This class mantain basic reference to an unread messages until read
 */
public class ReadStateTracker {

    private static final String SAVED_UNREAD_LIST = "unreadMessages";
    private static HashMap<String,Boolean> unreadMessages;

    /** setup the required objects of the tracker for any read/write operation done later
     *
     * @param context
     */
    public static void initTracker(Context context){
        unreadMessages = new HashMap<>();
        SharedPreferences preferences = context.getSharedPreferences(SAVED_UNREAD_LIST, Context.MODE_PRIVATE);
        unreadMessages = (HashMap<String, Boolean>) preferences.getAll();
    }

    /** write the local storage and also change the current reference to the supplied message
     * and set its read mode to either read on unread.
     *
     * @param context
     * @param message the message to edit
     * @param isRead true for read false for unread
     */
    public static void setReadState(Context context,String message, boolean isRead){
        SharedPreferences preferences = context.getSharedPreferences(SAVED_UNREAD_LIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if(unreadMessages == null){
            initTracker(context);
        }

        if(isRead) {
            editor.remove(message);
            if(unreadMessages.containsKey(message)){
                unreadMessages.remove(message);
            }
        } else {
            editor.putBoolean(message, false);
            unreadMessages.put(message, false);
        }
        editor.apply();
    }

    /** check if the supplied message is read or not.
     *
     * @param message the message to check for
     * @return false if the message is not read, true if read or
     * tracker not initialized first.
     */
    public static boolean isRead(String message){
        if(unreadMessages != null){
            return !unreadMessages.containsKey(message);
        }
        return true;
    }

    /** reset the local storage to make all the messages marked as read
     *
     * @param context
     */
    public static void markAllAsRead(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SAVED_UNREAD_LIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();

        if(unreadMessages != null){
            unreadMessages.clear();
        }
    }

    /** return how many items are currently marked as unread
     * @param context
     * @return the number of items marked as unread
     */
    public static int getUnreadCount(Context context){

        if(unreadMessages == null){
            initTracker(context);
        }

        return unreadMessages.size();
    }

    public static HashMap<String,Boolean> getAllUnreadMessages(Context context){
        if(unreadMessages == null){
            initTracker(context);
        }

        return (HashMap<String, Boolean>) unreadMessages.clone();
    }
}
