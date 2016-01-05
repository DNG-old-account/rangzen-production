package org.denovogroup.rangzen.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.FriendStore;

/**
 * Created by Liran on 1/3/2016.
 */
public class ContactAdapter extends CursorAdapter {

    boolean selectionMode = false;

    int name_colIndex;
    int number_colIndex;
    int addedVia_colIndex;
    int checked_colIndex;

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    ViewHolder viewHolder;

    public ContactAdapter(Context context, Cursor c, boolean inSelectionMode) {
        super(context, c, false);
        selectionMode = inSelectionMode;
        init(context,c);
    }

    private void init(Context context,Cursor cursor) {

        name_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_DISPLAY_NAME);
        number_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_NUMBER);
        addedVia_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_ADDED_VIA);
        checked_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_CHECKED);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View convertView = LayoutInflater.from(context).inflate(selectionMode ? R.layout.contact_list_item_selection : R.layout.contact_list_item, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.mCheckBox = (CheckBox) convertView.findViewById(R.id.checkbox);
        viewHolder.mIconView = (ImageView) convertView.findViewById(R.id.item_icon);
        viewHolder.mIndexView = (TextView) convertView.findViewById(R.id.char_index);
        viewHolder.mNameView = (TextView) convertView.findViewById(R.id.item_text);
        viewHolder.mDivider = convertView.findViewById(R.id.item_divider);
        convertView.setTag(viewHolder);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        viewHolder = (ViewHolder) view.getTag();

        boolean addedViaPhone = cursor.getInt(addedVia_colIndex) == FriendStore.ADDED_VIA_PHONE;

        if(viewHolder.mCheckBox != null){
            viewHolder.mCheckBox.setChecked(cursor.getInt(checked_colIndex) == FriendStore.TRUE);
            viewHolder.mCheckBox.setFocusable(false);
        }
        viewHolder.mIconView.setImageResource(addedViaPhone ? android.R.drawable.ic_menu_call : android.R.drawable.ic_menu_camera);
        viewHolder.mNameView.setText(cursor.getString(name_colIndex));

        char currentIndex = cursor.getString(name_colIndex).toUpperCase().charAt(0);
        char prevIndex = '^';
        char nextIndex = '^';

        if(cursor.moveToPrevious()){
            prevIndex = cursor.getString(name_colIndex).toUpperCase().charAt(0);
            cursor.moveToNext();
        }

        if(cursor.moveToNext()){
            nextIndex = cursor.getString(name_colIndex).toUpperCase().charAt(0);
            cursor.moveToPrevious();
        }

        if(viewHolder.mIndexView != null){
            viewHolder.mIndexView.setText(currentIndex+"");
            viewHolder.mIndexView.setVisibility((currentIndex != prevIndex) ? View.VISIBLE : View.INVISIBLE);
        }

        viewHolder.mDivider.setVisibility(currentIndex != nextIndex ? View.VISIBLE : View.INVISIBLE);
    }

    static class ViewHolder {
        /**
         * The view object that holds the icon for this current row item.
         */
        private ImageView mIconView;
        /**
         * The view object that holds the char index for this current row item
         */
        private TextView mIndexView;
        /**
         * The view object that holds the name for this current row item.
         */
        private TextView mNameView;
        /**
         * The view object that holds the check to delete for this current row item.
         */
        private CheckBox mCheckBox;

        private View mDivider;
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }
}
