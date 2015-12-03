package org.denovogroup.rangzen.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

/**
 * Created by Liran on 12/2/2015.
 */
public class SecuritySettingsFragment extends android.support.v4.app.Fragment implements
        CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener,
        TextView.OnEditorActionListener{

    SecurityProfile currentProfile;
    TextView thresholdDisplay;
    SeekBar profileSeekBar;
    SeekBar thresholdSeekBar;
    String[] availableProfiles = SecurityManager.getInstance().getProfiles();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        currentProfile = SecurityManager.getCurrentProfile(getActivity());

        View v = inflater.inflate(R.layout.security_settings_frag, container, false);

        refreshView(v);

        //create the profile seekbar titles and position seekbar marker
        ViewGroup seekbarTitles = (ViewGroup) v.findViewById(R.id.seekbarTitles);
        seekbarTitles.removeAllViews();
        for(int i=0; i<availableProfiles.length; i++){

            if(currentProfile.getName().equals(availableProfiles[i])){
                profileSeekBar.setProgress(i);
            }

            TextView tv = new TextView(getActivity());
            tv.setText(availableProfiles[i]);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if(i == 0){
                tv.setGravity(Gravity.LEFT);
            } else if( i == availableProfiles.length -1){
                tv.setGravity(Gravity.RIGHT);
            } else {
                tv.setGravity(Gravity.CENTER);
            }
            seekbarTitles.addView(tv);
        }
        return v;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch(buttonView.getId()){
            case R.id.checkbox_autodel:
                currentProfile.setAutodelete(isChecked);
                thresholdSeekBar.setEnabled(isChecked);
                break;
            case R.id.checkbox_friends_vcode:
                currentProfile.setFriendsViaQR(isChecked);
                break;
            case R.id.checkbox_friends_vphone:
                currentProfile.setFriendsViaBook(isChecked);
                break;
            case R.id.checkbox_pseudonym:
                currentProfile.setPseudonyms(isChecked);
                break;
            case R.id.checkbox_share_location:
                currentProfile.setShareLocation(isChecked);
                break;
        }

        setCurrentAsCustomProfile();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()){
            case R.id.priorityThresholdSeekBar:
                if(fromUser) {
                    SecurityManager.setCurrentAutodeleteThreshold(getActivity(), progress/100f);
                    setCurrentAsCustomProfile();
                }
                thresholdDisplay.setText(String.valueOf(progress));
                break;
            case R.id.privacySeekBar:
                if(fromUser){
                    if(progress > 0) {
                        currentProfile = SecurityManager.getInstance().getProfile(progress-1);
                    } else {
                        currentProfile.setName(SecurityManager.CUSTOM_PROFILE_NAME);
                    }
                    SecurityManager.setCurrentProfile(getActivity(), currentProfile);
                    refreshView(getView());
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        switch (v.getId()){
            case R.id.editText_feedsize:
                currentProfile.setFeedSize(Integer.parseInt(v.getText().toString()));
                break;
            case R.id.editText_min_contacts:
                currentProfile.setMinSharedContacts(Integer.parseInt(v.getText().toString()));
                break;
            case R.id.editText_max_messages:
                currentProfile.setMaxMessages(Integer.parseInt(v.getText().toString()));
                break;
        }

        setCurrentAsCustomProfile();

        return false;
    }

    private void refreshView(View v){
        if(v == null) return;

        thresholdDisplay = (TextView) v.findViewById(R.id.priorityThresholdDisplay);

        thresholdSeekBar =((SeekBar) v.findViewById(R.id.priorityThresholdSeekBar));
        profileSeekBar = ((SeekBar) v.findViewById(R.id.privacySeekBar));

        ((CheckBox) v.findViewById(R.id.checkbox_autodel)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_autodel)).setChecked(currentProfile.isAutodelete());
        ((CheckBox) v.findViewById(R.id.checkbox_autodel)).setOnCheckedChangeListener(this);

        ((CheckBox) v.findViewById(R.id.checkbox_friends_vcode)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_friends_vcode)).setChecked(currentProfile.isFriendsViaQR());
        ((CheckBox) v.findViewById(R.id.checkbox_friends_vcode)).setOnCheckedChangeListener(this);

        ((CheckBox) v.findViewById(R.id.checkbox_friends_vphone)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_friends_vphone)).setChecked(currentProfile.isFriendsViaBook());
        ((CheckBox) v.findViewById(R.id.checkbox_friends_vphone)).setOnCheckedChangeListener(this);

        ((CheckBox) v.findViewById(R.id.checkbox_pseudonym)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_pseudonym)).setChecked(currentProfile.isPseudonyms());
        ((CheckBox) v.findViewById(R.id.checkbox_pseudonym)).setOnCheckedChangeListener(this);

        ((CheckBox) v.findViewById(R.id.checkbox_share_location)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_share_location)).setChecked(currentProfile.isShareLocation());
        ((CheckBox) v.findViewById(R.id.checkbox_share_location)).setOnCheckedChangeListener(this);

        ((EditText) v.findViewById(R.id.editText_feedsize)).setOnEditorActionListener(null);
        ((EditText) v.findViewById(R.id.editText_feedsize)).setText(String.valueOf(currentProfile.getFeedSize()));
        ((EditText) v.findViewById(R.id.editText_feedsize)).setOnEditorActionListener(this);

        ((EditText) v.findViewById(R.id.editText_min_contacts)).setOnEditorActionListener(null);
        ((EditText) v.findViewById(R.id.editText_min_contacts)).setText(String.valueOf(currentProfile.getMinSharedContacts()));
        ((EditText) v.findViewById(R.id.editText_min_contacts)).setOnEditorActionListener(this);

        ((EditText) v.findViewById(R.id.editText_max_messages)).setOnEditorActionListener(null);
        ((EditText) v.findViewById(R.id.editText_max_messages)).setText(String.valueOf(currentProfile.getMaxMessages()));
        ((EditText) v.findViewById(R.id.editText_max_messages)).setOnEditorActionListener(this);

        thresholdSeekBar.setOnSeekBarChangeListener(null);
        thresholdSeekBar.setOnSeekBarChangeListener(this);
        thresholdSeekBar.setProgress(Math.round(100 * SecurityManager.getCurrentAutodeleteThreshold(getActivity())));

        profileSeekBar.setOnSeekBarChangeListener(null);
        profileSeekBar.setMax(availableProfiles.length-1);
        profileSeekBar.setOnSeekBarChangeListener(this);
    }

    private void setCurrentAsCustomProfile(){
        currentProfile.setName(SecurityManager.CUSTOM_PROFILE_NAME);
        SecurityManager.setCurrentProfile(getActivity(), currentProfile);
        profileSeekBar.setProgress(0);
    }
}
