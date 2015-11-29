package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;

/**
 * Created by Liran on 11/26/2015.
 */
public class FeedAdapter extends CursorAdapter {

    private int text_colIndex;
    private int trust_colIndex;
    private int priority_colIndex;
    private int pseudonym_colIndex;

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    private ViewHolder mViewHolder;

    public FeedAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        init(c);
    }

    public FeedAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        init(c);
    }

    private void init(Cursor cursor){
        text_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_MESSAGE);
        trust_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_TRUST);
        priority_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_PRIORITY);
        pseudonym_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_PSEUDONYM);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = inflater.inflate(R.layout.feed_row, parent, false);

        mViewHolder = new ViewHolder();
        mViewHolder.mUpvoteView = (TextView) convertView
                .findViewById(R.id.upvoteView);
        mViewHolder.mTrustView = (TextView) convertView
                .findViewById(R.id.trustView);
        mViewHolder.mHashtagView = (TextView) convertView
                .findViewById(R.id.hashtagView);
        mViewHolder.mNewView = convertView
                .findViewById(R.id.unread_indicator);
        mViewHolder.mExpandView = (ImageView) convertView
                .findViewById(R.id.expand);

        convertView.setTag(mViewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        mViewHolder = (ViewHolder) view.getTag();
        setProperties(mViewHolder, cursor);
    }

    private void setProperties(ViewHolder viewHolder, Cursor cursor){
        viewHolder.mHashtagView.setText(cursor.getString(text_colIndex));
        viewHolder.mUpvoteView.setText(cursor.getString(priority_colIndex));
        viewHolder.mTrustView.setText(cursor.getString(trust_colIndex));
    }

    /**
     * This is used to recycle the views and increase speed of scrolling. This
     * is held by the row object that keeps references to the views so that they
     * do not have to be looked up every time they are populated or reshown.
     */
    static class ViewHolder {
        /** The view object that holds the hashtag for this current row item. */
        private TextView mHashtagView;
        /**
         * The view object that holds the priority score for this current row item.
         */
        private TextView mUpvoteView;
        /**
         * The view object that holds the trust score for this current row item.
         */
        private TextView mTrustView;

        /**
         * The view object that holds the new message indicator for this current row item.
         */
        private View mNewView;

        /**
         * The view object that holds the button to expose the swipe menu
         */
        private ImageView mExpandView;
    }
}
