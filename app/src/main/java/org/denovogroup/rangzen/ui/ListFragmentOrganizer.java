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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.backend.Utils;

/**
 * This class is meant to be an organizer for the list views that will be
 * present in Rangzen which are the friends list and feed.
 */
public class ListFragmentOrganizer extends Fragment {

    private static final double PRIORITY_INCREMENT = 0.01d;
    private boolean shouldRefreshList = false;

    /**
     * There are two list Fragments in the ui, the feed and possibly the friends
     * page.
     */
    enum FragmentType {
        FEED, FRIENDS
    }

    /** Creates and populates the content for the feed fragment. */
    private FeedListAdapter mFeedListAdaper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Log.d("Opener", "feed's on create was called");
        Bundle b = getArguments();
        FragmentType whichScreen = (FragmentType) b
                .getSerializable("whichScreen");
        switch (whichScreen) {
        case FEED:
            View view = (View) inflater
                    .inflate(R.layout.feed, container, false);

            ImageView iv = (ImageView) view.findViewById(R.id.normal_image);

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = (int) (size.y);

            Bitmap bd = FragmentOrganizer.decodeSampledBitmapFromResource(getResources(), R.drawable.firstb,
                    width, height);
            BitmapDrawable ob = new BitmapDrawable(bd);
            iv.setBackgroundDrawable(ob);

            final int upvoteItemId = 0;
            final int downvoteItemId = 1;
            final int deleteItemId = 2;

            SwipeMenuListView listView = (SwipeMenuListView) view.findViewById(R.id.list);
            SwipeMenuCreator creator = new SwipeMenuCreator() {

                @Override
                public void create(SwipeMenu menu) {
                    SwipeMenuItem upvoteItem = new SwipeMenuItem(getActivity());
                    upvoteItem.setId(upvoteItemId);
                    upvoteItem.setBackground(new ColorDrawable(Color.parseColor("#b7b7b7")));
                    upvoteItem.setWidth(Utils.dpToPx(60, getActivity()));
                    upvoteItem.setIcon(R.drawable.ic_thumb_up);
                    upvoteItem.getIcon().setAlpha(65);
                    upvoteItem.setTitle(R.string.Upvote);
                    upvoteItem.setTitleSize(14);
                    upvoteItem.setTitleColor(Color.GRAY);
                    menu.addMenuItem(upvoteItem);

                    SwipeMenuItem downvoteItem = new SwipeMenuItem(getActivity());
                    downvoteItem.setId(downvoteItemId);
                    downvoteItem.setBackground(new ColorDrawable(Color.parseColor("#b7b7b7")));
                    downvoteItem.setWidth(Utils.dpToPx(65, getActivity()));
                    downvoteItem.setIcon(R.drawable.ic_thumb_down);
                    downvoteItem.getIcon().setAlpha(60);
                    downvoteItem.setTitle(R.string.Downvote);
                    downvoteItem.setTitleSize(14);
                    downvoteItem.setTitleColor(Color.GRAY);
                    menu.addMenuItem(downvoteItem);

                    SwipeMenuItem deleteItem = new SwipeMenuItem(getActivity());
                    deleteItem.setId(deleteItemId);
                    deleteItem.setBackground(new ColorDrawable(Color.RED));
                    deleteItem.setWidth(Utils.dpToPx(60, getActivity()));
                    deleteItem.setIcon(R.drawable.ic_bin);
                    deleteItem.getIcon().setAlpha(60);
                    menu.addMenuItem(deleteItem);
                }
            };

            listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(int position, SwipeMenu swipeMenu, int index) {
                    MessageStore store = new MessageStore(getActivity(), StorageBase.ENCRYPTION_DEFAULT);
                    MessageStore.Message message = store.getKthMessage(position);
                    boolean updateViewDelayed = false;

                    switch (swipeMenu.getMenuItem(index).getId()) {
                        case upvoteItemId:
                            double oneAbove = 0;
                            double twoAbove = 0;
                            if(position > 0) {
                                MessageStore.Message aboveMessage = store.getKthMessage(position - 1);
                                oneAbove = aboveMessage.getPriority();
                                if(position > 1){
                                    MessageStore.Message twoAboveMessage = store.getKthMessage(position - 2);
                                    twoAbove = twoAboveMessage.getPriority();
                                }
                            }
                            double addedPriority = Math.min(PRIORITY_INCREMENT,
                                    (Math.max(0,oneAbove - message.getPriority())+(Math.max(0,twoAbove-oneAbove))/2));
                            double newHigherPriority = message.getPriority() + ((addedPriority > 0) ? addedPriority : PRIORITY_INCREMENT);
                            store.updatePriority(message.getMessage(), newHigherPriority);
                            updateViewDelayed = true;
                            break;
                        case downvoteItemId:
                            double oneBelow = 0;
                            double twoBelow = 0;
                            if(position < store.getMessageCount() -1 ) {
                                MessageStore.Message belowMessage = store.getKthMessage(position + 1);
                                oneBelow = belowMessage.getPriority();
                                if(position < store.getMessageCount() - 2){
                                    MessageStore.Message twoBelowMessage = store.getKthMessage(position + 2);
                                    twoBelow = twoBelowMessage.getPriority();
                                }
                            }
                            double subtractedPriority = Math.min(PRIORITY_INCREMENT,
                                    (Math.max(0,message.getPriority() - oneBelow)+(Math.max(0,oneBelow - twoBelow))/2));
                            double newLowerPriority = message.getPriority() - ((subtractedPriority > 0) ? subtractedPriority : PRIORITY_INCREMENT);
                            store.updatePriority(message.getMessage(), newLowerPriority);
                            updateViewDelayed = true;
                            break;
                        case deleteItemId:
                            store.deleteMessage(message.getMessage());
                            resetListAdapter(FragmentType.FEED);
                            break;
                    }

                    if(updateViewDelayed){
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                resetListAdapter(FragmentType.FEED);
                            }
                        }, 360);
                    }

                    return false;
                }
            });

            listView.setMenuCreator(creator);

            resetListAdapter(whichScreen);
            return view;
        case FRIENDS:
            View view2 = inflater.inflate(R.layout.feed_row, container, false);
            return view2;
        default:
            return null;
        }
    }


    public void resetListAdapter(FragmentType fragmentType) {
        switch(fragmentType){
            case FEED:
                View v = getView();
                if(v != null){
                    SwipeMenuListView listView = (SwipeMenuListView) v.findViewById(R.id.list);
                    mFeedListAdaper = new FeedListAdapter(getActivity());
                    listView.setAdapter(mFeedListAdaper);
                }
                break;
            case FRIENDS:
                break;
        }
    }
}
