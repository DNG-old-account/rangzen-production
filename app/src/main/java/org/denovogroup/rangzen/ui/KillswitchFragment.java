package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

/**
 * Created by Liran on 12/7/2015.
 */
public class KillswitchFragment extends Fragment {

    public static final String IS_APP_ENABLED = "isEnabled";
    Switch onoffButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.killswitch_frag, container, false);

        onoffButton = ((Switch)v.findViewById(R.id.onSwitch));

        SharedPreferences pref = getActivity().getSharedPreferences(PreferencesActivity.PREF_FILE, Context.MODE_PRIVATE);

        onoffButton.setChecked(pref.getBoolean(IS_APP_ENABLED, true));
        onoffButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences pref = getActivity().getSharedPreferences(PreferencesActivity.PREF_FILE, Context.MODE_PRIVATE);
                pref.edit().putBoolean(IS_APP_ENABLED, isChecked).commit();

                Intent serviceIntent = new Intent(getActivity(), RangzenService.class);
                if (isChecked) {
                    getActivity().startService(serviceIntent);
                } else {
                    getActivity().stopService(serviceIntent);
                }
            }
        });

        v.findViewById(R.id.reset_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setIcon(R.drawable.ic_dialog_alert_holo_light)
                        .setTitle(R.string.reset_dialog_title)
                        .setMessage(R.string.reset_app_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity())
                                        .setIcon(R.drawable.ic_dialog_alert_holo_light)
                                        .setTitle(R.string.confirm_reset_dialog_title)
                                        .setMessage(R.string.confirm_reset_app_message)
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                resetApp();
                                                dialog.dismiss();
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                builder2.show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                builder.show();
            }
        });

        return v;
    }

    private void resetApp(){
        MessageStore.getInstance(getActivity()).purgeStore();
        FriendStore.getInstance(getActivity()).purgeStore();
        SecurityManager.getInstance().clearProfileData(getActivity());
        onoffButton.setChecked(true);

        Intent intent = new Intent(getActivity(), Opener.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getActivity().startActivity(intent);
    }
}
