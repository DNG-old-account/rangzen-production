package org.denovogroup.rangzen.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.backend.Utils;
import org.denovogroup.rangzen.beta.NetworkHandler;
import org.denovogroup.rangzen.beta.ReportsMaker;
import org.json.JSONObject;

import java.util.Collections;
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

        return view;
    }


    /** Create all the necessary components for the swipe menu of the items in the list and assign
     * a list adapter to the feed list view
      */
    private void setupListView(){
        setupListSwipeMenu();
        resetListAdapter(null);
    }

    /** Setting the Swipe menu which appears when an item is being swiped to the left
     * this also include assigning the menu buttons with click listeners
     */
    private void setupListSwipeMenu(){

        final int upvoteItemId = 0;
        final int downvoteItemId = 1;
        final int deleteItemId = 2;

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem upvoteItem = new SwipeMenuItem(getActivity());
                upvoteItem.setId(upvoteItemId);
                upvoteItem.setBackground(new ColorDrawable(Color.parseColor("#b7b7b7")));
                upvoteItem.setWidth(Utils.dpToPx(80, getActivity()));
                upvoteItem.setIcon(R.drawable.ic_thumb_up);
                upvoteItem.getIcon().setAlpha(65);
                upvoteItem.setTitle(R.string.Upvote);
                upvoteItem.setTitleSize(14);
                upvoteItem.setTitleColor(Color.GRAY);
                menu.addMenuItem(upvoteItem);

                SwipeMenuItem downvoteItem = new SwipeMenuItem(getActivity());
                downvoteItem.setId(downvoteItemId);
                downvoteItem.setBackground(new ColorDrawable(Color.parseColor("#b7b7b7")));
                downvoteItem.setWidth(Utils.dpToPx(80, getActivity()));
                downvoteItem.setIcon(R.drawable.ic_thumb_down);
                downvoteItem.getIcon().setAlpha(60);
                downvoteItem.setTitle(R.string.Downvote);
                downvoteItem.setTitleSize(14);
                downvoteItem.setTitleColor(Color.GRAY);
                menu.addMenuItem(downvoteItem);

                SwipeMenuItem deleteItem = new SwipeMenuItem(getActivity());
                deleteItem.setId(deleteItemId);
                deleteItem.setBackground(new ColorDrawable(Color.RED));
                deleteItem.setWidth(Utils.dpToPx(80, getActivity()));
                deleteItem.setIcon(R.drawable.ic_bin);
                deleteItem.getIcon().setAlpha(60);
                menu.addMenuItem(deleteItem);
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
                        double oneAbove = 0;
                        double twoAbove = 0;
                        if (position > 0) {
                            MessageStore.Message aboveMessage = (mFeedListAdaper.getItems() == null) ? store.getKthMessage(position - 1) : mFeedListAdaper.getItems().get(position-1);
                            oneAbove = aboveMessage.getPriority();
                            if (position > 1) {
                                MessageStore.Message twoAboveMessage = (mFeedListAdaper.getItems() == null) ? store.getKthMessage(position - 2) : mFeedListAdaper.getItems().get(position-2);
                                twoAbove = twoAboveMessage.getPriority();
                            }
                        }
                        double addedPriority = Math.min(PRIORITY_INCREMENT,
                                (Math.max(0, oneAbove - message.getPriority()) + (Math.max(0, twoAbove - oneAbove)) / 2));
                        double newHigherPriority = message.getPriority() + ((addedPriority > 0) ? addedPriority : PRIORITY_INCREMENT);
                        //BETA
                        JSONObject report = ReportsMaker.getMessagePriorityChangedByUserReport(System.currentTimeMillis(), message.getMId(),message.getPriority(),newHigherPriority,message.getMessage());
                        NetworkHandler.getInstance(getActivity()).sendEventReport(report);
                        //BETA END
                        store.updatePriority(message.getMessage(), newHigherPriority);
                        updateViewDelayed = true;
                        break;
                    case downvoteItemId:
                        double oneBelow = 0;
                        double twoBelow = 0;
                        if (position < mFeedListAdaper.getCount() - 1) {
                            MessageStore.Message belowMessage = (mFeedListAdaper.getItems() == null) ? store.getKthMessage(position + 1) : mFeedListAdaper.getItems().get(position + 1);
                            oneBelow = belowMessage.getPriority();
                            if (position < mFeedListAdaper.getCount() - 2) {
                                MessageStore.Message twoBelowMessage = (mFeedListAdaper.getItems() == null) ? store.getKthMessage(position + 2) : mFeedListAdaper.getItems().get(position+2);
                                twoBelow = twoBelowMessage.getPriority();
                            }
                        }
                        double subtractedPriority = Math.min(PRIORITY_INCREMENT,
                                (Math.max(0, message.getPriority() - oneBelow) + (Math.max(0, oneBelow - twoBelow)) / 2));
                        double newLowerPriority = message.getPriority() - ((subtractedPriority > 0) ? subtractedPriority : PRIORITY_INCREMENT);
                        //BETA
                        JSONObject report2 = ReportsMaker.getMessagePriorityChangedByUserReport(System.currentTimeMillis(), message.getMId(),message.getPriority(),newLowerPriority,message.getMessage());
                        NetworkHandler.getInstance(getActivity()).sendEventReport(report2);
                        //BETA END
                        store.updatePriority(message.getMessage(), newLowerPriority);
                        updateViewDelayed = true;
                        break;
                    case deleteItemId:
                        //BETA
                        JSONObject report3 = ReportsMaker.getMessageDeletedReport(System.currentTimeMillis(), message.getMId(), message.getPriority(), message.getMessage());
                        NetworkHandler.getInstance(getActivity()).sendEventReport(report3);
                        //BETA END

                        //delete data from storage
                        store.deleteMessage(message.getMessage());

                        /*If data is currently being presenting as filtered search results, update
                          the currently displayed to retain consistent look */
                        List<MessageStore.Message> updatedList = mFeedListAdaper.getItems();
                        if (updatedList != null) {
                            updatedList.remove(position);
                        }
                        //refresh listview
                        resetListAdapter(updatedList);
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
                                Log.d("liran", "updated " + updatedList.get(position).getMessage()+" from "+oldPri+" to "+updatedList.get(position).getPriority());
                                updatedList = sortDetachedList(updatedList);
                            }
                            //refresh the listView
                            resetListAdapter(updatedList);
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
     * default list of items to the adapter*/

    public void resetListAdapter(List<MessageStore.Message> items) {
        mFeedListAdaper = new FeedListAdapter(getActivity(), items);
        if (listView != null) {
            listView.setAdapter(mFeedListAdaper);
        }
    }

    @Override
    public void refreshView(List<?> items) {
        resetListAdapter((List<MessageStore.Message>)items);
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
}
