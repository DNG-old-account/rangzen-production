package org.denovogroup.rangzen.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 11/17/2015.
 *
 * The controller class in charge of managing various privacy profiles available through the app
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
}