package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.Utils;

import java.util.Calendar;

/**
 * Created by Liran on 12/7/2015.
 */
public class SearchActivity extends ActionBarActivity {

    public static final String SEARCH_EXTRA = "search_extra";

    EditText message;
    EditText likes;
    EditText trust;
    Button date;
    Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        message = (EditText) findViewById(R.id.message_field);
        likes = (EditText) findViewById(R.id.likes_field);
        trust = (EditText) findViewById(R.id.trust_field);
        date = (Button) findViewById(R.id.date_button);
        selectedDate = Utils.reduceCalendar(Calendar.getInstance());
        date.setText(Utils.convertTimestampToDateStringCompact(false, selectedDate.getTimeInMillis()));
        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
                builder.setTitle(R.string.select_date);
                final View dialogView = getLayoutInflater().inflate(R.layout.select_date_dialog, null);
                builder.setView(dialogView);
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CalendarView calendarView = (CalendarView) dialogView.findViewById(R.id.calendarView);
                        selectedDate.setTimeInMillis(calendarView.getDate());
                        selectedDate = Utils.reduceCalendar(selectedDate);
                        date.setText(Utils.convertTimestampToDateStringCompact(false, selectedDate.getTimeInMillis()));
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.search){

            String rawQuery = "";

            if(message.getText().length() > 0){
                rawQuery += MessageStore.COL_MESSAGE+":"+message.getText().toString();
            }
            if(likes.getText().length() > 0){
                try {
                    if (Integer.parseInt(likes.getText().toString()) > 0) {
                        rawQuery += ((rawQuery.length() > 0) ? " " : "") + MessageStore.COL_LIKES + ":" + likes.getText().toString();
                    }
                } catch (Exception e){}
            }
            if(trust.getText().length() > 0){
                try {
                    if (Float.parseFloat(trust.getText().toString()) > 0) {
                        rawQuery += ((rawQuery.length() > 0) ? " " : "") + MessageStore.COL_TRUST + ":" + trust.getText().toString();
                    }
                } catch (Exception e){}
            }
            if(selectedDate.getTimeInMillis() < Utils.reduceCalendar(Calendar.getInstance()).getTimeInMillis()){
                rawQuery += ((rawQuery.length() > 0) ? " " : "") + MessageStore.COL_TIMESTAMP + ":" + Utils.convertTimestampToDateStringCompact(false, selectedDate.getTimeInMillis());
            }

            Intent intent = new Intent();
            intent.putExtra(SEARCH_EXTRA, rawQuery);
            setResult(RESULT_OK, intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
