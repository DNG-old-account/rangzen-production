package org.denovogroup.rangzen.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

/** This activity crates the message sending page. It also handles back button. */
public class PostActivity extends ActionBarActivity {

    public static final String MESSAGE_BODY = "MESSAGE_BODY";

    MenuItem characterCounter;

    private int maxChars = 140;
    private EditText messageBox;
    String messageBody = "";

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        if(getIntent().hasExtra(MESSAGE_BODY)){
            messageBody = getIntent().getStringExtra(MESSAGE_BODY);
        }

        setContentView(R.layout.makepost);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Post");

        messageBox = (EditText) findViewById(R.id.editText1);
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
                        System.currentTimeMillis() :0;
                messageStore.addMessage(messageBody, trust, priority, pseudonym, timestamp, true);
                Toast.makeText(PostActivity.this, "Message sent!",
                        Toast.LENGTH_SHORT).show();
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
        characterCounter = menu.findItem(R.id.characterCount);
        if(messageBox != null) {
            characterCounter.setTitle(String.valueOf(maxChars - messageBody.length()));
        } else {
            characterCounter.setTitle(String.valueOf(maxChars));
        }
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
}
