package org.denovogroup.rangzen.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.SearchHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 12/30/2015.
 *
 * A fragment which show a parent message along with a list of all the replies associated with this
 * parent message.
 * when creating this fragment MESSAGE_ID_KEY must be passed as an argument with the messageId of
 * the parent message
 */
public class ExpandedMessageFragment extends Fragment {

    public static final String MESSAGE_ID_KEY = "messageId";

    private static final int REQ_CODE_MESSAGE = 100;

    String messageId;

    ListView parentMessage;
    ListView listView;
    Spinner sortSpinner;

    private SearchView searchView;

    Menu menu;

    private String query = "";

    private enum sortOption{
        NEWEST, OLDEST, MOST_ENDORSED, LEAST_ENDORSED, MOST_CONNECTED, LEAST_CONNECTED
    }

    private sortOption currentSort = sortOption.NEWEST;

    private List<Object[]> sortOptions = new ArrayList<Object[]>(){{
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_newest, sortOption.NEWEST});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_oldest, sortOption.OLDEST});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_mostendorsed, sortOption.MOST_ENDORSED});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_leastendorsed, sortOption.LEAST_ENDORSED});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_mostconnected, sortOption.MOST_CONNECTED});
        add(new Object[]{R.drawable.ic_action_about,R.string.sort_opt_leastconnected, sortOption.LEAST_CONNECTED});
    }};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.feed_fragment_menu, menu);
        this.menu = menu;

        //Setup the search view
        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        setSearchView(searchView);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        messageId = getArguments().getString(MESSAGE_ID_KEY);

        View view = inflater.inflate(R.layout.expanded_message_fragment, container,false);

        parentMessage = (ListView) view.findViewById(R.id.expanded_item);
        parentMessage.setAdapter(new FeedAdapter(getActivity(), MessageStore.getInstance(getActivity()).getMessageById(messageId), false, feedAdapterCallbacks));

        listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(new FeedReplyAdapter(getActivity(), getCursor()));

        initSortSpinner();

        return view;
    }

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
        public void onNavigate(String message, String latxLon) {
            double lat = Double.parseDouble(latxLon.substring(0, latxLon.indexOf("x")));
            double lon = Double.parseDouble(latxLon.substring(latxLon.indexOf("x") + 1));

            Uri gmmIntentUri = Uri.parse("geo:"+lat+","+lon);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                getActivity().startActivity(mapIntent);
            }
        }

        @Override
        public void onReply(String parentId) {
            Intent intent = new Intent(getActivity(), PostActivity.class);
            intent.putExtra(PostActivity.MESSAGE_PARENT, parentId);
            startActivityForResult(intent, REQ_CODE_MESSAGE);
        }

        @Override
        public void onFavorite(String message, boolean isFavoriteBefore) {
            MessageStore.getInstance(getActivity()).favoriteMessage(
                    message,
                    !isFavoriteBefore);
            swapCursor();
        }

        private void swapCursor(){
            parentMessage.setAdapter(new FeedAdapter(getActivity(), MessageStore.getInstance(getActivity()).getMessageById(messageId), false, feedAdapterCallbacks));
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            switch(requestCode){
                case REQ_CODE_MESSAGE:
                    swapCursor();
                    break;
            }
        }
    }

    private void swapCursor(){

        int offsetFromTop = 0;
        int firstVisiblePosition = Math.max(0, listView.getFirstVisiblePosition());
        if(listView.getChildCount() > 0) {
            offsetFromTop = listView.getChildAt(0).getTop();
        }

        CursorAdapter newAdapter = ((CursorAdapter) listView.getAdapter());
        newAdapter.swapCursor(getCursor());
        listView.setAdapter(newAdapter);

        listView.setSelectionFromTop(firstVisiblePosition, offsetFromTop);
        parentMessage.setAdapter(new FeedAdapter(getActivity(), MessageStore.getInstance(getActivity()).getMessageById(messageId), false, feedAdapterCallbacks));
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

                    swapCursor();
                    listView.setSelectionFromTop(0,0);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // do nothing
                }
            });
        }
    }

    public void setSearchView(final SearchView searchView) {

        //TODO instead of overriding colors like that use ContextActionBar to overflow the regular one with custom actions

        searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text)
                .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        // TODO if(sortSpinner != null) sortSpinner.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
                    }
                });

        //Define on close listener which support pre-honycomb devices as well with the app compat
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

                if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                    ((AppCompatActivity) getActivity()).getSupportActionBar()
                            .setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.white)));
                }

                if (menu != null && menu.findItem(android.R.id.home) != null) {
                    menu.findItem(android.R.id.home).getIcon().mutate().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                }

                if (searchView != null) {
                    //tint the X button for clearing the text field
                    ImageView closeButton = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
                    if (closeButton != null)
                        closeButton.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);

                    //tint the ? button
                    ImageView collapseButton = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
                    if (collapseButton != null)
                        collapseButton.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);

                    //tint the ? button
                    ImageView goButton = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_go_btn);
                    if (goButton != null)
                        goButton.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);

                    //tint the ? button
                    ImageView searchButton = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_button);
                    if (searchButton != null)
                        searchButton.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);

                    //tint the ? button
                    ImageView voiceButton = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_voice_btn);
                    if (voiceButton != null)
                        voiceButton.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                    ((AppCompatActivity) getActivity()).getSupportActionBar()
                            .setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.app_purple)));
                }

                //reset the list to its normal state
                swapCursor();
            }
        });

        //Define the search procedure
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryText) {
                query = queryText;
                listView.setAdapter(new FeedReplyAdapter(getActivity(), getCursor()));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onQueryTextSubmit(newText);
                return false;
            }
        });
    }

    private Cursor getCursor(){
        String sqlQuery = SearchHelper.searchToSQL(query);
        return (sqlQuery != null) ?
                MessageStore.getInstance(getActivity()).getCommentsByQuery(messageId, sqlQuery) :
                MessageStore.getInstance(getActivity()).getCommentsContaining(messageId, query);
    }
}
