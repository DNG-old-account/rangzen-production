package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.denovogroup.rangzen.R;

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

        return convertView;
    }
}
