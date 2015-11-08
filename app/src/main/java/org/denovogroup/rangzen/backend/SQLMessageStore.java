package org.denovogroup.rangzen.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Liran on 11/8/2015.
 */
public class SQLMessageStore extends SQLiteOpenHelper{

    private static SQLMessageStore instance;
    private static final String TAG = "SQLMessageStore";
    //readable true/false operators since SQLite does not support boolean values
    private static final int TRUE = 1;
    private static final int FALSE = 0;

    //messages properties
    private static final double MIN_PRIORITY_VALUE = 0.01f;
    private static final double MAX_PRIORITY_VALUE = 1.0f;
    private static final int MAX_MESSAGE_SIZE = 140;

    private static final String DATABASE_NAME = "MessageStore.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE = "Messages";
    private static final String COL_ROWID = "_id";
    private static final String COL_MESSAGE = "message";
    private static final String COL_PRIORITY = "priority";
    private static final String COL_DELETED = "deleted";

    /** Get the current instance of MessageStore and create one if necessary.
     * Implemented as a singleton */
    public synchronized static SQLMessageStore getInstance(Context context){
        if(instance == null){
            instance = new SQLMessageStore(context);
        }
        return instance;
    }

    /** private constructor for forcing singleton pattern for MessageStore */
    private SQLMessageStore(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        instance = this;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE + " ("
                +COL_ROWID+" INTEGER PRIMARY KEY AUTOINCREMENT,"
                +COL_MESSAGE+" VARCHAR("+MAX_MESSAGE_SIZE+") NOT NULL,"
                +COL_PRIORITY+" REAL NOT NULL DEFAULT "+MIN_PRIORITY_VALUE+","
                +COL_DELETED+" BOOLEAN DEFAULT "+FALSE+" NOT NULL CHECK("+COL_DELETED+" IN("+TRUE+","+FALSE+"))"
                +");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*recreate table on upgrade, this should be better implemented once final data base structure
          is reached*/
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Check that the given priority value is in range.
     * @throws IllegalArgumentException if value is outside of range
     */
    private static void checkPriority(double priority) throws IllegalArgumentException{
        if (priority < MIN_PRIORITY_VALUE || priority > MAX_PRIORITY_VALUE) {
            throw new IllegalArgumentException("Priority " + priority
                    + " is outside valid range of ["+MIN_PRIORITY_VALUE+","+MAX_PRIORITY_VALUE+"]");
        }
    }

    /**
     * Check check that the given priority value is in range and return nearest limit if not.
     */
    private static double streamlinePriority(double priority){
        if (priority < MIN_PRIORITY_VALUE){
            return MIN_PRIORITY_VALUE;

        } else if(priority > MAX_PRIORITY_VALUE) {
            return MAX_PRIORITY_VALUE;
        }else{
            return priority;
        }
    }

    /** convert cursor data returned from SQL queries into Message objects that can be returned to
     * query supplier. This implementation does not close the supplied cursor when done
     * @param cursor Cursor data returned from SQLite database
     * @return array of Message items contained by the cursor or null if cursor is empty
     */
    private Message[] convertToMessages(Cursor cursor){
        if(cursor.getCount() > 0){
            Message[] messages = new Message[cursor.getCount()];
            cursor.moveToFirst();

            int priorityColIndex = cursor.getColumnIndex(COL_PRIORITY);
            int messageColIndex = cursor.getColumnIndex(COL_MESSAGE);

            for(int i=0; i<messages.length; i++){
                messages[i] = new Message(
                        cursor.getDouble(priorityColIndex),
                        cursor.getString(messageColIndex)
                );
                cursor.moveToNext();
            }
            return messages;
        }
        return null;
    }

    /** Return an array of messages sorted by according their priority and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return Array of Message items based on database items matching conditions
     */
    public Message[] getMessages(boolean getDeleted, int limit){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String query = "SELECT * FROM " + TABLE
                    +(!getDeleted ? " WHERE "+COL_DELETED+"="+FALSE : "")
                    +(limit > 0 ? " LIMIT "+limit : "")
                    +"ORDER BY "+COL_DELETED+", "+COL_PRIORITY+" DESC;";
            Cursor cursor = db.rawQuery(query, null);
            if(cursor.getCount() > 0){
                Message[] result = convertToMessages(cursor);
                cursor.close();
                return result;
            }
            cursor.close();
        }
        return null;
    }

    /** Return an array of messages sorted by according their priority and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return Array of Message items based on database items matching conditions
     */
    public Message[] getMessagesContaining(String message, boolean getDeleted, int limit){
        if(message == null) message = "";

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String query = "SELECT * FROM " + TABLE+" WHERE "+COL_MESSAGE+" LIKE '%"+message+"%'"
                    +(!getDeleted ? " AND "+COL_DELETED+"="+FALSE : "")
                    +(limit > 0 ? " LIMIT "+limit : "")
                    +"ORDER BY "+COL_DELETED+", "+COL_PRIORITY+" DESC;";
            Cursor cursor = db.rawQuery(query, null);
            if(cursor.getCount() > 0){
                Message[] result = convertToMessages(cursor);
                cursor.close();
                return result;
            }
            cursor.close();
        }
        return null;
    }

    /** Return a single message matching supplied text or null if no match can be found.
     * @param getDeleted whether or not results should include deleted items
     * @return A Message item based on database item matching conditions
     */
    private Message getMessage(String message, boolean getDeleted) throws IllegalArgumentException{
        if(message == null || message.isEmpty()) throw new IllegalArgumentException("Message cannot be empty or null ["+message+"].");

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String query = "SELECT * FROM " + TABLE + " WHERE " + COL_MESSAGE + "='" + message + "'"
                    +(!getDeleted ? " AND "+COL_DELETED+"="+FALSE : "")
                    +" LIMIT 1;";
            Cursor cursor = db.rawQuery(query, null);
            if(cursor.getCount() > 0){
                Message result = convertToMessages(cursor)[0];
                cursor.close();
                return result;
            }
            cursor.close();
            Log.d(TAG, "getMessage found no match for [" + message + "]");
        }
        return null;
    }

    /**
     * Adds the given message with the given priority.
     *
     * @param message      The message to add.
     * @param priority The priority to associate with the message. The priority must
     *                 be [0,1].
     * @param enforceLimit whether or not the priority should be streamlined to the limits
     *                     if set to false and value is outside of limit, an exception is thrown
     * @return Returns true if the message was added. If message already exists, update its values
     */
    public boolean addMessage(String message, double priority, boolean enforceLimit){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            if(enforceLimit){
                priority = streamlinePriority(priority);
            } else {
                checkPriority(priority);
            }
            ContentValues content = new ContentValues();
            content.put(COL_MESSAGE, message);
            content.put(COL_PRIORITY, priority);
            db.insert(TABLE, null, content);
            Log.d(TAG, "Message added to store.");
            return true;
        }
        Log.d(TAG, "Message not added to store, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Remove the given message from the store, the message data is retained with its deleted
     * state set to true.
     *
     * @param message The message to remove.
     * @return Returns true if the message was removed. If the message was not
     * found, returns false.
     */
    public boolean removeMessage(String message){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_MESSAGE + "='" + message + "';");
            return  true;
        }
        Log.d(TAG, "Message not added to store, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Delete the given message from the store. this will completely remove the data from storage
     *
     * @param message The message to remove.
     * @return Returns true if the message was removed or not found, false otherwise
     */
    public boolean deleteMessage(String message) {
        SQLiteDatabase db = getWritableDatabase();
        if (db != null && message != null) {
            db.execSQL("DELETE FROM " + TABLE + " WHERE " + COL_MESSAGE + "='" + message + "';");
            return true;
        }
        Log.d(TAG, "Message not added to store, either message or database is null. [" + message + "]");
        return false;
    }

    /** return the amount of items in the database.
     *
     * @param countDeleted if set to true count will include items marked as deleted
     * @return number of items in the database.
     */
    public long getMessageCount(boolean countDeleted){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null){
            return DatabaseUtils.queryNumEntries(db, TABLE, countDeleted ? null : COL_DELETED+"="+FALSE);
        }
        return 0;
    }

    /**
     * Update the priority of a message, if it exists in the store
     *
     * @param message      The message whose priority should be changed.
     * @param priority The new priority to set.
     * @param enforceLimit whether or not the new priority should be streamlined to limits
     *                      if set to false and priority is outside of limit an exception is thrown
     * @return True if the message was in the store (and its priority was changed),
     * false otherwise.
     */
    public boolean updatePriority(String message, double priority, boolean enforceLimit) {
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            if(enforceLimit){
                priority = streamlinePriority(priority);
            } else {
                checkPriority(priority);
            }
            db.execSQL("UPDATE "+TABLE+" SET "+COL_PRIORITY+"="+priority+" WHERE "+COL_MESSAGE+"='"+message+"';");

            Log.d(TAG, "Message priority changed to store.");
            return true;
        }
        Log.d(TAG, "Message not be edited, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Message Object that contains the message's priority and the contents of
     * the message.
     *
     * @author Jesus Garcia
     */
    public class Message {
        /**
         * The priority of the message.
         */
        private double mPriority;
        /**
         * The contents of the message.
         */
        private String mMessage;

        public Message(double priority, String message) {
            mPriority = priority;
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }

        public double getPriority() {
            return mPriority;
        }

        /**this method is used for temporary displayed item update purposes and should not be used
         * for actual stored data manipulation */
        public void setPriority(double priority){
            mPriority = priority;
        }
    }
}
