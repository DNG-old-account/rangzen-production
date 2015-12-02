package org.denovogroup.rangzen.backend;

import android.content.Context;
import android.content.SharedPreferences;

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
    /** the key under which security profile is set in the file*/
    public static final String SECURITY_KEY = "security";
    /** the key under which priority threshold is set in the file*/
    public static final String PRIORITY_THRESHOLD_KEY = "priority_threshold";
    /** the key under which use pseudonym is set in the file*/
    public static final String PSEUDONYM_KEY = "pseudonym";


    /** Default security profile value if none is stored */
    public static final int DEFAULT_SECURITY_PROFILE = SECURITY_MEDIUM;
    /** Default priority threshold value if none is stored */
    public static final float DEFAULT_PRIORITY_THRESHOLD = 0f;
    /** Default pseudonym value if none is stored */
    public static final String DEFAULT_PSEUDONYM = "John snow";

    /** initiate the managers parameters such as the profiles list */
    private void init(){
        profiles = new ArrayList<>();

        profiles.add(new SecurityProfile(0,"Low", true, true, -1, true, true, false, true, -1));
        profiles.add(new SecurityProfile(1,"Medium", true, false, 1000, true, true, true, true, 3));
        profiles.add(new SecurityProfile(2,"High", false, false, 500, false, true, true, false, 5));
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
        String[] names = new String[profiles.size()];
        for(int i=0; i<names.length; i++){
            names[i] = profiles.get(i).getName();
        }

        return names;
    }

    /** return the SecurityProfile object located in the i position in the profiles list of this adapter */
    public SecurityProfile getProfile(int i){
        return profiles.get(i);
    }

    /** return the SecurityProfile object with the supplied name in the profiles list of this adapter  or null*/
    public SecurityProfile getProfile(String name){

        for(SecurityProfile profile : profiles) {
            if(profile.name.equals(name)) return profile;
        }
        return null;
    }

    /** read the currently set privacy profile from local storage */
    public static int getCurrentProfile(Context context){
        return context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE).getInt(SECURITY_KEY, DEFAULT_SECURITY_PROFILE);
    }

    /** write the supplied privacy profile to local storage */
    public static void setCurrentProfile(Context context, int profile){
        context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE).edit()
            .putInt(SECURITY_KEY, profile)
            .commit();
    }

    /** read the currently set priority threshold for autodelete from local storage */
    public static float getCurrentAutodeleteThreshold(Context context){
        return context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE)
                .getFloat(PRIORITY_THRESHOLD_KEY, DEFAULT_PRIORITY_THRESHOLD);
    }

    /** write the supplied priority threshold for autodelete to local storage */
    public static void setCurrentAutodeleteThreshold(Context context, float threshold){
        context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE).edit()
                .putFloat(PRIORITY_THRESHOLD_KEY, threshold)
                .commit();
    }

    /** read the currently set user pseudonym from local storage */
    public static String getCurrentPseudonym(Context context){
        SharedPreferences pref = context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE);

        if(!pref.contains(PSEUDONYM_KEY)){
            setCurrentPseudonym(context, DEFAULT_PSEUDONYM+(System.nanoTime()/System.currentTimeMillis()));
        }
        return pref.getString(PSEUDONYM_KEY, DEFAULT_PSEUDONYM);
    }

    /** write the supplied pseudonym to local storage */
    public static void setCurrentPseudonym(Context context, String name){
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
}