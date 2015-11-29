package org.denovogroup.rangzen.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.Utils;
import org.denovogroup.rangzen.objects.RangzenMessage;

import java.util.List;

public class FeedFragment extends Fragment implements Refreshable{

    /** the amount to change the priority of a message by upon clicking on upvote/downvote*/
    private static final double PRIORITY_INCREMENT = 0.01d;

    private SwipeMenuListView listView;
    private FeedAdapter mFeedListAdaper;
    private String query = "";
    private AsyncTask<String, Void, Cursor> searchTask;

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

    public void setQuery(String query){
        this.query = query;

        if (searchTask != null) {
            searchTask.cancel(true);
        }

        searchTask = new AsyncTask<String, Void, Cursor>() {

            @Override
            protected Cursor doInBackground(String... params) {
                MessageStore store = MessageStore.getInstance(getActivity());
                return store.getMessagesContainingCursor(params[0], false, -1);
            }

            @Override
            protected void onPostExecute(Cursor messages) {
                super.onPostExecute(messages);

                if (messages != null) {
                    refreshView(messages);
                }
            }
        };

        searchTask.execute(query);
    }


    /** Create all the necessary components for the swipe menu of the items in the list and assign
     * a list adapter to the feed list view
      */
    private void setupListView(){
        setupListSwipeMenu();
        resetListAdapter(null, false);
    }

    /** Setting the Swipe menu which appears when an item is being swiped to the left
     * this also include assigning the menu buttons with click listeners
     */
    private void setupListSwipeMenu(){

        final int upvoteItemId = 0;
        final int downvoteItemId = 1;
        final int deleteItemId = 2;
        final int retweetItemId = 3;

        final SwipeMenuCreator creator = new SwipeMenuCreator() {

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
                MessageStore store = MessageStore.getInstance(getActivity());
                final Cursor cursor = mFeedListAdaper.getCursor();
                cursor.moveToPosition(position);
                boolean updateViewDelayed = false;

                final String message = cursor.getString(cursor.getColumnIndex(MessageStore.COL_MESSAGE));
                int priorityChange = cursor.getInt(cursor.getColumnIndex(MessageStore.COL_PRIORITY));

                switch (swipeMenu.getMenuItem(index).getId()) {
                    case upvoteItemId:
                        priorityChange = Math.min(100, priorityChange+1);
                        updateViewDelayed = true;
                        break;
                    case downvoteItemId:
                        priorityChange = Math.max(0, priorityChange-1);
                        updateViewDelayed = true;
                        break;
                    case deleteItemId:

                        final MessageStore store_final = store;

                        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                        dialog.setTitle(R.string.confirm_delete_title);
                        dialog.setMessage(R.string.confirm_delete);
                        dialog.setIcon(R.drawable.ic_bin);
                        dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                store_final.removeMessage(message);
                                //refresh listview
                                setQuery(query);
                                dialog.dismiss();
                            }
                        });
                        dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        dialog.show();

                        break;
                    case retweetItemId:
                        priorityChange = 100;
                        updateViewDelayed = true;
                        break;
                }

                final int priorityChange_final = priorityChange;

                if (updateViewDelayed) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MessageStore.getInstance(getActivity())
                                    .updatePriority(
                                            message,
                                            priorityChange_final);
                            //refresh the listView
                            setQuery(query);
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
    public void resetListAdapter(Cursor items, Boolean keepApperance) {

        if(items == null) {
            items = MessageStore.getInstance(getActivity()).getMessagesCursor(false, 500);
        }

        mFeedListAdaper = new FeedAdapter(getActivity(), items, false);
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
    public void refreshView(Cursor items) {
        resetListAdapter(items, false);
    }

    /** Create a copy of the provided list and sort it based on the message priority
     *
      * @param list the list to be cloned and sorted
     * @return a sorted clone of the source list, sorted by priority
     */
    private List<RangzenMessage> sortDetachedList(List<RangzenMessage> list){
        List<RangzenMessage> sortedList = list;
        for(RangzenMessage m : list){
            while(sortedList.indexOf(m) > 0 && sortedList.get(sortedList.indexOf(m)-1).trust < m.trust) {

                int position = sortedList.indexOf(m);
                RangzenMessage m1 = m;
                RangzenMessage m2 = sortedList.get(sortedList.indexOf(m)-1);

                sortedList.set(position, m2);
                sortedList.set(position-1, m1);
            }
        }

        return sortedList;
    }
}
