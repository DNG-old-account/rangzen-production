package org.denovogroup.rangzen.uiold;

import android.os.Bundle;
import android.support.annotation.Nullable;
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
import org.denovogroup.rangzen.ui.DisableableLinearLayout;

/**
 * Created by Liran on 12/2/2015.
 */
public class SecuritySettingsFragment extends android.support.v4.app.Fragment implements
        CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener,
        TextView.OnEditorActionListener{

    SecurityProfile currentProfile;
    TextView trustThresholdDisplay;
    TextView ageThresholdDisplay;
    SeekBar profileSeekBar;
    SeekBar trustThresholdSeekBar;
    SeekBar ageThresholdSeekBar;
    String[] availableProfiles = null;//String[] availableProfiles = SecurityManager.getInstance().getProfiles();

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

            if(currentProfile.getName() == 0){//if(currentProfile.getName().equals(availableProfiles[i])){
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
                trustThresholdSeekBar.setEnabled(isChecked);
                ageThresholdSeekBar.setEnabled(isChecked);
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
            case R.id.checkbox_timestamp:
                currentProfile.setTimestamp(isChecked);
                break;
            case R.id.checkbox_use_trust:
                currentProfile.setUseTrust(isChecked);
                break;
            case R.id.checkbox_enforce_lock:
                currentProfile.setEnforceLock(isChecked);
                break;
            case R.id.checkbox_random_exchange:
                currentProfile.setRandomExchange(isChecked);
                break;
        }

        setCurrentAsCustomProfile();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()){
            case R.id.trustThresholdSeekBar:
                if(fromUser) {
                    currentProfile.setAutodeleteTrust(progress/100f);
                    setCurrentAsCustomProfile();
                }
                trustThresholdDisplay.setText(String.valueOf(progress));
                break;
            case R.id.ageThresholdSeekBar:
                if(fromUser) {
                    int minProgress = 1;
                    if(progress >= minProgress) {
                        currentProfile.setAutodeleteAge(progress);
                    } else {
                        currentProfile.setAutodeleteAge(minProgress);
                        seekBar.setProgress(minProgress);
                    }
                    setCurrentAsCustomProfile();
                }
                ageThresholdDisplay.setText(String.valueOf(progress));
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

        trustThresholdDisplay = (TextView) v.findViewById(R.id.trustThresholdDisplay);
        ageThresholdDisplay = (TextView) v.findViewById(R.id.ageThresholdDisplay);

        trustThresholdSeekBar =((SeekBar) v.findViewById(R.id.trustThresholdSeekBar));
        ageThresholdSeekBar =((SeekBar) v.findViewById(R.id.ageThresholdSeekBar));
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

        ((CheckBox) v.findViewById(R.id.checkbox_timestamp)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_timestamp)).setChecked(currentProfile.isTimestamp());
        ((CheckBox) v.findViewById(R.id.checkbox_timestamp)).setOnCheckedChangeListener(this);

        ((CheckBox) v.findViewById(R.id.checkbox_share_location)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_share_location)).setChecked(currentProfile.isShareLocation());
        ((CheckBox) v.findViewById(R.id.checkbox_share_location)).setOnCheckedChangeListener(this);

        ((CheckBox) v.findViewById(R.id.checkbox_use_trust)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_use_trust)).setChecked(currentProfile.isUseTrust());
        ((CheckBox) v.findViewById(R.id.checkbox_use_trust)).setOnCheckedChangeListener(this);

        ((CheckBox) v.findViewById(R.id.checkbox_enforce_lock)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_enforce_lock)).setChecked(currentProfile.isEnforceLock());
        ((CheckBox) v.findViewById(R.id.checkbox_enforce_lock)).setOnCheckedChangeListener(this);

        ((CheckBox) v.findViewById(R.id.checkbox_random_exchange)).setOnCheckedChangeListener(null);
        ((CheckBox) v.findViewById(R.id.checkbox_random_exchange)).setChecked(currentProfile.isRandomExchange());
        ((CheckBox) v.findViewById(R.id.checkbox_random_exchange)).setOnCheckedChangeListener(this);

        ((EditText) v.findViewById(R.id.editText_feedsize)).setOnEditorActionListener(null);
        ((EditText) v.findViewById(R.id.editText_feedsize)).setText(String.valueOf(currentProfile.getFeedSize()));
        ((EditText) v.findViewById(R.id.editText_feedsize)).setOnEditorActionListener(this);

        ((EditText) v.findViewById(R.id.editText_min_contacts)).setOnEditorActionListener(null);
        ((EditText) v.findViewById(R.id.editText_min_contacts)).setText(String.valueOf(currentProfile.getMinSharedContacts()));
        ((EditText) v.findViewById(R.id.editText_min_contacts)).setOnEditorActionListener(this);
        ((EditText) v.findViewById(R.id.editText_min_contacts)).setEnabled(currentProfile.isUseTrust());

        ((EditText) v.findViewById(R.id.editText_max_messages)).setOnEditorActionListener(null);
        ((EditText) v.findViewById(R.id.editText_max_messages)).setText(String.valueOf(currentProfile.getMaxMessages()));
        ((EditText) v.findViewById(R.id.editText_max_messages)).setOnEditorActionListener(this);

        ((TextView) v.findViewById(R.id.TextView_cooldown)).setText(String.valueOf(currentProfile.getCooldown()));
        ((TextView) v.findViewById(R.id.TextView_timebound)).setText(String.valueOf(currentProfile.getTimeboundPeriod()));

        trustThresholdSeekBar.setOnSeekBarChangeListener(null);
        trustThresholdSeekBar.setEnabled(currentProfile.isAutodelete());
        trustThresholdSeekBar.setOnSeekBarChangeListener(this);
        trustThresholdSeekBar.setProgress(Math.round(100 * currentProfile.getAutodeleteTrust()));

        ageThresholdSeekBar.setOnSeekBarChangeListener(null);
        ageThresholdSeekBar.setMax(365);
        ageThresholdSeekBar.setEnabled(currentProfile.isAutodelete());
        ageThresholdSeekBar.setOnSeekBarChangeListener(this);
        ageThresholdSeekBar.setProgress(currentProfile.getAutodeleteAge());

        profileSeekBar.setOnSeekBarChangeListener(null);
        profileSeekBar.setMax(availableProfiles.length-1);
        profileSeekBar.setOnSeekBarChangeListener(this);
    }

    private void setCurrentAsCustomProfile(){
        currentProfile.setName(SecurityManager.CUSTOM_PROFILE_NAME);
        SecurityManager.setCurrentProfile(getActivity(), currentProfile);
        profileSeekBar.setProgress(0);
        trustThresholdSeekBar.setEnabled(currentProfile.isAutodelete() && currentProfile.isUseTrust());
        ((EditText) getView().findViewById(R.id.editText_min_contacts)).setEnabled(currentProfile.isUseTrust());
    }
}
