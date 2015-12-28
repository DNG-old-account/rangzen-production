package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.SearchHelper;
import org.denovogroup.rangzen.uiold.Opener;
import org.denovogroup.rangzen.uiold.PostActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 12/27/2015.
 *
 * The fragment which display the message feed overview
 */
public class FeedFragment extends Fragment implements View.OnClickListener{

    private boolean inSelectionMode = false;
    private boolean selectAll = false;

    private ListView feedListView;
    private Button newPostButton;

    private ViewGroup newMessagesNotification;
    private TextView newMessagesNotification_text;
    private Button newMessagesNotification_button;
    private Spinner sortSpinner;
    private TextView leftText;
    private sortOption currentSort = sortOption.NEWEST;

    Menu menu;

    private String query = "";

    private enum sortOption{
        NEWEST, OLDEST, MOST_ENDORSED, LEAST_ENDORSED, MOST_CONNECTED, LEAST_CONNECTED
    }

    private List<Object[]> sortOptions = new ArrayList<Object[]>(){{
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_newest, sortOption.NEWEST});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_oldest, sortOption.OLDEST});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_mostendorsed, sortOption.MOST_ENDORSED});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_leastendorsed, sortOption.LEAST_ENDORSED});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_mostconnected, sortOption.MOST_CONNECTED});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_leastconnected, sortOption.LEAST_CONNECTED});
    }};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.feed);

        leftText = (TextView) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.leftText);

        initSortSpinner();

        View v = inflater.inflate(R.layout.feed_fragment, container, false);

        feedListView = (ListView) v.findViewById(R.id.feed_listView);
        newPostButton = (Button) v.findViewById(R.id.new_post_button);
            newPostButton.setOnClickListener(this);
        newMessagesNotification = (ViewGroup) v.findViewById(R.id.new_message_notification);
        newMessagesNotification_text = (TextView) v.findViewById(R.id.new_message_notification_desc);
        newMessagesNotification_button = (Button) v.findViewById(R.id.new_message_notification_btn);
            newMessagesNotification_button.setOnClickListener(this);

        //TODO temp add stuff to the store
        MessageStore.getInstance(getActivity()).addMessage(getActivity(), "" + System.currentTimeMillis(), "#message " + System.currentTimeMillis(), 0.98d, 123, "Mario", System.currentTimeMillis(), true, System.currentTimeMillis() + (400000000L), null, null);

        setListView();

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.new_post_button:
                //TODO
                break;
            case R.id.new_message_notification_btn:
                //TODO
                break;
        }
    }

    private void initSortSpinner(){
        if(getActivity() instanceof MainActivity) {
            sortSpinner = (Spinner) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.sortSpinner);
            sortSpinner.setVisibility(View.VISIBLE);
            sortSpinner.setAdapter(new FeedSortSpinnerAdapter(getActivity(), sortOptions));
            for(int i=0; i<sortOptions.size();i++){
                if(sortOptions.get(i)[2] == currentSort){
                    sortSpinner.setSelection(i);
                    break;
                }
            }
            sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentSort = (sortOption) sortOptions.get(position)[2];

                    MessageStore store = MessageStore.getInstance(getActivity());

                    switch(currentSort){
                        case LEAST_CONNECTED:
                            store.setSortOption(new String[]{MessageStore.COL_TRUST}, true);
                            break;
                        case MOST_CONNECTED:
                            store.setSortOption(new String[]{MessageStore.COL_TRUST}, false);
                            break;
                        case LEAST_ENDORSED:
                            store.setSortOption(new String[]{MessageStore.COL_LIKES}, true);
                            break;
                        case MOST_ENDORSED:
                            store.setSortOption(new String[]{MessageStore.COL_LIKES}, false);
                            break;
                        case NEWEST:
                            store.setSortOption(new String[]{MessageStore.COL_TIMESTAMP}, true);
                            break;
                        case OLDEST:
                            store.setSortOption(new String[]{MessageStore.COL_TIMESTAMP}, false);
                            break;
                    }

                    feedListView.setAdapter(new FeedAdapter(getActivity(), getCursor(), inSelectionMode, feedAdapterCallbacks));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // do nothing
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.feed_fragment_menu, menu);
        this.menu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    /** callback handler from clicks on buttons inside the feed list view */
    FeedAdapter.FeedAdapterCallbacks feedAdapterCallbacks = new FeedAdapter.FeedAdapterCallbacks() {
        @Override
        public void onUpvote(String message, int oldPriority) {
            MessageStore.getInstance(getActivity()).likeMessage(
                    message,
                    true);
            swapCursor();
        }

        @Override
        public void onDownvote(String message, int oldPriority) {
            MessageStore.getInstance(getActivity()).likeMessage(
                    message,
                    false);
            swapCursor();
        }

        @Override
        public void onFavorite(String message, boolean isFavoriteBefore) {
            MessageStore.getInstance(getActivity()).favoriteMessage(
                    message,
                    !isFavoriteBefore);
            swapCursor();
        }

        @Override
        public void onNavigate(String message, String latxLon) {
            double lat = Double.parseDouble(latxLon.substring(0, latxLon.indexOf("x")));
            double lon = Double.parseDouble(latxLon.substring(latxLon.indexOf("x") + 1));

            Uri gmmIntentUri = Uri.parse("geo:"+lat+","+lon);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            }

        }

        @Override
        public void onReply(String parentId) {
            Intent intent = new Intent(getActivity(), PostActivity.class);
            intent.putExtra(PostActivity.MESSAGE_PARENT, parentId);
            startActivityForResult(intent, Opener.Message);
        }
    };

    private Cursor getCursor(){
        String sqlQuery = SearchHelper.searchToSQL(query);
        return (sqlQuery != null) ?
                MessageStore.getInstance(getActivity()).getMessagesByQuery(sqlQuery) :
                MessageStore.getInstance(getActivity()).getMessagesContainingCursor(query, false, true, -1);
    }

    private void swapCursor(){

        int offsetFromTop = 0;
        int firstVisiblePosition = Math.max(0, feedListView.getFirstVisiblePosition());
        if(feedListView.getChildCount() > 0) {
            offsetFromTop = feedListView.getChildAt(0).getTop();
        }

        CursorAdapter newAdapter = ((CursorAdapter) feedListView.getAdapter());
        newAdapter.swapCursor(getCursor());
        feedListView.setAdapter(newAdapter);

        feedListView.setSelectionFromTop(firstVisiblePosition, offsetFromTop);
    }

    private void setListView(){
        feedListView.setAdapter(new FeedAdapter(getActivity(), getCursor(), inSelectionMode, feedAdapterCallbacks));
        if(inSelectionMode) {
            setListInSelectionMode();
        } else {
            setListInDisplayMode();
        }
    }

    AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            setListInSelectionMode();
            return false;
        }
    };

    private void setListInSelectionMode(){
        inSelectionMode = true;
        ((FeedAdapter) feedListView.getAdapter()).setSelectionMode(true);
        swapCursor();
        feedListView.setOnItemLongClickListener(null);
        feedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = ((CursorAdapter) feedListView.getAdapter()).getCursor();
                c.moveToPosition(position);
                boolean isChecked = c.getInt(c.getColumnIndex(MessageStore.COL_CHECKED)) == MessageStore.TRUE;
                String message = c.getString(c.getColumnIndex(MessageStore.COL_MESSAGE));

                MessageStore.getInstance(getActivity()).checkMessage(message, !isChecked);
                swapCursor();

                int checkedCount = MessageStore.getInstance(getActivity()).getCheckedMessages().getCount();

                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(checkedCount <= 99 ? String.valueOf(checkedCount) : "+99");

                if (menu != null) {
                    menu.findItem(R.id.action_delete).setEnabled(checkedCount > 0);
                    menu.findItem(R.id.action_delete_by_connection).setEnabled(checkedCount == 1);
                    menu.findItem(R.id.action_delete_by_exchange).setEnabled(checkedCount == 1);
                    menu.findItem(R.id.action_delete_from_sender).setEnabled(checkedCount == 1);
                    menu.findItem(R.id.action_retweet).setEnabled(checkedCount == 1);
                    menu.findItem(R.id.action_share).setEnabled(checkedCount == 1);
                }
            }
        });

        setActionbar();

        newPostButton.setVisibility(View.INVISIBLE);
    }

    private void setListInDisplayMode(){
        inSelectionMode = false;
        MessageStore.getInstance(getActivity()).checkAllMessages(false);
                ((FeedAdapter) feedListView.getAdapter()).setSelectionMode(false);
        feedListView.setOnItemLongClickListener(longClickListener);
        swapCursor();
        setActionbar();
        newPostButton.setVisibility(View.VISIBLE);
    }

    private void setActionbar(){
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(getActivity().getResources().getColor(inSelectionMode ? R.color.toolbar_grey : R.color.app_purple)));
            actionBar.setTitle(inSelectionMode ? R.string.empty_string : R.string.feed);
        }
        if(menu != null) {
            menu.setGroupVisible(R.id.checked_only_actions, inSelectionMode);
            menu.findItem(R.id.search).setVisible(!inSelectionMode);
        }
        if(sortSpinner != null) sortSpinner.setVisibility(inSelectionMode ? View.GONE : View.VISIBLE);
        if(leftText != null){
            leftText.setText(inSelectionMode ? R.string.select_all : R.string.empty_string);
            leftText.setVisibility(inSelectionMode ? View.VISIBLE : View.GONE);
            leftText.setOnClickListener(inSelectionMode ? new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MessageStore.getInstance(getActivity()).checkAllMessages(!selectAll);
                    selectAll = !selectAll;
                    swapCursor();

                    int checkedCount = MessageStore.getInstance(getActivity()).getCheckedMessages().getCount();
                    ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(checkedCount <= 99 ? String.valueOf(checkedCount) : "+99");
                }
            } : null);
        }
        if(getActivity() instanceof DrawerActivityHelper){
            ActionBarDrawerToggle toogle = ((DrawerActivityHelper) getActivity()).getDrawerToggle();
            toogle.setDrawerIndicatorEnabled(!inSelectionMode);
            toogle.syncState();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final Cursor checkedMessages = MessageStore.getInstance(getActivity()).getCheckedMessages();
        checkedMessages.moveToFirst();
        AlertDialog.Builder dialog = null;

        switch (item.getItemId()){
            case android.R.id.home:
                ActionBarDrawerToggle toogle = ((DrawerActivityHelper) getActivity()).getDrawerToggle();
                if(!toogle.isDrawerIndicatorEnabled()){
                    setListInDisplayMode();
                }
                break;
            case R.id.action_retweet:
                //TODO fix this to new assets
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra(PostActivity.MESSAGE_BODY, checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_MESSAGE)));
                startActivityForResult(intent, MainActivity.REQ_CODE_MESSAGE);
                break;
            case R.id.action_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Sent with Rangzen");
                shareIntent.putExtra(Intent.EXTRA_TEXT, checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_MESSAGE)));
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_using)));
                break;
            case R.id.action_delete:
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " " + checkedMessages.getCount() + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MessageStore.getInstance(getActivity()).removeCheckedMessage();
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.action_delete_by_connection:
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " " + checkedMessages.getCount() + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkedMessages.getFloat(checkedMessages.getColumnIndex(MessageStore.COL_TRUST));
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.action_delete_by_exchange:
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " " + checkedMessages.getCount() + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /*TODO delete by exchange
                        MessageStore.getInstance(getActivity()).deleteBySender(
                                checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_PSEUDONYM))
                        );*/
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.action_delete_from_sender:
                dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_message1) + " " + checkedMessages.getCount() + " " + getString(R.string.delete_dialog_message2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MessageStore.getInstance(getActivity()).deleteBySender(
                                checkedMessages.getString(checkedMessages.getColumnIndex(MessageStore.COL_PSEUDONYM))
                        );
                        setListInDisplayMode();
                        dialog.dismiss();
                    }
                });
                break;
        }

        if(dialog != null) dialog.show();
        checkedMessages.close();
        return super.onOptionsItemSelected(item);
    }
}
