package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

/**
 * Created by Liran on 11/26/2015.
 */
public class FeedAdapter extends CursorAdapter {

    private int text_colIndex;
    private int trust_colIndex;
    private int priority_colIndex;
    private int pseudonym_colIndex;
    private int timestamp_colIndex;

    private SecurityProfile currentProfile;

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    private ViewHolder mViewHolder;
    private FeedAdapterCallbacks callbacks;

    private ListView listView;

    public interface FeedAdapterCallbacks{
        void onDelete(String message);
        void onUpvote(String message, int oldPriority);
        void onDownvote(String message, int oldPriority);
        void onRetweet(String message);
    }

    public FeedAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        init(context, c);
    }

    public FeedAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        init(context, c);
    }

    private void init(Context context,Cursor cursor){
        text_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_MESSAGE);
        trust_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_TRUST);
        priority_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_PRIORITY);
        pseudonym_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_PSEUDONYM);
        timestamp_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_TIMESTAMP);
        currentProfile = org.denovogroup.rangzen.backend.SecurityManager.getCurrentProfile(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        listView = (ListView) parent;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = inflater.inflate(R.layout.feed_item, parent, false);

        mViewHolder = new ViewHolder();
        mViewHolder.mPriorityView = (TextView) convertView
                .findViewById(R.id.item_priority);
        mViewHolder.mTrustView = (TextView) convertView
                .findViewById(R.id.item_trust);
        mViewHolder.mPseudonymView = (TextView) convertView
                .findViewById(R.id.item_pseudonym);
        mViewHolder.mTimestampView = (TextView) convertView
                .findViewById(R.id.item_date);
        mViewHolder.mTextView = (TextView) convertView
                .findViewById(R.id.item_text);
        mViewHolder.btn_delete = (ImageButton) convertView
                .findViewById(R.id.item_delete);
        mViewHolder.btn_like = (ImageButton) convertView
                .findViewById(R.id.item_like);
        mViewHolder.btn_dislike = (ImageButton) convertView
                .findViewById(R.id.item_dislike);
        mViewHolder.btn_retweet = (ImageButton) convertView
                .findViewById(R.id.item_retweet);
        convertView.setTag(mViewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        mViewHolder = (ViewHolder) view.getTag();
        setProperties(view, mViewHolder, cursor);
    }

    private void setProperties(View view, ViewHolder viewHolder, Cursor cursor){

        boolean isRead = ReadStateTracker.isRead(cursor.getString(text_colIndex));

        view.setBackgroundResource(isRead ? R.drawable.feed_item_background_gradient : R.drawable.feed_item_background_gradient_unread);

        viewHolder.mPriorityView.setText(cursor.getString(priority_colIndex));
        viewHolder.mTrustView.setText(String.valueOf(cursor.getFloat(trust_colIndex)*100));
        viewHolder.mTextView.setText(cursor.getString(text_colIndex));
        viewHolder.mPseudonymView.setText(currentProfile.isPseudonyms() ?
                cursor.getString(pseudonym_colIndex) : "");
        viewHolder.mTimestampView.setText(currentProfile.isTimestamp() ?
                cursor.getString(timestamp_colIndex) : "");

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(listView == null) return;

                int containerIndex = -1;

                for(int i=0; i<listView.getChildCount();i++){
                    if(listView.getChildAt(i).findViewById(v.getId()) == v){
                        containerIndex = i;
                        break;
                    }
                }
                if(containerIndex < 0) return;

                int cursorPosition = getCursor().getPosition();
                getCursor().moveToPosition(listView.getFirstVisiblePosition()+containerIndex);

                String message = getCursor().getString(text_colIndex);
                int priority = getCursor().getInt(priority_colIndex);

                getCursor().moveToPosition(cursorPosition);

                switch (v.getId()){
                    case R.id.item_delete:
                        if(callbacks!= null) callbacks.onDelete(message);
                        break;
                    case R.id.item_like:
                        if(callbacks!= null) callbacks.onUpvote(message,priority);
                        break;
                    case R.id.item_dislike:
                        if(callbacks!= null) callbacks.onDownvote(message,priority);
                        break;
                    case R.id.item_retweet:
                        if(callbacks!= null) callbacks.onRetweet(message);
                        break;
                }
            }
        };

        viewHolder.btn_delete.setOnClickListener(clickListener);
        viewHolder.btn_like.setOnClickListener(clickListener);
        viewHolder.btn_dislike.setOnClickListener(clickListener);
        viewHolder.btn_retweet.setOnClickListener(clickListener);
    }

    /**
     * This is used to recycle the views and increase speed of scrolling. This
     * is held by the row object that keeps references to the views so that they
     * do not have to be looked up every time they are populated or reshown.
     */
    static class ViewHolder {
        /** The view object that holds the text for this current row item. */
        private TextView mTextView;
        /**
         * The view object that holds the priority score for this current row item.
         */
        private TextView mPriorityView;
        /**
         * The view object that holds the trust score for this current row item.
         */
        private TextView mTrustView;
        /**
         * The view object that holds the pseudonym for this current row item.
         */
        private TextView mPseudonymView;
        /**
         * The view object that holds the timestamp for this current row item.
         */
        private TextView mTimestampView;
        /**
         * The view object that act as delete button.
         */
        private ImageButton btn_delete;
        /**
         * The view object that act as like button.
         */
        private ImageButton btn_like;
        /**
         * The view object that act as dislike button.
         */
        private ImageButton btn_dislike;
        /**
         * The view object that act as retween button.
         */
        private ImageButton btn_retweet;
    }

    public void setAdapterCallbacks(FeedAdapterCallbacks callbacks){
        this.callbacks = callbacks;
    }
}
