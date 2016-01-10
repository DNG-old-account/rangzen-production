package org.denovogroup.rangzen.uiold;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;

import java.util.List;

/**
 * Created by Liran on 11/26/2015.
 */
public class FeedAdapter extends CursorAdapter {

    private int text_colIndex;
    private int trust_colIndex;
    private int priority_colIndex;
    private int liked_colIndex;
    private int pseudonym_colIndex;
    private int timestamp_colIndex;
    private int timebound_colIndex;
    private int read_colIndex;
    private int location_colIndex;
    private int parent_colIndex;
    private int messageId_colIndex;

    private SecurityProfile currentProfile;

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    private ViewHolder mViewHolder;
    private FeedAdapterCallbacks callbacks;

    private ListView listView;
    private Context context;
    int resourceId = -1;

    public interface FeedAdapterCallbacks{
        void onDelete(String message);
        void onDeleteMany(String message, float trust, String pseudonym, int likes);
        void onUpvote(String message, int oldPriority);
        void onDownvote(String message, int oldPriority);
        void onRetweet(String message);
        void onShare(String message);
        void onTimeboundClick(String message, long timebound);
        void onNavigate(String message, String latxLon);
        void onReply(String messageId);
    }

    public FeedAdapter(Context context, Cursor c, boolean autoRequery, int resourceId) {
        super(context, c, autoRequery);
        init(context, c, resourceId);
    }

    public FeedAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        init(context, c, -1);
    }

    public FeedAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        init(context, c, -1);
    }

    private void init(Context context,Cursor cursor, int resourceId){
        this.context = context;
        this.resourceId = resourceId == -1 ? R.layout.feed_item : resourceId;
        text_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_MESSAGE);
        trust_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_TRUST);
        priority_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_LIKES);
        liked_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_LIKED);
        pseudonym_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_PSEUDONYM);
        timestamp_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_TIMESTAMP);
        read_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_READ);
        timebound_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_EXPIRE);
        location_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_LATLONG);
        currentProfile = org.denovogroup.rangzen.backend.SecurityManager.getCurrentProfile(context);
        parent_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_PARENT);
        messageId_colIndex = cursor.getColumnIndexOrThrow(MessageStore.COL_MESSAGE_ID);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        listView = (ListView) parent;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = inflater.inflate(resourceId, parent, false);

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
        mViewHolder.btn_share = (ImageButton) convertView
                .findViewById(R.id.item_share);
        mViewHolder.timebound_marker = convertView
                .findViewById(R.id.timebound_marker);
        mViewHolder.btn_navigate = (ImageButton) convertView
                .findViewById(R.id.item_navigate);
        mViewHolder.btn_reply = (ImageButton) convertView
                .findViewById(R.id.item_reply);
        mViewHolder.comments_holder = (ListView) convertView
                .findViewById(R.id.comments_holder);
        convertView.setTag(mViewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        mViewHolder = (ViewHolder) view.getTag();
        setProperties(view, mViewHolder, cursor);
    }

    private void setProperties(View view, ViewHolder viewHolder, Cursor cursor){

        boolean isRead = cursor.getInt(read_colIndex) == MessageStore.TRUE;
        boolean isComment = cursor.getString(parent_colIndex) != null;

        //view.setBackgroundResource(isRead ? R.drawable.feed_item_background_gradient : R.drawable.feed_item_background_gradient_unread);
        //view.setBackgroundResource(isComment ? R.drawable.feed_item_comment_background_gradient : R.drawable.feed_item_background_gradient);

        viewHolder.mPriorityView.setText(cursor.getString(priority_colIndex));
        viewHolder.mTrustView.setText(String.valueOf(cursor.getFloat(trust_colIndex) * 100f));

        setHashtagLinks(viewHolder.mTextView, cursor.getString(text_colIndex));

        viewHolder.mPseudonymView.setText(currentProfile.isPseudonyms() ?
                cursor.getString(pseudonym_colIndex) : "");
        long timstamp = currentProfile.isTimestamp() ?
                cursor.getLong(timestamp_colIndex) : 0;
        int age = timstamp > 0 ? Utils.convertTimestampToRelativeDays(timstamp) : -1;
        if(age >= 0) {
            viewHolder.mTimestampView.setText(age > 0 ? age+" ago" : "today");
        } else {
            viewHolder.mTimestampView.setText("");
        }

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
                long timebound = getCursor().getLong(timebound_colIndex);
                String location = getCursor().getString(location_colIndex);
                String messageId = getCursor().getString(messageId_colIndex);

                getCursor().moveToPosition(cursorPosition);

                switch (v.getId()){
                    case R.id.item_delete:
                        if(callbacks!= null) callbacks.onDelete(message);
                        break;
                    case R.id.item_like:
                        if(callbacks!= null) callbacks.onUpvote(message, priority);
                        break;
                    case R.id.item_dislike:
                        if(callbacks!= null) callbacks.onDownvote(message, priority);
                        break;
                    case R.id.item_retweet:
                        if(callbacks!= null) callbacks.onRetweet(message);
                        break;
                    case R.id.item_share:
                        if(callbacks!= null) callbacks.onShare(message);
                        break;
                    case R.id.timebound_marker:
                        if(callbacks != null) callbacks.onTimeboundClick(message, timebound);
                        break;
                    case R.id.item_navigate:
                        if(callbacks != null) callbacks.onNavigate(message, location);
                        break;
                    case R.id.item_reply:
                        if(callbacks != null) callbacks.onReply(messageId);
                        break;
                }
            }
        };

        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if(listView == null) return false;

                int containerIndex = -1;

                for(int i=0; i<listView.getChildCount();i++){
                    if(listView.getChildAt(i).findViewById(v.getId()) == v){
                        containerIndex = i;
                        break;
                    }
                }
                if(containerIndex < 0) return false;

                int cursorPosition = getCursor().getPosition();
                getCursor().moveToPosition(listView.getFirstVisiblePosition()+containerIndex);

                String message = getCursor().getString(text_colIndex);
                float trust = getCursor().getFloat(trust_colIndex);
                String pseudoynm = getCursor().getString(pseudonym_colIndex);
                int likes = getCursor().getInt(priority_colIndex);

                getCursor().moveToPosition(cursorPosition);

                switch(v.getId()){
                    case R.id.item_delete:
                        if(callbacks!= null) callbacks.onDeleteMany(message, trust, pseudoynm, likes);
                        break;
                }
                return true;
            }
        };

        boolean isLiked = cursor.getInt(liked_colIndex) == MessageStore.TRUE;
        boolean isTimebound = cursor.getLong(timebound_colIndex) > 0;
        boolean hasLocation = false;
        if(currentProfile.isShareLocation()){
            String location = cursor.getString(location_colIndex);
            hasLocation = (location != null && location.indexOf("x")> 0);
        }

        viewHolder.btn_delete.setOnClickListener(clickListener);
        viewHolder.btn_delete.setOnLongClickListener(longClickListener);
        viewHolder.btn_like.setOnClickListener(clickListener);
        viewHolder.btn_like.setVisibility(isLiked ? View.GONE : View.VISIBLE);
        viewHolder.btn_dislike.setOnClickListener(clickListener);
        viewHolder.btn_dislike.setVisibility(isLiked ? View.VISIBLE : View.GONE);
        viewHolder.btn_retweet.setOnClickListener(clickListener);
        viewHolder.btn_share.setOnClickListener(clickListener);
        viewHolder.timebound_marker.setOnClickListener(clickListener);
        viewHolder.timebound_marker.setVisibility(isTimebound ? View.VISIBLE : View.GONE);
        viewHolder.btn_navigate.setOnClickListener(clickListener);
        viewHolder.btn_navigate.setVisibility(hasLocation ? View.VISIBLE : View.GONE);
        if(viewHolder.btn_reply != null) viewHolder.btn_reply.setOnClickListener(clickListener);

        if(viewHolder.comments_holder != null) {
            if (!isComment) {
                Cursor commentsCursor = MessageStore.getInstance().getComments(cursor.getString(messageId_colIndex));
                if (commentsCursor != null && commentsCursor.getCount() > 0) {
                    viewHolder.comments_holder.setAdapter(new FeedAdapter(context, commentsCursor, false, R.layout.feed_item_comment));
                } else {
                    viewHolder.comments_holder.setAdapter(null);
                }
            } else {
                viewHolder.comments_holder.setAdapter(null);
            }
        }
    }

    /** set hashtag links to any hashtag in the supplied text and assign the text to the supplied
     * text view, clicking a hashtag will call new instance of the activity with intent filter of
     * //hashtag
     * @param textView
     * @param source
     */
    private void setHashtagLinks(TextView textView, String source){
        String hashtaggedMessage = source;

        List<String> hashtags = Utils.getHashtags(source);
        for(String hashtag : hashtags){
            String textBefore = hashtaggedMessage.substring(0,hashtaggedMessage.indexOf(hashtag));
            String textAfter = hashtaggedMessage.substring(hashtaggedMessage.indexOf(hashtag)+hashtag.length());
            hashtaggedMessage = textBefore+"<a href=\"org.denovogroup.rangzen://hashtag/"+hashtag+"/\">"+hashtag+"</a>"+textAfter;
        }

        textView.setText(Html.fromHtml(hashtaggedMessage));
        textView.setLinksClickable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
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
        /**
         * The view object that act as share button.
         */
        private ImageButton btn_share;
        /**
         * The view object that act as a marker for timebound messages.
         */
        private View timebound_marker;
        /**
         * The view object that act as navigate button.
         */
        private ImageButton btn_navigate;
        /**
         * The view object that act as reply button.
         */
        private ImageButton btn_reply;
        /**
         * The view object that act as navigate button.
         */
        private ListView comments_holder;
    }

    public void setAdapterCallbacks(FeedAdapterCallbacks callbacks){
        this.callbacks = callbacks;
    }
}