package org.denovogroup.rangzen.ui;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.ReadStateTracker;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.backend.Utils;
import org.denovogroup.rangzen.beta.NetworkHandler;
import org.denovogroup.rangzen.beta.ReportsMaker;
import org.json.JSONObject;
import org.denovogroup.rangzen.objects.RangzenMessage;

import java.util.List;

public class FeedFragment extends Fragment implements Refreshable{

    /** the amount to change the priority of a message by upon clicking on upvote/downvote*/
    private static final double PRIORITY_INCREMENT = 0.01d;

    private SwipeMenuListView listView;
    private FeedListAdapter mFeedListAdaper;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.feed, container, false);
        listView = (SwipeMenuListView) view.findViewById(R.id.list);
        setupListView();

        view.findViewById(R.id.new_post_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity().getClass() == Opener.class) {
                    ((Opener) getActivity()).showFragment(1);
                }
            }
        });

        return view;
    }


    /** Create all the necessary components for the swipe menu of the items in the list and assign
     * a list adapter to the feed list view
      */
    private void setupListView(){
        setupListSwipeMenu();
        resetListAdapter(null,false);
    }

    /** Setting the Swipe menu which appears when an item is being swiped to the left
     * this also include assigning the menu buttons with click listeners
     */
    private void setupListSwipeMenu(){

        final int upvoteItemId = 0;
        final int downvoteItemId = 1;
        final int deleteItemId = 2;
        final int retweetItemId = 3;

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem upvoteItem = new SwipeMenuItem(getActivity());
                upvoteItem.setId(upvoteItemId);
                upvoteItem.setBackground(new ColorDrawable(getResources().getColor(R.color.purple)));
                upvoteItem.setWidth(Utils.dpToPx(80, getActivity()));
                upvoteItem.setIcon(R.drawable.ic_thumb_up);
                upvoteItem.getIcon().setAlpha(65);
                upvoteItem.setTitle(R.string.Upvote);
                upvoteItem.setTitleSize(14);
                upvoteItem.setTitleColor(Color.parseColor("#96FFFFFF"));
                menu.addMenuItem(upvoteItem);

                SwipeMenuItem downvoteItem = new SwipeMenuItem(getActivity());
                downvoteItem.setId(downvoteItemId);
                downvoteItem.setBackground(new ColorDrawable(getResources().getColor(R.color.purple)));
                downvoteItem.setWidth(Utils.dpToPx(80, getActivity()));
                downvoteItem.setIcon(R.drawable.ic_thumb_down);
                downvoteItem.getIcon().setAlpha(60);
                downvoteItem.setTitle(R.string.Downvote);
                downvoteItem.setTitleSize(14);
                downvoteItem.setTitleColor(Color.parseColor("#96FFFFFF"));
                menu.addMenuItem(downvoteItem);

                SwipeMenuItem deleteItem = new SwipeMenuItem(getActivity());
                deleteItem.setId(deleteItemId);
                deleteItem.setBackground(new ColorDrawable(Color.RED));
                deleteItem.setWidth(Utils.dpToPx(80, getActivity()));
                deleteItem.setIcon(R.drawable.ic_bin);
                deleteItem.getIcon().setAlpha(60);
                menu.addMenuItem(deleteItem);

                SwipeMenuItem retweetItem = new SwipeMenuItem(getActivity());
                retweetItem.setId(retweetItemId);
                retweetItem.setBackground(new ColorDrawable(Color.parseColor("#49b4d3")));
                retweetItem.setWidth(Utils.dpToPx(80, getActivity()));
                retweetItem.setIcon(R.drawable.ic_retweet);
                retweetItem.getIcon().setAlpha(80);
                menu.addMenuItem(retweetItem);
            }
        };

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final int position, SwipeMenu swipeMenu, int index) {
                MessageStore store = new MessageStore(getActivity(), StorageBase.ENCRYPTION_DEFAULT);
                MessageStore.Message message = (mFeedListAdaper.getItems() == null) ? store.getKthMessage(position) : mFeedListAdaper.getItems().get(position);
                boolean updateViewDelayed = false;
                switch (swipeMenu.getMenuItem(index).getId()) {
                    case upvoteItemId:
						//BETA
                        double oldLowPriority = message.getPriority();
                        /*TODO implement the following
                        double nextUpPriority;
                        if(mFeedListAdaper.getItems() != null) {
                            nextUpPriority = (position > 0 && mFeedListAdaper.getItems().get(position - 1).getPriority() == message.getPriority()) ? message.getPriority() : getNextPriority(true, message, position);
                        } else {
                            nextUpPriority = (position > 0 && store.getKthMessage(position - 1).getPriority() == message.getPriority()) ? message.getPriority() : getNextPriority(true, message, position);
                        }

                        if(nextUpPriority == message.getPriority()){
                            //change order within same priority group
                            store.updatePositionInBin(message.getMessage(), true);
                        } else {
                            //change priority
                            store.updatePriority(message.getMessage(), nextUpPriority);
                        }*/
                        store.updatePriority(message.getMessage(), getNextPriority(true, message, position), true, message.getMId());
                        updateViewDelayed = true;
                        //BETA
                        JSONObject report = ReportsMaker.getMessagePriorityChangedByUserReport(System.currentTimeMillis(), message.getMId(), oldLowPriority, message.getPriority(), message.getMessage());
                        NetworkHandler.getInstance(getActivity()).sendEventReport(report);
                        //BETA END
                        break;
                    case downvoteItemId:
						//BETA
                        double oldHighPriority = message.getPriority();
                        /*TODO implement the following
                        double nextDownPriority;
                        if(mFeedListAdaper.getItems() != null) {
                            nextDownPriority = (position < mFeedListAdaper.getCount() - 2 && mFeedListAdaper.getItems().get(position + 1).getPriority() == message.getPriority()) ? message.getPriority() : getNextPriority(false, message, position);
                        } else {
                            nextDownPriority = (position < store.getMessageCount() - 2 && store.getKthMessage(position + 1).getPriority() == message.getPriority()) ? message.getPriority() : getNextPriority(false, message, position);
                        }

                        if(nextDownPriority == message.getPriority()){
                            //change order within same priority group
                            store.updatePositionInBin(message.getMessage(), true);
                        } else {
                            //change priority
                            store.updatePriority(message.getMessage(), nextDownPriority);
                        }*/
                        store.updatePriority(message.getMessage(), getNextPriority(false, message, position), true, message.getMId());
                        updateViewDelayed = true;
                        //BETA
                        JSONObject report2 = ReportsMaker.getMessagePriorityChangedByUserReport(System.currentTimeMillis(), message.getMId(), oldHighPriority, message.getPriority(), message.getMessage());
                        NetworkHandler.getInstance(getActivity()).sendEventReport(report2);
                        //BETA END
                        break;
                    case deleteItemId:
                        //BETA
                        JSONObject report3 = ReportsMaker.getMessageDeletedReport(System.currentTimeMillis(), message.getMId(), message.getPriority(), message.getMessage());
                        NetworkHandler.getInstance(getActivity()).sendEventReport(report3);
                        //BETA END

                        //delete data from storage
                        store.removeMessage(message.getMessage(), message.getMId());

                        /*If data is currently being presenting as filtered search results, update
                          the currently displayed to retain consistent look */
                        List<MessageStore.Message> updatedList = mFeedListAdaper.getItems();
                        if (updatedList != null) {
                            updatedList.remove(position);
                        }
                        //refresh listview
                        resetListAdapter(updatedList,true);
                        break;
                    case retweetItemId:
                        //BETA
                        JSONObject report4 = ReportsMaker.getMessageReweetedReport(System.currentTimeMillis(), message.getMId(), message.getPriority(), message.getMessage());
                        NetworkHandler.getInstance().sendEventReport(report4);
                        //BETA

                        store.updatePriority(message.getMessage(), 1d, true, message.getMId());
                        updateViewDelayed = true;
                        break;
                }

                if (updateViewDelayed) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            /*If data is currently being presenting as filtered search results, update
                                    the currently displayed to retain consistent look */
                            List<MessageStore.Message> updatedList = mFeedListAdaper.getItems();
                            if (updatedList != null) {
                                MessageStore store = new MessageStore(getActivity(), StorageBase.ENCRYPTION_DEFAULT);
                                double oldPri = updatedList.get(position).getPriority();
                                updatedList.get(position).setPriority(store.getPriority(updatedList.get(position).getMessage()));
                                updatedList = sortDetachedList(updatedList);
                            }
                            //refresh the listView
                            resetListAdapter(updatedList,true);
                        }
                    }, 360);
                }

                return false;
            }
        });

        listView.setMenuCreator(creator);
    }

    /** set a feed list adapter to the list using the supplied list as source or using the default
     * items set if supplied list is null
     * @param items List of messages to be displayed by the attached listView or null to set
     * @param keepApperance whether or not the view should retain its relative position before updating
     * default list of items to the adapter*/
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)//this line is required to overcome a bug in the documentation, the problematic call is actually available since API 1 not 21
    public void resetListAdapter(List<MessageStore.Message> items, Boolean keepApperance) {

        mFeedListAdaper = new FeedListAdapter(getActivity(), items);
        if (listView != null) {
            int position = listView.getFirstVisiblePosition();
            int offset = (listView.getChildAt(0) != null) ? listView.getChildAt(0).getTop() : 0;

            listView.setAdapter(mFeedListAdaper);
            if(keepApperance && listView.getFirstVisiblePosition() > -1) {
                listView.setSelectionFromTop(position,offset);
            }
        }
    }

    @Override
    public void refreshView(List<?> items) {
        resetListAdapter((List<MessageStore.Message>) items, false);
    }

    /** Create a copy of the provided list and sort it based on the message priority
     *
      * @param list the list to be cloned and sorted
     * @return a sorted clone of the source list, sorted by priority
     */
    private List<MessageStore.Message> sortDetachedList(List<MessageStore.Message> list){

        List<MessageStore.Message> sortedList = list;
        for(MessageStore.Message m : list){
            while(sortedList.indexOf(m) > 0 && sortedList.get(sortedList.indexOf(m)-1).getPriority() < m.getPriority()) {

                int position = sortedList.indexOf(m);
                MessageStore.Message m1 = m;
                MessageStore.Message m2 = sortedList.get(sortedList.indexOf(m)-1);

                sortedList.set(position, m2);
                sortedList.set(position-1, m1);
            }
        }

        return sortedList;
    }

    /** Calculate the next available priority in the currently displayed list
     *
     * @param up a booean value representing if the priority should go up or down
     * @param message the message to have its priority checked
     * @param position the position of the message item in the dataset
     * @return
     */
    private double getNextPriority(boolean up, MessageStore.Message message, int position){

        MessageStore store = new MessageStore(getActivity(),StorageBase.ENCRYPTION_DEFAULT);
        MessageStore.Message currentlyEvaluatedMessage;
        /** The message one priority slot away from the supplied message based on the direction of the update*/
        MessageStore.Message m1 = null;
        /** The message two priority slots away from the supplied message based on the direction of the update*/
        MessageStore.Message m2 = null;
        double newPriority;

        if(up){
            //upvote
            int iteratedPos = position-1;
            while (iteratedPos >= 0 && message.getPriority() < 1) {

                currentlyEvaluatedMessage = (mFeedListAdaper.getItems() == null) ? store.getKthMessage(iteratedPos) : mFeedListAdaper.getItems().get(iteratedPos);

                if(m1 == null){
                    if(currentlyEvaluatedMessage.getPriority() >= message.getPriority()){
                        m1 = currentlyEvaluatedMessage;
                    }
                } else if(m2 == null){
                    if(currentlyEvaluatedMessage.getPriority() > m1.getPriority()) {
                        m2 = currentlyEvaluatedMessage;
                    }
                } else{
                    break;
                }
                iteratedPos--;
            }

            if(m2 != null) {
                newPriority = (m2.getPriority()+m1.getPriority())/2;
            } else if(m1 != null && m1.getPriority() < 1) {
                newPriority = (m1.getPriority() + 1) / 2;
            } else {
                newPriority = 1;
            }
        } else{
            //downvote
            int iteratedPos = position+1;
            while (iteratedPos < mFeedListAdaper.getCount() && message.getPriority() > 0.01d) {

                currentlyEvaluatedMessage = (mFeedListAdaper.getItems() == null) ? store.getKthMessage(iteratedPos) : mFeedListAdaper.getItems().get(iteratedPos);

                if(m1 == null){
                    if(currentlyEvaluatedMessage.getPriority() <= message.getPriority()){
                        m1 = currentlyEvaluatedMessage;
                    }
                } else if(m2 == null){
                    if(currentlyEvaluatedMessage.getPriority() < m1.getPriority()) {
                        m2 = currentlyEvaluatedMessage;
                    }
                } else{
                    break;
                }
                iteratedPos++;
            }

            if(m2 != null) {
                newPriority = (m2.getPriority()+m1.getPriority())/2;
            } else if(m1 != null && m1.getPriority() > 0.01d) {
                newPriority = (m1.getPriority() + 0.01d) / 2;
            } else {
                newPriority = 0.01d;
            }
        }

        return newPriority;
    }
}
