package org.denovogroup.rangzen.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 11/17/2015.
 *
 * The controller class in charge of managing various security profiles available through the app
 * and provid the methods for setting and reading the currently set security related settings
 */
public class SecurityManager {

    private static SecurityManager instance;

    /** list of privacy profiles available */
    private static List<SecurityProfile> profiles;

    /**an index for the position of the LOW security profile in the list */
    public static final int SECURITY_LOW = 0;
    /**an index for the position of the MEDIUM security profile in the list */
    public static final int SECURITY_MEDIUM = 1;
    /**an index for the position of the HIGH security profile in the list */
    public static final int SECURITY_HIGH = 2;

    /** the shared preference file where security settings are stored */
    public static final String SETTINGS_FILE = "Settings";
    /** the key under which use pseudonym is set in the file*/
    public static final String PSEUDONYM_KEY = "pseudonym";

    // profile settings
    public static final String CUSTOM_PROFILE_NAME = "Custom";
    private static final String PROFILE_NAME_KEY = "name";
    private static final String PROFILE_TIMESTAMP_KEY = "useTimestamp";
    private static final String PROFILE_PSEUDONYM_KEY = "usePseudonym";
    private static final String PROFILE_FEED_SIZE_KEY = "maxFeedSize";
    private static final String PROFILE_FRIEND_VIA_BOOK_KEY = "addFromBook";
    private static final String PROFILE_FRIEND_VIA_QR_KEY = "addFromQR";
    private static final String PROFILE_AUTO_DELETE_KEY = "useAutoDecay";
    private static final String PROFILE_AUTO_DELETE_TRUST_KEY = "AutoDecayTrust";
    private static final String PROFILE_AUTO_DELETE_AGE_KEY = "AutoDecayAge";
    private static final String PROFILE_SHARE_LOCATIONS_KEY = "shareLocations";
    private static final String PROFILE_MIN_SHARED_CONTACTS_KEY = "minSharedContacts";
    private static final String PROFILE_MAX_MESSAGES_KEY = "maxMessagesPerExchange";
    private static final String PROFILE_COOLDOWN_KEY = "exchangeCooldown";
    private static final String PROFILE_TIMEBOUND_KEY = "timebound";


    /** Default security profile value if none is stored */
    public static final int DEFAULT_SECURITY_PROFILE = SECURITY_MEDIUM;
    /** Default trust threshold value if none is stored */
    public static final float DEFAULT_TRUST_THRESHOLD = 0.01f;
    /** Default pseudonym value if none is stored */
    public static final String DEFAULT_PSEUDONYM = "John snow";

    /** initiate the managers parameters such as the profiles list */
    private void init(){
        profiles = new ArrayList<>();
        profiles.add(new SecurityProfile(1,"Basic", true, true, 999999999, true, true, false, 0f, 0, true, 0, 300, 5, 60));
        profiles.add(new SecurityProfile(2,"Moderate", true, false, 1000, true, true, true, 0.01f, 14, true, 3, 200, 15, 30));
        profiles.add(new SecurityProfile(3,"Strict", false, false, 500, false, true, true, 0.05f, 14, false, 5, 100, 30, 14));
    }

    /** return the existing instance of the manager if exists. create new if not*/
    public static SecurityManager getInstance(){
        if(instance == null){
            instance = new SecurityManager();
            instance.init();
        }
        return instance;
    }

    private SecurityManager() {
        //an empty constructor
    }

    /** Return the names of available profiles managed by this manager */
    public String[] getProfiles(){
        String[] names = new String[profiles.size()+1];
        names[0] = CUSTOM_PROFILE_NAME;
        for(int i=1; i<names.length; i++){
            names[i] = profiles.get(i-1).getName();
        }

        return names;
    }

    /** return the SecurityProfile object located in the i position in the profiles list of this adapter */
    public SecurityProfile getProfile(int i){
        return profiles.get(i);
    }

    /** return the SecurityProfile object with the supplied name in the profiles list of this adapter or null if using custom profile*/
    public SecurityProfile getProfile(String name){

        for(SecurityProfile profile : profiles) {
            if(profile.name.equals(name)) return profile.clone();
        }
        return null;
    }

    /** read the currently set privacy profile from local storage */
    public static SecurityProfile getCurrentProfile(Context context){
        if(instance == null) getInstance();

        SharedPreferences pref = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);

        String profile_name = pref.getString(PROFILE_NAME_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getName());

        if(profile_name != CUSTOM_PROFILE_NAME){
            for(SecurityProfile profile : profiles) {
                if(profile.name.equals(profile_name)){
                    return profile.clone();
                }
            }
        }

        SecurityProfile customProfile = new SecurityProfile(
                0,
                CUSTOM_PROFILE_NAME,
                pref.getBoolean(PROFILE_TIMESTAMP_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isTimestamp()),
                pref.getBoolean(PROFILE_PSEUDONYM_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isPseudonyms()),
                pref.getInt(PROFILE_FEED_SIZE_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getFeedSize()),
                pref.getBoolean(PROFILE_FRIEND_VIA_BOOK_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).friendsViaBook),
                pref.getBoolean(PROFILE_FRIEND_VIA_QR_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).friendsViaQR),
                pref.getBoolean(PROFILE_AUTO_DELETE_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isAutodelete()),
                pref.getFloat(PROFILE_AUTO_DELETE_TRUST_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getAutodeleteTrust()),
                pref.getInt(PROFILE_AUTO_DELETE_AGE_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getAutodeleteAge()),
                pref.getBoolean(PROFILE_SHARE_LOCATIONS_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).isShareLocation()),
                pref.getInt(PROFILE_MIN_SHARED_CONTACTS_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getMinSharedContacts()),
                pref.getInt(PROFILE_MAX_MESSAGES_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getMaxMessages()),
                pref.getInt(PROFILE_COOLDOWN_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getCooldown()),
                pref.getInt(PROFILE_TIMEBOUND_KEY, profiles.get(DEFAULT_SECURITY_PROFILE).getTimeboundPeriod())
        );

        return customProfile;
    }

    /** write the properties of specified profile as the current profile in the local storage.
     * return true if specified name recognized and saved, false otherwise */
    public static boolean setCurrentProfile(Context context, String profileName){
        if(instance == null) getInstance();

        for(SecurityProfile profile : profiles){
            if(profile.getName().equals(profileName)){
                setCurrentProfile(context, profile);
                return true;
            }
        }
        return false;
    }

    /** write the supplied privacy profile to local storage */
    public static void setCurrentProfile(Context context, SecurityProfile profile){
        if(instance == null) getInstance();

        SharedPreferences.Editor pref = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE).edit();
            pref.putString(PROFILE_NAME_KEY, profile.getName());
            pref.putBoolean(PROFILE_TIMESTAMP_KEY, profile.isTimestamp());
        pref.putBoolean(PROFILE_PSEUDONYM_KEY, profile.isPseudonyms());
            pref.putInt(PROFILE_FEED_SIZE_KEY, profile.getFeedSize());
            pref.putBoolean(PROFILE_FRIEND_VIA_BOOK_KEY, profile.friendsViaBook);
            pref.putBoolean(PROFILE_FRIEND_VIA_QR_KEY, profile.friendsViaQR);
            pref.putBoolean(PROFILE_AUTO_DELETE_KEY, profile.isAutodelete());
            pref.putFloat(PROFILE_AUTO_DELETE_TRUST_KEY, profile.getAutodeleteTrust());
            pref.putInt(PROFILE_AUTO_DELETE_AGE_KEY, profile.getAutodeleteAge());
            pref.putBoolean(PROFILE_SHARE_LOCATIONS_KEY, profile.isShareLocation());
        pref.putInt(PROFILE_MIN_SHARED_CONTACTS_KEY, profile.getMinSharedContacts());
            pref.putInt(PROFILE_MAX_MESSAGES_KEY, profile.getMaxMessages());
            pref.putInt(PROFILE_COOLDOWN_KEY, profile.getCooldown());
            pref.putInt(PROFILE_TIMEBOUND_KEY, profile.getTimeboundPeriod());
            pref.commit();

        RangzenService.TIME_BETWEEN_EXCHANGES_MILLIS = profile.getCooldown() * 1000;
    }

    /** read the currently set user pseudonym from local storage */
    public static String getCurrentPseudonym(Context context){
        if(instance == null) getInstance();

        SharedPreferences pref = context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE);

        if(!pref.contains(PSEUDONYM_KEY)){
            setCurrentPseudonym(context, DEFAULT_PSEUDONYM+(System.nanoTime()/System.currentTimeMillis()));
        }
        return pref.getString(PSEUDONYM_KEY, DEFAULT_PSEUDONYM);
    }

    /** write the supplied pseudonym to local storage */
    public static void setCurrentPseudonym(Context context, String name){
        if(instance == null) getInstance();

        context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE).edit()
                .putString(PSEUDONYM_KEY, name)
                .commit();
    }

    /** compare the supplied profiles and return the index of the most secure one of the two */
    public int getMostSecureProfile(int profileA, int profileB){
        SecurityProfile profA = getProfile(profileA);
        SecurityProfile profB = getProfile(profileB);

        return (profA.getStrength() > profB.getStrength()) ? profileA : profileB;
    }

    public void clearProfileData(Context context){
        SharedPreferences pref = context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE);
        pref.edit().clear().commit();
    }
}