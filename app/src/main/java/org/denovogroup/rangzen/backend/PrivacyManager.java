package org.denovogroup.rangzen.backend;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 11/17/2015.
 *
 * The controller class in charge of managing various privacy profiles available through the app
 * and provid the methods for setting and reading the currently set privacy related settings
 */
public class PrivacyManager {

    private static PrivacyManager instance;

    /** list of privacy schemes available */
    private static List<PrivacyScheme> schemes;

    /**an index for the position of the LOW privacy scheme in the list */
    public static final int PRIVACY_LOW = 0;
    /**an index for the position of the MEDIUM privacy scheme in the list */
    public static final int PRIVACY_MEDIUM = 1;
    /**an index for the position of the HIGH privacy scheme in the list */
    public static final int PRIVACY_HIGH = 2;

    /** the shared preference file where privacy settings are stored */
    public static final String SETTINGS_FILE = "Settings";
    /** the key under which privacy scheme is set in the file*/
    public static final String PRIVACY_KEY = "privacy";
    /** the key under which priority threshold is set in the file*/
    public static final String PRIORITY_THRESHOLD_KEY = "priority_threshold";


    /** Default privacy scheme value if none is stored */
    public static final int DEFAULT_PRIVACY_SCHEME = PRIVACY_MEDIUM;
    /** Default priority threshold value if none is stored */
    public static final float DEFAULT_PRIORITY_THRESHOLD = 0f;

    /** initiate the managers parameters such as the schemesList */
    private void init(){
        schemes = new ArrayList<>();

        schemes.add(new PrivacyScheme("Low",true, true, -1, true, true, false, true, -1));
        schemes.add(new PrivacyScheme("Medium",true, false, 1000, true, true, true, true, 3));
        schemes.add(new PrivacyScheme("High",false, false, 500, false, true, true, false, 5));
    }

    /** return the existing instance of the manager if exists. create new if not*/
    public static PrivacyManager getInstance(){
        if(instance == null){
            instance = new PrivacyManager();
            instance.init();
        }

        return instance;
    }

    private PrivacyManager() {
        //an empty constructor
    }

    /** Return the names of available schemes managed by this manager */
    public String[] getSchemes(){
        String[] names = new String[schemes.size()];
        for(int i=0; i<names.length; i++){
            names[i] = schemes.get(i).getName();
        }

        return names;
    }

    /** return the PrivacyScheme object located in the i position in the schemes list of this adapter */
    public PrivacyScheme getScheme(int i){
        return schemes.get(i);
    }

    /** return the PrivacyScheme object with the supplied name in the schemes list of this adapter  or null*/
    public PrivacyScheme getScheme(String name){

        for(PrivacyScheme scheme : schemes) {
            if(scheme.name.equals(name)) return scheme;
        }
        return null;
    }

    /** read the currently set privacy scheme from local storage */
    public static int getCurrentScheme(Context context){
        return context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE).getInt(PRIVACY_KEY, DEFAULT_PRIVACY_SCHEME);
    }

    /** write the supplied privacy scheme to local storage */
    public static void setCurrentScheme(Context context, int scheme){
        context.getSharedPreferences(SETTINGS_FILE,Context.MODE_PRIVATE).edit()
            .putInt(PRIVACY_KEY, scheme)
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
}