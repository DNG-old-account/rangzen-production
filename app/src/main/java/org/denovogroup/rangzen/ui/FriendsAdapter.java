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
 * Created by Liran on 12/6/2015.
 */
public class FriendsAdapter extends CursorAdapter {

    private int name_colIndex;
    private int number_colIndex;
    private int addedVia_colIndex;
    private int checked_colIndex;

    /**
     * Holds references to views so that findViewById() is not needed to be
     * called so many times.
     */
    private ViewHolder mViewHolder;
    private boolean isInEditMode;

    public FriendsAdapter(Context context, Cursor c, boolean isInEditMode) {
        super(context, c, false);
        this.isInEditMode = isInEditMode;
        init(context, c);
    }

    private void init(Context context,Cursor cursor) {
        name_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_DISPLAY_NAME);
        number_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_NUMBER);
        addedVia_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_ADDED_VIA);
        checked_colIndex = cursor.getColumnIndexOrThrow(FriendStore.COL_CHECKED);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = inflater.inflate(R.layout.friend_list_item, parent, false);

        mViewHolder = new ViewHolder();
        mViewHolder.mCheckBox = (CheckBox) convertView
                .findViewById(R.id.checkbox);
        mViewHolder.mIconView = (ImageView) convertView
                .findViewById(R.id.item_icon);
        mViewHolder.mCheckBox.setVisibility(isInEditMode ? View.VISIBLE : View.GONE);
        mViewHolder.mNameView = (TextView) convertView
                .findViewById(R.id.display_name);
        mViewHolder.mNumberView = (TextView) convertView
                .findViewById(R.id.phone_number);
        convertView.setTag(mViewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        boolean addedViaPhone = cursor.getInt(addedVia_colIndex) == FriendStore.ADDED_VIA_PHONE;
        mViewHolder = (ViewHolder) view.getTag();
        mViewHolder.mCheckBox.setChecked(cursor.getInt(checked_colIndex) == FriendStore.TRUE);
        mViewHolder.mIconView.setImageResource(addedViaPhone ? android.R.drawable.ic_menu_call : android.R.drawable.ic_menu_camera);
        mViewHolder.mNameView.setText(cursor.getString(name_colIndex));
        mViewHolder.mNumberView.setText(addedViaPhone ? cursor.getString(number_colIndex) : "");
    }

    static class ViewHolder {
        /**
         * The view object that holds the icon for this current row item.
         */
        private ImageView mIconView;
        /**
         * The view object that holds the name for this current row item.
         */
        private TextView mNameView;
        /**
         * The view object that holds the number for this current row item.
         */
        private TextView mNumberView;
        /**
         * The view object that holds the check to delete for this current row item.
         */
        private CheckBox mCheckBox;
    }

    public boolean isInEditMode(){
        return isInEditMode;
    }
}
