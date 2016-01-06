package org.denovogroup.rangzen.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Created by Liran on 1/6/2016.
 */
public class WelcomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String prefName = "initializes";
        String prefProperty = "initializes";

        SharedPreferences prefFile = getSharedPreferences(prefName, Context.MODE_PRIVATE);

        if(prefFile.contains(prefProperty)){
            goToMain();
        } else {
            prefFile.edit().putBoolean(prefProperty,true).commit();
            //TODO show welcome UI, for now just go to main anyway
            goToMain();
        }

    }

    private void goToMain(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
