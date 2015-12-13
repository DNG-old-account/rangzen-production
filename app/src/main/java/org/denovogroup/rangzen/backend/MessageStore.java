/*
 * Copyright (c) 2014, De Novo Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.denovogroup.rangzen.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import org.denovogroup.rangzen.objects.RangzenMessage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Storage for Rangzen messages that uses StorageBase underneath. If
 * instantiated as such, automatically encrypts and decrypts data before storing
 * in Android.
 */
public class MessageStore extends SQLiteOpenHelper {

    public static final String NEW_MESSAGE = "new message";

    private static String storeVersion;

    private static MessageStore instance;
    private static final String TAG = "MessageStore";
    //readable true/false operators since SQLite does not support boolean values
    public static final int TRUE = 1;
    public static final int FALSE = 0;

    //messages properties
    private static final double MIN_TRUST = 0.01f;
    private static final double MAX_PRIORITY_VALUE = 1.0f;
    private static final int MAX_MESSAGE_SIZE = 140;
    private static final double DEFAULT_PRIORITY = 0;

    private static final String DATABASE_NAME = "MessageStore.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE = "Messages";
    public static final String COL_ROWID = "_id";
    private static final String COL_MESSAGE_ID = "messageId";
    public static final String COL_MESSAGE = "message";
    public static final String COL_TRUST = "trust";
    public static final String COL_LIKES = "likes";
    public static final String COL_LIKED = "liked";
    public static final String COL_PSEUDONYM = "pseudonym";
    public static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_DELETED = "deleted";
    public static final String COL_READ = "read";
    public static final String COL_EXPIRE = "expire";
    public static final String COL_LATLONG = "location";

    private static final String[] defaultSort = new String[]{COL_DELETED,COL_READ};

    private String sortOption;

    /** Get the current instance of MessageStore and create one if necessary.
     * Implemented as a singleton */
    public synchronized static MessageStore getInstance(Context context){
        if(instance == null && context != null){
            instance = new MessageStore(context);
            instance.setSortOption(new String[]{COL_ROWID}, false);
        }
        return instance;
    }

    /** Get the current instance of MessageStore or null if none already created */
    public synchronized static MessageStore getInstance(){
        return getInstance(null);
    }

    /** private constructor for forcing singleton pattern for MessageStore */
    private MessageStore(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        instance = this;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + COL_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                //+ COL_MESSAGE_ID + " VARCHAR(255) NOT NULL,"
                + COL_MESSAGE + " VARCHAR(" + MAX_MESSAGE_SIZE + ") NOT NULL,"
                + COL_TIMESTAMP + " INTEGER NOT NULL,"
                + COL_EXPIRE + " INTEGER NOT NULL,"
                + COL_TRUST + " REAL NOT NULL DEFAULT " + MIN_TRUST + ","
                + COL_LIKES + " INT NOT NULL DEFAULT " + DEFAULT_PRIORITY + ","
                + COL_PSEUDONYM + " VARCHAR(255) NOT NULL,"
                + COL_LATLONG + " TEXT,"
                + COL_LIKED + " BOOLEAN DEFAULT " + FALSE + " NOT NULL CHECK(" + COL_LIKED + " IN(" + TRUE + "," + FALSE + ")),"
                + COL_DELETED + " BOOLEAN DEFAULT " + FALSE + " NOT NULL CHECK(" + COL_DELETED + " IN(" + TRUE + "," + FALSE + ")),"
                + COL_READ + " BOOLEAN DEFAULT " + FALSE + " NOT NULL CHECK(" + COL_READ + " IN(" + TRUE + "," + FALSE + "))"
                + ");");
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
     * Check that the given trust value is in range.
     * @throws IllegalArgumentException if value is outside of range
     */
    private static void checkTrust(double priority) throws IllegalArgumentException{
        if (priority < MIN_TRUST || priority > MAX_PRIORITY_VALUE) {
            throw new IllegalArgumentException("Priority " + priority
                    + " is outside valid range of ["+ MIN_TRUST +","+MAX_PRIORITY_VALUE+"]");
        }
    }

    /**
     * Check check that the given trust value is in range and return nearest limit if not.
     */
    private static double streamlineTrust(double priority){
        if (priority < MIN_TRUST){
            return MIN_TRUST;

        } else if(priority > MAX_PRIORITY_VALUE) {
            return MAX_PRIORITY_VALUE;
        }else{
            return priority;
        }
    }

    /** convert cursor data returned from SQL queries into Message objects that can be returned to
     * query supplier. This implementation does not close the supplied cursor when done
     * @param cursor Cursor data returned from SQLite database
     * @return list of Message items contained by the cursor or an empty list if cursor was empty
     */
    private List<RangzenMessage> convertToMessages(Cursor cursor){

        List<RangzenMessage> messages = new ArrayList<>();
        cursor.moveToFirst();

        int trustColIndex = cursor.getColumnIndex(COL_TRUST);
        int priorityColIndex = cursor.getColumnIndex(COL_LIKES);
        int messageColIndex = cursor.getColumnIndex(COL_MESSAGE);
        int pseudonymColIndex = cursor.getColumnIndex(COL_PSEUDONYM);
        int timestampColIndex = cursor.getColumnIndex(COL_TIMESTAMP);
        int latlongColIndex = cursor.getColumnIndex(COL_LATLONG);
        int timeboundColIndex = cursor.getColumnIndex(COL_EXPIRE);

        if (cursor.getCount() > 0) {
            while (!cursor.isAfterLast()){
                messages.add(new RangzenMessage(
                        cursor.getString(messageColIndex),
                        cursor.getDouble(trustColIndex),
                        cursor.getInt(priorityColIndex),
                        cursor.getString(pseudonymColIndex),
                        cursor.getLong(timestampColIndex),
                        cursor.getString(latlongColIndex),
                        cursor.getLong(timeboundColIndex)
                ));
                cursor.moveToNext();
            }
        }

        return messages;
    }

    /** Return a cursor pointing to messages sorted by according their priority and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return Cursor with Message items based on database items matching conditions
     */
    public Cursor getMessagesCursor(boolean getDeleted, int limit){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null) {
            String query = "SELECT * FROM " + TABLE
                    + (!getDeleted ? " WHERE " + COL_DELETED + "=" + FALSE : "")
                    + " " + sortOption
                    + (limit > 0 ? " LIMIT " + limit : "")
                    + ";";
            return db.rawQuery(query, null);
        }
        return null;
    }

    /** Return an array of messages sorted by according their priority and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return List of Message items based on database items matching conditions
     */
    public List<RangzenMessage> getMessages(boolean getDeleted, int limit){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            Cursor cursor = getMessagesCursor(getDeleted, limit);
            if(cursor != null && cursor.getCount() > 0){
                List<RangzenMessage> result = convertToMessages(cursor);
                cursor.close();
                return result;
            }
            cursor.close();
        }
        return new ArrayList<>();
    }

    /** Return a cursor pointing to messages sorted by according their priority and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return Cursor of Message items based on database items matching conditions
     */
    public Cursor getMessagesContainingCursor(String message, boolean getDeleted, int limit){
        if(message == null) message = "";

        SQLiteDatabase db = getWritableDatabase();
        if(db != null) {
            String query = "SELECT * FROM " + TABLE + " WHERE " + COL_MESSAGE + " LIKE '%" + Utils.makeTextSafeForSQL(message) + "%'"
                    + (!getDeleted ? " AND " + COL_DELETED + "=" + FALSE : "")
                    + " "+sortOption
                    + (limit > 0 ? " LIMIT " + limit : "")
                    + ";";
            return db.rawQuery(query, null);
        }
        return null;
    }

    /** Return an array of messages sorted by according their priority and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return List of Message items based on database items matching conditions
     */
    public List<RangzenMessage> getMessagesContaining(String message, boolean getDeleted, int limit){
        if(message == null) message = "";

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            Cursor cursor = getMessagesContainingCursor(message, getDeleted, limit);
            if(cursor != null && cursor.getCount() > 0){
                List<RangzenMessage> result = convertToMessages(cursor);
                cursor.close();
                return result;
            }
            cursor.close();
        }
        return new ArrayList<>();
    }

    /** Return a single message matching supplied text or null if no match can be found.
     * @param getDeleted whether or not results should include deleted items
     * @return A Message item based on database item matching conditions or null
     */
    private Cursor getMessageCursor(String message, boolean getDeleted) throws IllegalArgumentException{
        if(message == null || message.isEmpty()) throw new IllegalArgumentException("Message cannot be empty or null ["+message+"].");

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String query = "SELECT * FROM " + TABLE + " WHERE " + COL_MESSAGE + "='" + Utils.makeTextSafeForSQL(message) + "'"
                    +(!getDeleted ? " AND "+COL_DELETED+"="+FALSE : "")
                    +" LIMIT 1;";
            return db.rawQuery(query, null);
        }
        return null;
    }

    /** Return a single message matching supplied text or null if no match can be found.
     * @param getDeleted whether or not results should include deleted items
     * @return A Message item based on database item matching conditions or null
     */
    private RangzenMessage getMessage(String message, boolean getDeleted) throws IllegalArgumentException{
        if(message == null || message.isEmpty()) throw new IllegalArgumentException("Message cannot be empty or null ["+message+"].");

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            Cursor cursor = getMessageCursor(message, getDeleted);
            if(cursor.getCount() > 0){
                RangzenMessage result = convertToMessages(cursor).get(0);
                cursor.close();
                return result;
            }
            cursor.close();
            Log.d(TAG, "getMessage found no match for [" + message + "]");
        }
        return null;
    }

    /** return if message exists in database and is not in removed state **/
    public boolean contains(String message){
        return getMessage(message, false) != null;
    }

    /** return if message exists in database, even if is in removed state **/
    public boolean containsOrRemoved(String message){
        return getMessage(message, true) != null;
    }

    /** return the message in position K from the database. K position is calculated
     * after sorting results based on trust. removed messages do not count.
     * @param position position of the message to be returned
     * @return Message in the K position based on priority or null if position too high
     */
    public RangzenMessage getKthMessage(int position){
        List<RangzenMessage> result = getMessages(false, position + 1);
        return (result.size() > position) ? result.get(position) : null;
    }

    /**
     * Adds the given message with the given priority.
     *
     * @param message The message to add.
     * @param trust The trust to associate with the message. The trust must
     *                 be [0,1].
     * @param priority The priority to associate with the message.
     * @param pseudonym The senders pseudonym.
     * @param enforceLimit whether or not the trust should be streamlined to the limits
     *                     if set to false and value is outside of limit, an exception is thrown
     * @return Returns true if the message was added. If message already exists, update its values
     */
    public boolean addMessage(String message, double trust, double priority, String pseudonym, long timestamp,boolean enforceLimit, long timebound, Location location){

        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            if (enforceLimit) {
                trust = streamlineTrust(trust);
            } else {
                checkTrust(trust);
            }

            if(message.length() > MAX_MESSAGE_SIZE) message = message.substring(0, MAX_MESSAGE_SIZE);

            Calendar tempCal = Calendar.getInstance();
            tempCal.setTimeInMillis(timestamp);
            Calendar reducedTimestamp = Utils.reduceCalendar(tempCal);

            if(containsOrRemoved(message)) {
                db.execSQL("UPDATE "+TABLE+" SET "
                        +COL_TRUST+"="+trust+","
                        +COL_DELETED+"="+FALSE+","
                        + COL_LIKES +"="+priority+","
                        +COL_PSEUDONYM+"='"+pseudonym+"',"
                        + ((location != null) ? (COL_LATLONG+"='"+location.getLatitude()+"x"+location.getLongitude()+"',") : "")
                        +COL_TIMESTAMP+"="+reducedTimestamp.getTimeInMillis()+","
                        +COL_EXPIRE+"="+timebound
                        +" WHERE " + COL_MESSAGE + "='" + Utils.makeTextSafeForSQL(message) + "';");
                Log.d(TAG, "Message was already in store and was simply updated.");
            } else {
                ContentValues content = new ContentValues();
                content.put(COL_MESSAGE, message);
                content.put(COL_TRUST, trust);
                content.put(COL_LIKES, priority);
                if(location != null) content.put(COL_LATLONG, location.getLatitude()+"x"+location.getLongitude());
                content.put(COL_PSEUDONYM, pseudonym);
                content.put(COL_EXPIRE, timebound);
                content.put(COL_TIMESTAMP, reducedTimestamp.getTimeInMillis());
                db.insert(TABLE, null, content);
                Log.d(TAG, "Message added to store.");
            }
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
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_MESSAGE + "='" + Utils.makeTextSafeForSQL(message) + "';");
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
            db.execSQL("DELETE FROM " + TABLE + " WHERE " + COL_MESSAGE + "='" + Utils.makeTextSafeForSQL(message) + "';");
            return true;
        }
        Log.d(TAG, "Message not deleted from store, either message or database is null. [" + message + "]");
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
            return DatabaseUtils.queryNumEntries(db, TABLE, countDeleted ? null : COL_DELETED + "=" + FALSE);
        }
        return 0;
    }

    /** return the trust of the given message or 0 if message not exists**/
    public double getTrust(String message){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            Cursor cursor = db.rawQuery("SELECT "+COL_TRUST+" FROM "+TABLE+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';", null);
            if(cursor.getCount() > 0){
                cursor.moveToFirst();
                return cursor.getDouble(cursor.getColumnIndex(COL_TRUST));
            }
        }
        return 0;
    }

    /** return the priority of the given message or 0 if message not exists**/
    public double getPriority(String message){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            Cursor cursor = db.rawQuery("SELECT "+ COL_LIKES +" FROM "+TABLE+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';", null);
            if(cursor.getCount() > 0){
                cursor.moveToFirst();
                return cursor.getInt(cursor.getColumnIndex(COL_LIKES));
            }
        }
        return 0;
    }

    /**
     * Update the priority of a message, if it exists in the store
     *
     * @param message      The message whose priority should be changed.
     * @param trust The new trust to set.
     * @param enforceLimit whether or not the new priority should be streamlined to limits
     *                      if set to false and priority is outside of limit an exception is thrown
     * @return True if the message was in the store (and its priority was changed),
     * false otherwise.
     */
    public boolean updateTrust(String message, double trust, boolean enforceLimit) {
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            if(enforceLimit){
                trust = streamlineTrust(trust);
            } else {
                checkTrust(trust);
            }
            db.execSQL("UPDATE "+TABLE+" SET "+COL_TRUST+"="+trust+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';");

            Log.d(TAG, "Message trust changed in the store.");
            return true;
        }
        Log.d(TAG, "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Update the priority of a message, if it exists in the store
     *
     * @param message      The message whose priority should be changed.
     * @param priority The new priority to set.
     * @return True if the message was in the store (and its priority was changed),
     * false otherwise.
     */
    public boolean updateTrust(String message, int priority) {
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            db.execSQL("UPDATE "+TABLE+" SET "+ COL_LIKES +"="+priority+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';");

            Log.d(TAG, "Message priority changed in the store.");
            return true;
        }
        Log.d(TAG, "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Update the priority of a message, if it exists in the store
     *
     * @param message      The message whose priority should be changed.
     * @param priority The new priority to set.
     * @return True if the message was in the store (and its priority was changed),
     * false otherwise.
     */
    public boolean updatePriority(String message, int priority) {
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            db.execSQL("UPDATE "+TABLE+" SET "+ COL_LIKES +"="+priority+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';");

            Log.d(TAG, "Message priority changed in the store.");
            return true;
        }
        Log.d(TAG, "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /** Updating the priority of a message by either +1 or -1 and set the message state as liked or not liked
     *
     * @param message the message to edit
     * @param like if to set the like status of the message to true or false
     * @return true if message was found and edited, false otherwise (false is also returned if message was already in the liked status
     */
    public boolean likeMessage(String message, boolean like){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            int likedStatus = like ? FALSE : TRUE;
            Cursor c = db.rawQuery("SELECT "+COL_LIKES+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" AND "+COL_LIKED+"="+likedStatus+" AND "+COL_MESSAGE+" ='"+Utils.makeTextSafeForSQL(message)+"';",null);
            c.moveToFirst();
            if(c.getCount() > 0){
                int likes = c.getInt(c.getColumnIndex(COL_LIKES)) + (like ? 1 : -1);
                c.close();
                likes = Math.max(0, likes);
                db.execSQL("UPDATE "+TABLE+" SET "+COL_LIKED+"="+(like ? TRUE : FALSE)+","+COL_LIKES +"="+likes+" WHERE "+COL_DELETED+" ="+FALSE+" AND "+COL_LIKED+"="+likedStatus+" AND "+COL_MESSAGE+" ='"+Utils.makeTextSafeForSQL(message)+"';");
                return true;
            }
        }
        Log.d(TAG, "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /** Return the current version of the store */
    public String getStoreVersion(){
        if(storeVersion == null) updateStoreVersion();

        return  storeVersion;
    }

    /** Randomize a version code for the store and set it*/
    public void updateStoreVersion(){
        storeVersion = UUID.randomUUID().toString();
    }

    /** set the read state of the supplied message to either read or unread */
    public boolean setRead(String message, boolean isRead){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            int read = isRead ? TRUE : FALSE;
            db.execSQL("UPDATE "+TABLE+" SET "+COL_READ+"="+read+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';");

            Log.d(TAG, "Message read state changed in the store.");
            return true;
        }
        Log.d(TAG, "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /** reset the local storage to make all the messages marked as read */
    public boolean setAllAsRead(){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            db.execSQL("UPDATE "+TABLE+" SET "+COL_READ+"="+TRUE+";");

            Log.d(TAG, "Messages read state changed in the store.");
            return true;
        }
        Log.d(TAG, "Messages not edited, database is null.");
        return false;
    }

    public long getUnreadCount(){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            return DatabaseUtils.queryNumEntries(db, TABLE, COL_READ + "=" + FALSE);
        }
        return  0;
    }

    /** completely delete records from the database based on passed security profile
     *
     * @param currentProfile
     */
    public void deleteOutdatedOrIrrelevant(SecurityProfile currentProfile){

        if(currentProfile == null || !currentProfile.isAutodelete()) return;

        SQLiteDatabase db = getWritableDatabase();
        if(db == null) return;

        Calendar reducedAge = Utils.reduceCalendar(Calendar.getInstance());

        long ageThreshold = reducedAge.getTimeInMillis() - currentProfile.getAutodeleteAge() * 1000L * 60L *60L * 24L;

        db.execSQL("DELETE FROM "+TABLE+" WHERE "+COL_TRUST+"<="+currentProfile.getAutodeleteTrust()
                +" OR ("+COL_TIMESTAMP+"> 0 AND "+COL_TIMESTAMP+"<"+ageThreshold+")"
                +" OR ("+COL_EXPIRE+"> 0 AND "+COL_EXPIRE+"<"+reducedAge.getTimeInMillis()+");"
        );
    }

    public Cursor getMessagesByQuery(String query){
        SQLiteDatabase db = getWritableDatabase();
        if(db == null || query == null) return null;

        String pretext = "SELECT * FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" ";
        String posttext = " "+sortOption;

        try {
            Cursor cursor = db.rawQuery(pretext + query + posttext, null);
            return cursor;
        } catch (Exception e){
            return null;
        }
    }

    public void deleteByLikes(int likes){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            db.execSQL("DELETE FROM " + TABLE + " WHERE " + COL_LIKES + "<=" + likes + ";");
        }
    }

    public void deleteByTrust(float trust){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            db.execSQL("DELETE FROM " + TABLE + " WHERE " + COL_TRUST + "<=" + trust + ";");
        }
    }

    public void deleteBySender(String sender){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null && sender != null && sender.length() > 0) {
            db.execSQL("DELETE FROM " + TABLE + " WHERE " + COL_PSEUDONYM + "='" + sender + "';");
        }
    }

    public void purgeStore(){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            onCreate(db);
        }
    }

    public void setSortOption(String[] columns, boolean ascending){
        String options = "";
        for (int i = 0; i < defaultSort.length; i++) {
            options += defaultSort[i];
            if (i < defaultSort.length - 1 ||columns != null) {
                options += ",";
            }
        }

        if(columns != null) {
            for (int i = 0; i < columns.length; i++) {
                options += columns[i];
                if (i < columns.length - 1) {
                    options += ",";
                }
            }
        }
        sortOption = "ORDER BY "+options+(ascending ? " ASC" : " DESC");
    }
}
