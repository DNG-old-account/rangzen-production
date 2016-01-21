package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.denovogroup.rangzen.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Liran on 1/19/2016.
 */
public class DebugAdapter extends ArrayAdapter<String[]>{
    public DebugAdapter(Context context, List<String[]> objects) {
        super(context, R.layout.debug_list_item, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.debug_list_item, parent, false);
        }

        String[] currentItem = getItem(position);

        ((TextView) convertView.findViewById(R.id.peer_mac)).setText(currentItem[0]);
        ((TextView) convertView.findViewById(R.id.peer_backoff)).setText(currentItem[2] != null ? currentItem[2] : "No backoff");

        boolean speakTo = currentItem[1] != null;

        convertView.findViewById(R.id.speak_to).setVisibility(speakTo ? View.VISIBLE : View.GONE);
        convertView.findViewById(R.id.spoken_of).setVisibility(!speakTo ? View.VISIBLE : View.GONE);

        long lastExchangeTime = currentItem[3] != null ? Long.parseLong(currentItem[3]) : -1;

        String lastExchangeTimeString = "Last exchange:";

        if(lastExchangeTime < 0){
            lastExchangeTimeString+=" Unknown";
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(lastExchangeTime);
            SimpleDateFormat format = new SimpleDateFormat("h:mm:ss a");
            lastExchangeTimeString += format.format(cal.getTime());
        }

        ((TextView)convertView.findViewById(R.id.last_exchange)).setText(lastExchangeTimeString);

        return convertView;
    }
}
