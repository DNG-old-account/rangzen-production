package org.denovogroup.rangzen.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

/**
 * Created by Liran on 1/3/2016.
 */
public class ContactFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "ContactFragment";
    private static final int PICK_CONTACT = 100;

    private Menu menu;
    private ListView listView;
    private TextView leftText;

    private boolean inSelectionMode = false;
    private boolean selectAll = false;

    private String query;

    private String publicKey;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.drawer_menu_contact);

        leftText = (TextView) ((MainActivity) getActivity()).getToolbar().findViewById(R.id.leftText);

        View view = inflater.inflate(R.layout.contact_fragment, container, false);

        listView = (ListView) view.findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        listView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(null), false));

        setActionbar();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.contact_fragment, menu);
        this.menu = menu;

        MenuItem item = menu.findItem(R.id.action_delete);
        if(item != null) item.setVisible(inSelectionMode);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryText) {
                query = queryText;
                listView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(query), false));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return onQueryTextSubmit(newText);
            }
        });
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                query = null;
                listView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(query), false));
                inSelectionMode = false;
                setActionbar();
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case android.R.id.home:
                ActionBarDrawerToggle toggle = ((DrawerActivityHelper) getActivity()).getDrawerToggle();
                if(!toggle.isDrawerIndicatorEnabled()){
                    toggle.setDrawerIndicatorEnabled(true);
                    query = null;
                    listView.setAdapter(new ContactAdapter(getActivity(),FriendStore.getInstance(getActivity()).getFriendsCursor(null), false));
                    inSelectionMode = false;
                    setActionbar();
                }
                break;
            case R.id.add_friend:
                startAddFriend();
                break;
            case R.id.action_delete:
                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.delete_dialog_title);
                dialog.setMessage(getString(R.string.delete_dialog_friend1) + " " + FriendStore.getInstance(getActivity()).getCheckedCount() + " " + getString(R.string.delete_dialog_friend2));
                dialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FriendStore.getInstance(getActivity()).deleteChecked();
                        listView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(null), false));
                        inSelectionMode = false;
                        dialog.dismiss();
                    }
                });
                AlertDialog alertdialog = dialog.create();
                DialogStyler.styleAndShow(getActivity(), alertdialog);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, View view, int position, long id) {
        final Cursor cursor = ((CursorAdapter) parent.getAdapter()).getCursor();
        cursor.moveToPosition(position);
        publicKey = cursor.getString(cursor.getColumnIndex(FriendStore.COL_PUBLIC_KEY));

        if(((ContactAdapter) parent.getAdapter()).isSelectionMode()){
            boolean checkedState =  cursor.getInt(cursor.getColumnIndex(FriendStore.COL_CHECKED)) == FriendStore.TRUE;

            FriendStore.getInstance(getActivity()).setChecked(publicKey, !checkedState);
            ((ContactAdapter) parent.getAdapter()).changeCursor(FriendStore.getInstance(getActivity()).getFriendsCursor(query));
            ((ContactAdapter) parent.getAdapter()).notifyDataSetChanged();

            long checkedCount = FriendStore.getInstance(getActivity()).getCheckedCount();

            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(checkedCount <= 99 ? String.valueOf(checkedCount) : "+99");

        } else {

            boolean isFromPhone = cursor.getInt(cursor.getColumnIndex(FriendStore.COL_ADDED_VIA)) == FriendStore.ADDED_VIA_PHONE;

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setTitle(R.string.conf_friend_dialog_title);
            final View dialogView = getActivity().getLayoutInflater().inflate(isFromPhone ? R.layout.add_friend_from_phonebook_dialog : R.layout.add_friend_from_qr_dialog, null);
            dialogBuilder.setView(dialogView);
            dialogBuilder.setCancelable(false);
            dialogBuilder.setPositiveButton(android.R.string.ok, null);
            dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            final AlertDialog alertdialog = dialogBuilder.create();
            alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {

                    final EditText nameInput = (EditText) dialogView.findViewById(R.id.name);
                    nameInput.setText(cursor.getString(cursor.getColumnIndex(FriendStore.COL_DISPLAY_NAME)));

                    final EditText numberInput = (EditText) dialogView.findViewById(R.id.phone_number);
                    if (numberInput != null) {
                        numberInput.setText(cursor.getString(cursor.getColumnIndex(FriendStore.COL_NUMBER)));
                    }

                    alertdialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String name = nameInput.getText().toString();
                            if (name.length() == 0) return;

                            String number = numberInput == null ? null : numberInput.getText().toString();
                            FriendStore.getInstance(getActivity()).editFriend(
                                    publicKey,
                                    name,
                                    number
                            );

                            ((ContactAdapter) parent.getAdapter()).changeCursor(FriendStore.getInstance(getActivity()).getFriendsCursor(query));
                            ((ContactAdapter) parent.getAdapter()).notifyDataSetChanged();
                            alertdialog.dismiss();
                        }
                    });
                }
            });
            DialogStyler.styleAndShow(getActivity(), alertdialog);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if(!((ContactAdapter)parent.getAdapter()).isSelectionMode()){
            inSelectionMode = true;

            FriendStore.getInstance(getActivity()).setCheckedAll(false);
            listView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(query), true));

            setActionbar();
        }
        return false;
    }

    public void startAddFriend(){
        SecurityProfile profile = SecurityManager.getCurrentProfile(getActivity());
        if(profile.isFriendsViaBook() ^ profile.isFriendsViaQR()){
            if(profile.isFriendsViaBook()){
                openPhonebook();
            }{
                openScanner();
            }
        } else {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.add_friend_dialog_title)
                    .setMessage(R.string.add_friend_dialog_body)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            if (org.denovogroup.rangzen.backend.SecurityManager.getCurrentProfile(getActivity()).isFriendsViaQR()) {
                dialogBuilder.setNeutralButton(R.string.add_friend_qrcode, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openScanner();
                    }
                });
            }
            if (org.denovogroup.rangzen.backend.SecurityManager.getCurrentProfile(getActivity()).isFriendsViaBook()) {
                dialogBuilder.setPositiveButton(R.string.add_friend_phonebook, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openPhonebook();
                    }
                });
            }
            AlertDialog alertdialog = dialogBuilder.create();
            DialogStyler.styleAndShow(getActivity(), alertdialog);
        }
    }

    private void openScanner(){
        IntentIntegrator integrator = new IntentIntegrator(getActivity());
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan a barcode");
        integrator.setBeepEnabled(false);
        integrator.initiateScan();
    }

    private void openPhonebook(){
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    /**
     * Called whenever any activity launched from this activity exits. For
     * example, this is called when returning from the QR code activity,
     * providing us with the QR code (if any) that was scanned.
     *
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.i(TAG, "Got activity result back in ContactFragment!");

        // Check whether the activity that returned was the QR code activity,
        // and whether it succeeded.
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        Log.d(TAG,"result "+intentResult);
        if(intentResult != null){
            // Grab the string extra containing the QR code that was scanned.
            final FriendStore fs = FriendStore.getInstance(getActivity());
            String code = intentResult.getContents();
            // Convert the code into a public Rangzen ID.
            final byte[] publicIDBytes = intentResult.getRawBytes();
            Log.i(TAG, "received intent with code " + code);

            // Try to add the friend to the FriendStore, if they're not null.
            if (publicIDBytes != null) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle(R.string.conf_friend_dialog_title);
                final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.add_friend_from_qr_dialog, null);
                dialog.setView(dialogView);
                dialog.setCancelable(false);
                dialog.setPositiveButton(android.R.string.ok, null);
                dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                final AlertDialog alertdialog = dialog.create();
                alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        alertdialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                EditText userInput = (EditText) dialogView.findViewById(R.id.name);
                                String name = userInput.getText().toString();

                                if (name.length() == 0) return;

                                boolean wasAdded = fs.addFriendBytes(name, publicIDBytes, FriendStore.ADDED_VIA_QR, null);

                                Log.i(TAG, "Now have " + fs.getAllFriends().size()
                                        + " friends.");
                                if (wasAdded) {
                                    Toast.makeText(getActivity(), "Friend Added", Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    Toast.makeText(getActivity(), "Already Friends", Toast.LENGTH_SHORT)
                                            .show();

                                }
                                alertdialog.dismiss();
                                query = null;
                                listView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(null), false));
                            }
                        });
                    }
                });
                DialogStyler.styleAndShow(getActivity(), alertdialog);
            } else {
                // This can happen if the URI is well-formed (rangzen://<stuff>)
                // but the
                // stuff isn't valid base64, since we get here based on the
                // scheme but
                // not a check of the contents of the URI.
                Log.i(TAG,
                        "Opener got back a supposed rangzen scheme code that didn't process to produce a public id:"
                                + code);
                Toast.makeText(getActivity(), "Invalid Friend Code", Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            if(requestCode == PICK_CONTACT && resultCode == Activity.RESULT_OK && intent.getData() != null){
                Uri contactItemUri = intent.getData();
                Cursor contactCursor = getActivity().getContentResolver().query(contactItemUri, null, null, null, null);
                if(contactCursor != null && contactCursor.getCount() > 0){
                    contactCursor.moveToFirst();
                    String contactName = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    if(contactCursor.getInt(contactCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0){
                        String id = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts._ID));
                        Cursor phoneCursor = getActivity().getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                                new String[]{id}, null);

                        if(phoneCursor != null){
                            phoneCursor.moveToFirst();
                            while(!phoneCursor.isAfterLast()){
                                String unformattedNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                                TelephonyManager tm = (TelephonyManager)getActivity().getSystemService(Context.TELEPHONY_SERVICE);
                                String countryCode = "us";//tm.getSimCountryIso();

                                String formattedNumber;
                                if(Build.VERSION.SDK_INT >= 21) {
                                    formattedNumber = PhoneNumberUtils.formatNumberToE164(unformattedNumber, countryCode);
                                } else {
                                    formattedNumber = PhoneNumberUtils.formatNumber(unformattedNumber);
                                }
                                String noneNullValue = formattedNumber != null ? formattedNumber : unformattedNumber;
                                byte[] encryptedNumber = Crypto.encodeString(noneNullValue);

                                if(encryptedNumber != null) FriendStore.getInstance(getActivity()).addFriendBytes(contactName, encryptedNumber, FriendStore.ADDED_VIA_PHONE, noneNullValue);

                                phoneCursor.moveToNext();
                            }
                        }
                        if(phoneCursor != null) contactCursor.close();

                        query = null;
                        listView.setAdapter(new ContactAdapter(getActivity(), FriendStore.getInstance(getActivity()).getFriendsCursor(null),false));
                    } else {
                        Toast.makeText(getActivity(), "Selected item doesnt have a phone number", Toast.LENGTH_SHORT).show();
                    }
                }
                if(contactCursor != null) contactCursor.close();
            }
        }
    }

    private void setActionbar(){
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(getActivity().getResources().getColor(inSelectionMode ? R.color.toolbar_grey : R.color.app_purple)));
            actionBar.setTitle(inSelectionMode ? R.string.empty_string : R.string.feed);
        }
        if(menu != null) {
            menu.findItem(R.id.action_delete).setVisible(inSelectionMode);
            MenuItem searchItem = menu.findItem(R.id.search);
            if(searchItem != null) searchItem.setVisible(!inSelectionMode);
            if(searchItem != null) menu.findItem(R.id.add_friend).setVisible(!inSelectionMode);
        }
        if(leftText != null){
            leftText.setText(inSelectionMode ? R.string.select_all : R.string.empty_string);
            leftText.setVisibility(inSelectionMode ? View.VISIBLE : View.GONE);
            leftText.setOnClickListener(inSelectionMode ? new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FriendStore.getInstance(getActivity()).setCheckedAll(!selectAll);
                    selectAll = !selectAll;

                    ((ContactAdapter) listView.getAdapter()).changeCursor(FriendStore.getInstance(getActivity()).getFriendsCursor(query));
                    ((ContactAdapter) listView.getAdapter()).notifyDataSetChanged();

                    long checkedCount = FriendStore.getInstance(getActivity()).getCheckedCount();
                    ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(checkedCount <= 99 ? String.valueOf(checkedCount) : "+99");
                }
            } : null);
        }
        if(getActivity() instanceof DrawerActivityHelper){
            ActionBarDrawerToggle toggle = ((DrawerActivityHelper) getActivity()).getDrawerToggle();
            toggle.setDrawerIndicatorEnabled(!inSelectionMode);
            //toggle.syncState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        inSelectionMode = false;
        setActionbar();
    }
}
