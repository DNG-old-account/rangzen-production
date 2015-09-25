/*
 * Copyright (c) 2014, De Novo Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.denovogroup.rangzen.ui;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.ReadStateTracker;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.backend.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** The adapter used to create the feed list, this adapter either use a set list of items
 * supplied on creation or simply create a detached list object if not supplied additional
 * parameters, this is used to avoid reading from dataset while another thread is writting on it
 * resulting in ConcurrentModificationException when scrolling after receiving messages.
 * the original design of reading directly from the stored dataset for each item is still
 * kept but is not advised when using dynamically changed dataset.
 */
public class FeedListAdapter extends BaseAdapter {

    /** Activity context passed in to the FeedListAdapter. */
    private Context mContext;
    /** Message store to be used to get the messages and trust score. */
    private MessageStore mMessageStore;
    private List<MessageStore.Message> items;
    private HashMap<String,Boolean> unreadItems;

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    private ViewHolder mViewHolder;

    /**
     * Sets the feed text fields to be their values from messages from memory.
     * This finds the correct message at what position and populates recycled
     * views.
     * 
     * @param context
     *            The context of the activity that spawned this class.
     */
    public FeedListAdapter(Context context) {
        this.mContext = context;
        mMessageStore = new MessageStore((Activity) mContext, StorageBase.ENCRYPTION_DEFAULT);
        this.items = mMessageStore.getMessagesContaining("");
        this.unreadItems = ReadStateTracker.getAllUnreadMessages(context);
    }

    /**
     * Sets the feed text fields to be their values from messages from memory.
     * This use the supplied list as an items source and populates recycled
     * views.
     *
     * @param context
     *            The context of the activity that spawned this class.
     * @param items the list of items to be used by this adapter
     */
    public FeedListAdapter(Context context, List<MessageStore.Message> items) {
        this.mContext = context;
        mMessageStore = new MessageStore((Activity) mContext, StorageBase.ENCRYPTION_DEFAULT);
        this.items = (items != null) ? items : mMessageStore.getMessagesContaining("");
        this.unreadItems = ReadStateTracker.getAllUnreadMessages(context);
    }

    @Override
    public int getCount() {
        if(items != null){
            return items.size();
        } else {
            mMessageStore = new MessageStore((Activity) mContext, StorageBase.ENCRYPTION_DEFAULT);
            return mMessageStore.getMessageCount();
        }
    }

    /**
     * Returns the name of the item in the ListView of the NavigationDrawer at
     * this position.
     */
    @Override
    public Object getItem(int position) {
        return "No Name";
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Navigates the treemap and finds the correct message from memory to
     * display at this position in the feed, then returns the row's view object,
     * fully populated with information.
     * 
     * @param position
     *            The current row index in the feed.
     * @param convertView
     *            The view object that contains the row, or null is one has not
     *            been initialized.
     * @param parent
     *            The parent of convertView.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        MessageStore.Message message;

        if(items != null) {
            message = items.get(position);
        } else {
            MessageStore messageStore = new MessageStore((Activity) mContext,
                    StorageBase.ENCRYPTION_DEFAULT);
            message = messageStore.getKthMessage(position);
        }
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.feed_row, parent, false);

            mViewHolder = new ViewHolder();
            mViewHolder.mUpvoteView = (TextView) convertView
                    .findViewById(R.id.upvoteView);
            mViewHolder.mHashtagView = (TextView) convertView
                    .findViewById(R.id.hashtagView);
            mViewHolder.mNewView = convertView
                    .findViewById(R.id.unread_indicator);

            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        String hashtaggedMessage = message.getMessage();

        List<String> hashtags = Utils.getHashtags(message.getMessage());
        for(String hashtag : hashtags){
            String textBefore = hashtaggedMessage.substring(0,hashtaggedMessage.indexOf(hashtag));
            String textAfter = hashtaggedMessage.substring(hashtaggedMessage.indexOf(hashtag)+hashtag.length());
            hashtaggedMessage = textBefore+"<a href=\"org.denovogroup.rangzen://hashtag/"+hashtag+"/\">"+hashtag+"</a>"+textAfter;
        }

        mViewHolder.mHashtagView.setText(Html.fromHtml(hashtaggedMessage));
        mViewHolder.mHashtagView.setLinksClickable(true);
        mViewHolder.mHashtagView.setMovementMethod(LinkMovementMethod.getInstance());

        mViewHolder.mUpvoteView.setText(Integer.toString((int) Math.round(100 * message.getPriority())));

        boolean isUnread = (this.unreadItems == null) ? ReadStateTracker.isRead(message.getMessage()) : unreadItems.containsKey(message.getMessage());
        mViewHolder.mNewView.setVisibility(isUnread ? View.VISIBLE : View.GONE);

        return convertView;
    }

    public List<MessageStore.Message> getItems(){
        return items;
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
         * The view object that holds the trust score for this current row item.
         */
        private TextView mUpvoteView;

        /**
         * The view object that holds the new message indicator for this current row item.
         */
        private View mNewView;
    }
}
