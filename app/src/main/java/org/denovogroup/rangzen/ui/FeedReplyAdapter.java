package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;

/**
 * Created by Liran on 12/30/2015.
 */
public class FeedReplyAdapter extends CursorAdapter {

    private int message_colIndex;
    private int timestamp_colIndex;
    private int pseudonym_colIndex;

    private ViewHolder viewHolder;
    SecurityProfile securityProfile;
    ViewGroup parent;

    String[] colors = new String[]{
            "#ef9a9a",
            "#b39ddb",
            "#9fa8da",
            "#c5e1a5",
            "#c5e1a5",
            "#ffe082",
            "#ffab91"
    };

    public FeedReplyAdapter(Context context, Cursor c) {
        super(context, c, false);
        initAdapter(context, c);
    }

    private void initAdapter(Context context, Cursor cursor){
        securityProfile = org.denovogroup.rangzen.backend.SecurityManager.getCurrentProfile(context);

        message_colIndex = cursor.getColumnIndex(MessageStore.COL_MESSAGE);
        pseudonym_colIndex = cursor.getColumnIndex(MessageStore.COL_PSEUDONYM);
        timestamp_colIndex = cursor.getColumnIndex(MessageStore.COL_TIMESTAMP);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        this.parent = parent;
        View convertView = LayoutInflater.from(context).inflate(R.layout.feed_item_reply, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.deadSpace = convertView.findViewById(R.id.feed_reply_item_dead_space);
        viewHolder.colorTab = convertView.findViewById(R.id.feed_reply_item_color_tab);
        viewHolder.pseudonym = (TextView) convertView.findViewById(R.id.feed_reply_item_pseudonym);
        viewHolder.time = (TextView) convertView.findViewById(R.id.feed_reply_item_time);
        viewHolder.message = (TextView) convertView.findViewById(R.id.feed_reply_item_message);

        convertView.setTag(viewHolder);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        viewHolder = (ViewHolder) view.getTag();

        int position = cursor.getPosition();
        int poolSize = colors.length;
        int offset = (int) Math.floor(position/poolSize);

        if(position == 0){
            viewHolder.deadSpace.getLayoutParams().width = 0;
            viewHolder.deadSpace.invalidate();
            viewHolder.colorTab.setBackground(new ColorDrawable(Color.parseColor(colors[0])));
        } else {
            for (int i = poolSize; i > 0; i--) {
                if ((position - offset * poolSize) % i == 0) {
                    int innerOffset = offset > 0 ? -1 : 0;
                    viewHolder.deadSpace.getLayoutParams().width = viewHolder.colorTab.getLayoutParams().width * (i+innerOffset);
                    viewHolder.deadSpace.invalidate();
                    viewHolder.colorTab.setBackground(new ColorDrawable(Color.parseColor(colors[i+innerOffset])));
                    break;
                }
            }
        }

        viewHolder.message.setText(cursor.getString(message_colIndex));
        String pseudonymString = securityProfile.isPseudonyms() ? cursor.getString(pseudonym_colIndex) : "";
        viewHolder.pseudonym.setText(pseudonymString.length() > 0 ? "@" + pseudonymString : "");

        //get timestamp in proper format
        long timstamp = cursor.getLong(timestamp_colIndex);
        int age = timstamp > 0 ? Utils.convertTimestampToRelativeHours(timstamp) : -1;
        String timestampString = null;
        if(securityProfile.isTimestamp() && age >= 0) {
            if(age == 0) {
                timestampString = context.getString(R.string.just_now);
            }else {
                timestampString = (age < 24) ? age + context.getString(R.string.h_ago) : ((int) Math.floor(age / 24f)) + context.getString(R.string.d_ago);
            }
        }

        viewHolder.time.setText(timestampString != null ? timestampString : "");
    }

    private static class ViewHolder{
        View deadSpace;
        View colorTab;
        TextView pseudonym;
        TextView time;
        TextView message;
    }
}
