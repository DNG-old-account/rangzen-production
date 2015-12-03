package org.denovogroup.rangzen.backend;

/**
 * Created by Liran on 11/17/2015.
 *
 * A simple object representing a security profile and its properties
 */
public class SecurityProfile {
    /** profile security strength*/
    int strength;
    /** profile display name*/
    String name;
    /** allow display/storage of timestamps*/
    boolean timestamp;
    /** allow display/storage of pseudonym from sender*/
    boolean pseudonyms;
    /** Number of messages in feed (not archived) (handled as FIFO, i.e. at any given time not more than X messages are kept, newest replaces oldest)*/
    int feedSize;
    /** can add friends from phonebook */
    boolean friendsViaBook;
    /** can add friends using QR code */
    boolean friendsViaQR;
    /**Auto-delete + decay. user can change priority threshold*/
    boolean autodelete;
    /** allow sharing location */
    boolean shareLocation;
    /** Minimum shared contacts for message exchange */
    int minSharedContacts;
    /** Maximum messages for message exchange */
    int maxMessages;

    public SecurityProfile(int strength, String name, boolean timestamp, boolean pseudonyms, int feedSize, boolean friendsViaBook, boolean friendsViaQR, boolean autodelete, boolean shareLocation, int minSharedContacts, int maxMessages) {
        this.strength = strength;
        this.name = name;
        this.timestamp = timestamp;
        this.pseudonyms = pseudonyms;
        this.feedSize = feedSize;
        this.friendsViaBook = friendsViaBook;
        this.friendsViaQR = friendsViaQR;
        this.autodelete = autodelete;
        this.shareLocation = shareLocation;
        this.minSharedContacts = minSharedContacts;
        this.maxMessages = maxMessages;
    }

    public int getStrength(){
        return strength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPseudonyms() {
        return pseudonyms;
    }

    public void setPseudonyms(boolean pseudonyms) {
        this.pseudonyms = pseudonyms;
    }

    public boolean isTimestamp() {
        return timestamp;
    }

    public void setTimestamp(boolean timestamp) {
        this.timestamp = timestamp;
    }

    public int getFeedSize() {
        return feedSize;
    }

    public void setFeedSize(int feedSize) {
        this.feedSize = feedSize;
    }

    public boolean isFriendsViaBook() {
        return friendsViaBook;
    }

    public void setFriendsViaBook(boolean friendsViaBook) {
        this.friendsViaBook = friendsViaBook;
    }

    public boolean isFriendsViaQR() {
        return friendsViaQR;
    }

    public void setFriendsViaQR(boolean friendsViaQR) {
        this.friendsViaQR = friendsViaQR;
    }

    public boolean isAutodelete() {
        return autodelete;
    }

    public void setAutodelete(boolean autodelete) {
        this.autodelete = autodelete;
    }

    public boolean isShareLocation() {
        return shareLocation;
    }

    public void setShareLocation(boolean shareLocation) {
        this.shareLocation = shareLocation;
    }

    public int getMinSharedContacts() {
        return minSharedContacts;
    }

    public void setMinSharedContacts(int minSharedContacts) {
        this.minSharedContacts = minSharedContacts;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public SecurityProfile clone(){
        return new SecurityProfile(
                strength,
                name,
                timestamp,
                pseudonyms,
                feedSize,
                friendsViaBook,
                friendsViaQR,
                autodelete,
                shareLocation,
                minSharedContacts,
                maxMessages);
    }
}
