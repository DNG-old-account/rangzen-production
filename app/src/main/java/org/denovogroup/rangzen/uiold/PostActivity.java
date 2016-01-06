package org.denovogroup.rangzen.uiold;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

/** This activity crates the message sending page. It also handles back button. */
public class PostActivity extends AppCompatActivity {

    public static final String MESSAGE_BODY = "MESSAGE_BODY";
    public static final String MESSAGE_PARENT = "MESSAGE_PARENT";

    MenuItem characterCounter;

    private int maxChars = 140;
    private EditText messageBox;
    String messageBody = "";
    String messageParent = null;
    private long timebound = -1;

    Button timeboundButton;
    LocationManager manager;
    Location myLocation;
    List<String> providers;
    LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (myLocation == null) {
                myLocation = location;
            } else {
                pickBestLocation(location, myLocation);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        providers = manager.getAllProviders();
        for(String provider : providers) {
            manager.requestLocationUpdates(provider, 0, 0, listener);
        }

        if(getIntent().hasExtra(MESSAGE_BODY)){
            messageBody = getIntent().getStringExtra(MESSAGE_BODY);
            if(messageBody.length() > 140) messageBody = messageBody.substring(0, 140);
        }

        setContentView(R.layout.makepost);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Post");

        if(getIntent().hasExtra(MESSAGE_PARENT)){
            messageParent = getIntent().getStringExtra(MESSAGE_PARENT);
            getSupportActionBar().setTitle("Reply");
        }

        messageBox = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.checkbox_tag_location).setVisibility((SecurityManager.getCurrentProfile(this).isShareLocation()) ? View.VISIBLE : View.INVISIBLE);

        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY);

        Button cancel = (Button) findViewById(R.id.button1);
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                messageBox.setText("");
            }
        });

        timeboundButton = (Button) findViewById(R.id.timebound_button);
        timeboundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PostActivity.this);
                builder.setTitle(R.string.select_date);
                final View dialogView = getLayoutInflater().inflate(R.layout.select_date_dialog, null);
                builder.setView(dialogView);
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setNeutralButton(R.string.unbind, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        timebound = -1;
                        timeboundButton.setText(getString(R.string.not_timebound));
                    }
                });
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CalendarView calendarView = (CalendarView) dialogView.findViewById(R.id.calendarView);
                        Calendar today = Utils.reduceCalendar(Calendar.getInstance());
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(calendarView.getDate());
                        cal = Utils.reduceCalendar(cal);

                        if (cal.compareTo(today) > 0){
                            timebound = cal.getTimeInMillis();
                            timeboundButton.setText(Utils.convertTimestampToRelativeDays(timebound)+" days");
                        } else {
                            timebound = -1;
                            timeboundButton.setText(getString(R.string.not_timebound));
                        }
                    }
                });
                final AlertDialog dialog = builder.create();

                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialoginterface) {
                        long display;
                        if(timebound > 0){
                            display = timebound;
                        } else {
                            Calendar today = Calendar.getInstance();
                            today.add(Calendar.DAY_OF_YEAR, SecurityManager.getCurrentProfile(PostActivity.this).getTimeboundPeriod());
                            display = today.getTimeInMillis();
                        }
                        CalendarView calView = (CalendarView) dialogView.findViewById(R.id.calendarView);
                        if(calView != null) {
                            calView.setDate(display, false, true);
                        }
                    }
                });

                dialog.show();
            }
        });

        final Button send = (Button) findViewById(R.id.button2);
        send.setOnClickListener(new View.OnClickListener() {

            /**
             * Stores the text of the TextView in the MessageStore with
             * default priority 1.0f. Displays a Toast upon completion and
             * exits the Activity.
             *
             * @param v
             *            The view which is clicked - in this case, the Button.
             */
            @Override
            public void onClick(View v) {
                MessageStore messageStore = MessageStore.getInstance(PostActivity.this);
                float trust = 1.0f;
                int priority = 0;
                SecurityProfile currentProfile = org.denovogroup.rangzen.backend.SecurityManager.getCurrentProfile(PostActivity.this);
                String pseudonym = currentProfile.isPseudonyms() ?
                        SecurityManager.getCurrentPseudonym(PostActivity.this) : "";
                long timestamp = currentProfile.isTimestamp() ?
                        System.currentTimeMillis() : 0;


                Location myLocation = null;

                if(((CheckBox) findViewById(R.id.checkbox_tag_location)).isChecked()){
                    LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if(myLocation == null) {
                        for (String provider : providers) {
                            if (myLocation == null) {
                                myLocation = manager.getLastKnownLocation(provider);
                                continue;
                            }
                            myLocation = pickBestLocation(manager.getLastKnownLocation(provider), myLocation);
                        }
                    }

                    if(myLocation == null){
                        Toast.makeText(PostActivity.this, "Could not determine location please activate your GPS", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                Random random = new Random();
                long idLong = System.nanoTime()*(1+random.nextInt());
                String messageId = Base64.encodeToString(Crypto.encodeString(String.valueOf(idLong)), Base64.NO_WRAP);


                messageStore.addMessage(PostActivity.this, messageId, messageBody, trust, priority, pseudonym, timestamp, true, timebound, myLocation, messageParent, true,0,0);
                Toast.makeText(PostActivity.this, "Message sent!",
                        Toast.LENGTH_SHORT).show();
                ExchangeHistoryTracker.getInstance().cleanHistory(null);
                MessageStore.getInstance(PostActivity.this).updateStoreVersion();

                PostActivity.this.setResult(Activity.RESULT_OK);
                PostActivity.this.finish();
            }

        });

        messageBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                messageBody = s.toString();
                if (characterCounter != null)
                    characterCounter.setTitle(String.valueOf(maxChars - messageBody.length()));

                if (isTextValid(s.toString())) {
                    send.setEnabled(true);
                    send.setAlpha(1);
                } else {
                    send.setEnabled(false);
                    send.setAlpha(0.5f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

        });

        send.setEnabled(false);
        send.setAlpha(0.5f);

        messageBox.setText(messageBody);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        EditText mEditText = (EditText) findViewById(R.id.editText1);
        mgr.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post_message_menu, menu);
        /*characterCounter = menu.findItem(R.id.characterCount);
        if(messageBox != null) {
            characterCounter.setTitle(String.valueOf(maxChars - messageBody.length()));
        } else {
            characterCounter.setTitle(String.valueOf(maxChars));
        }*/
        return true;
    }

    private boolean isTextValid(String text){
        boolean valid = false;
        for(char c : text.toCharArray()){
            if((c != ' ') && (c != '\n')){
                valid = true;
                break;
            }
        }
        return valid;
    }

    private Location pickBestLocation(Location locationA, Location locationB){

        if(locationA == null && locationB == null){
            return null;
        } else if(locationA == null){
            return locationB;
        } else if(locationB == null){
            return locationA;
        }

        int maxTimeDiff = 1000;
        long timeDiff = Math.abs(locationA.getTime() - locationB.getTime());

        if(locationB.getAccuracy() < locationA.getAccuracy()) {
            //B has better accuracy (lower radius)
            // compare time taken to determine if not too old
            if (timeDiff <= maxTimeDiff) return locationB;
        } else {
            //Even though A is more accurate it is too outdated so b is better choice
            if(locationB.getTime() - locationA.getTime() >= maxTimeDiff){
                return locationB;
            }
        }
        return locationA;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.removeUpdates(listener);
    }
}
