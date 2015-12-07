package org.denovogroup.rangzen.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.SearchHelper;
import org.denovogroup.rangzen.objects.RangzenMessage;

import java.util.List;

public class FeedFragment extends Fragment implements Refreshable{

    /** the amount to change the priority of a message by upon clicking on upvote/downvote*/
    private static final double PRIORITY_INCREMENT = 0.01d;

    private ListView listView;
    private FeedAdapter mFeedListAdaper;
    private String query = "";
    private AsyncTask<String, Void, Cursor> searchTask;
    private int itemOffset = 0;
    private int firstItem = 0;
    private ImageButton addButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.feed, container, false);
        listView = (ListView) view.findViewById(R.id.list);
        setupListView();

        addButton = (ImageButton) view.findViewById(R.id.new_post_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity().getClass() == Opener.class) {
                    ((Opener) getActivity()).showFragment(1);
                }
            }
        });

        return view;
    }

    public void setQuery(String query, final boolean retainScroll){
        this.query = query;

        if(!retainScroll){
            firstItem = 0;
            firstItem = 0;
        }

        if (searchTask != null) {
            searchTask.cancel(true);
        }

        searchTask = new AsyncTask<String, Void, Cursor>() {

            @Override
            protected Cursor doInBackground(String... params) {
                MessageStore store = MessageStore.getInstance(getActivity());
                String sqlQuery = SearchHelper.searchToSQL(params[0]);

                Cursor cursor = (sqlQuery != null) ?
                        store.getMessagesByQuery(sqlQuery) :
                        store.getMessagesContainingCursor(params[0], false, -1);

                if(cursor == null){
                    cursor = store.getMessagesContainingCursor("", false, -1);
                }

                return cursor;
            }

            @Override
            protected void onPostExecute(Cursor messages) {
                super.onPostExecute(messages);

                if (messages != null) {
                    refreshView(messages, retainScroll);
                }
            }
        };

        searchTask.execute(query);
    }


    /** Create all the necessary components for the swipe menu of the items in the list and assign
     * a list adapter to the feed list view
      */
    private void setupListView(){
        resetListAdapter(null, false);
    }

    AlertDialog deleteManyDialog;

    /** set a feed list adapter to the list using the supplied list as source or using the default
     * items set if supplied list is null
     * @param items List of messages to be displayed by the attached listView or null to set
     * @param keepApperance whether or not the view should retain its relative position before updating
     * default list of items to the adapter*/
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)//this line is required to overcome a bug in the documentation, the problematic call is actually available since API 1 not 21
    public void resetListAdapter(Cursor items, final Boolean keepApperance) {

        if(items == null) {
            items = MessageStore.getInstance(getActivity()).getMessagesCursor(false, 500);
        }

        mFeedListAdaper = new FeedAdapter(getActivity(), items, false);
        if (listView != null) {
            int position = listView.getFirstVisiblePosition();
            int offset = (listView.getChildAt(0) != null) ? listView.getChildAt(0).getTop() : 0;

            listView.setAdapter(mFeedListAdaper);
            mFeedListAdaper.setAdapterCallbacks(new FeedAdapter.FeedAdapterCallbacks() {
                @Override
                public void onDelete(final String message) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setTitle(R.string.confirm_delete_title);
                    dialog.setMessage(R.string.confirm_delete);
                    dialog.setIcon(R.drawable.ic_bin);
                    dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MessageStore.getInstance(getActivity())
                                    .removeMessage(message);
                            //refresh the listView
                            firstItem = listView.getFirstVisiblePosition();
                            firstItem = listView.getChildAt(0).getTop();
                            setQuery(query, keepApperance);
                        }
                    });
                    dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                }

                @Override
                public void onDeleteMany(final String message, final float trust, final String pseudonym, final int likes) {
                    if(deleteManyDialog != null && deleteManyDialog.isShowing()) deleteManyDialog.dismiss();
                    deleteManyDialog = null;
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setTitle(R.string.confirm_delete_many_title);
                    dialog.setMessage(R.string.confirm_delete_many);
                    dialog.setIcon(R.drawable.ic_bin);
                    View contentView = getLayoutInflater(null).inflate(R.layout.delete_many_dialog, null);
                    contentView.findViewById(R.id.button_same_sender).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MessageStore.getInstance(getActivity()).deleteBySender(pseudonym);
                            setQuery(query, false);
                            if(deleteManyDialog != null) deleteManyDialog.dismiss();
                            deleteManyDialog = null;
                        }
                    });
                    contentView.findViewById(R.id.button_lower_trust).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MessageStore.getInstance(getActivity()).deleteByTrust(trust);
                            setQuery(query, false);
                            if(deleteManyDialog != null) deleteManyDialog.dismiss();
                            deleteManyDialog = null;
                        }
                    });
                    contentView.findViewById(R.id.button_lower_likes).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MessageStore.getInstance(getActivity()).deleteByLikes(likes);
                            setQuery(query, false);
                            if(deleteManyDialog != null) deleteManyDialog.dismiss();
                            deleteManyDialog = null;
                        }
                    });
                    dialog.setView(contentView);
                    dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    deleteManyDialog = dialog.show();
                }

                @Override
                public void onUpvote(String message, int oldPriority) {
                    int priorityChange = Math.min(100, oldPriority + 1);
                    MessageStore.getInstance(getActivity())
                            .updatePriority(
                                    message,
                                    priorityChange);
                    //refresh the listView
                    firstItem = listView.getFirstVisiblePosition();
                    firstItem = listView.getChildAt(0).getTop();
                    setQuery(query, true);
                }

                @Override
                public void onDownvote(String message, int oldPriority) {
                    int priorityChange = Math.max(0, oldPriority - 1);
                    MessageStore.getInstance(getActivity())
                            .updatePriority(
                                    message,
                                    priorityChange);
                    //refresh the listView
                    firstItem = listView.getFirstVisiblePosition();
                    firstItem = listView.getChildAt(0).getTop();
                    setQuery(query, true);
                }

                @Override
                public void onRetweet(String message) {
                    int priorityChange = 100;
                    MessageStore.getInstance(getActivity())
                            .updatePriority(
                                    message,
                                    priorityChange);
                    //refresh the listView
                    firstItem = listView.getFirstVisiblePosition();
                    firstItem = listView.getChildAt(0).getTop();
                    setQuery(query, true);
                }
            });
            if(keepApperance && listView.getFirstVisiblePosition() > -1) {
                listView.setSelectionFromTop(position,offset);
            }
        }
    }

    @Override
    public void refreshView(Cursor items, boolean retainScroll) {
        resetListAdapter(items, retainScroll);
        listView.smoothScrollToPositionFromTop(firstItem, itemOffset, 0);
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

    public void setAddButtonVisible(boolean visible){
        if(addButton != null){
            addButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
