package org.denovogroup.rangzen.ui;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

/**
 * Created by Liran on 1/4/2016.
 *
 * A fragment allowing user to set its own settings
 */
public class SettingsFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener, TextWatcher {

    SecurityProfile currentProfile;

    View generalBlock;
    View networkBlock;
    View feedBlock;
    View contactsBlock;
    View exchangeBlock;

    RadioGroup profilesGroup;
    Switch timestampSwitch;
    Switch trustSwitch;
    Switch locationSwitch;
    Switch pseudonymSwitch;

    EditText selfDestEditText;
    EditText restrictedEditText;
    EditText maxFeedEditText;
    Switch autodelSwitch;
    SeekBar autodelTrustSeekbar;
    EditText autodelTrustEditText;
    SeekBar autodelAgeSeekbar;
    EditText autodelAgeEditText;
    Switch addViaPhoneSwitch;
    Switch addViaQRSwitch;
    EditText maxMessagesPerExchangeEditText;
    EditText timeoutEditText;
    EditText macEditText;

    int autodelMaxTrust = 100;
    int autodelMaxAge = 365;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.settings_fragment, container,false);

        currentProfile = org.denovogroup.rangzen.backend.SecurityManager.getCurrentProfile(getActivity());

        generalBlock = view.findViewById(R.id.general_block);
        networkBlock = view.findViewById(R.id.network_block);
        networkBlock.setVisibility((Build.VERSION.SDK_INT >= 23) ? View.VISIBLE : View.GONE);
        feedBlock = view.findViewById(R.id.feed_block);
        contactsBlock = view.findViewById(R.id.contacts_block);
        exchangeBlock = view.findViewById(R.id.exchange_block);

        enableBlocks();

        profilesGroup = (RadioGroup) view.findViewById(R.id.radiogroup_profiles);
        timestampSwitch = (Switch) view.findViewById(R.id.switch_timestamp);
        trustSwitch = (Switch) view.findViewById(R.id.switch_trust);
        locationSwitch = (Switch) view.findViewById(R.id.switch_location_tagging);
        pseudonymSwitch = (Switch) view.findViewById(R.id.switch_pseudonym);
        selfDestEditText = (EditText) view.findViewById(R.id.edit_self_dest);
        restrictedEditText = (EditText) view.findViewById(R.id.edit_restricted);
        maxFeedEditText = (EditText) view.findViewById(R.id.edit_max_messages);
        autodelSwitch = (Switch) view.findViewById(R.id.switch_auto_delete);
        autodelTrustSeekbar = (SeekBar) view.findViewById(R.id.seekbar_autodelete_trust);
        autodelTrustSeekbar.setMax(autodelMaxTrust);
        autodelAgeSeekbar = (SeekBar) view.findViewById(R.id.seekbar_autodelete_age);
        autodelAgeSeekbar.setMax(autodelMaxAge);
        autodelTrustEditText = (EditText) view.findViewById(R.id.edit_autodelete_trust);
        autodelAgeEditText = (EditText) view.findViewById(R.id.edit_autodelete_age);
        addViaPhoneSwitch = (Switch) view.findViewById(R.id.switch_add_via_phone);
        addViaQRSwitch = (Switch) view.findViewById(R.id.switch_add_via_qr);
        maxMessagesPerExchangeEditText = (EditText) view.findViewById(R.id.edit_max_messages_p_exchange);
        timeoutEditText = (EditText) view.findViewById(R.id.edit_timeout_p_exchange);
        macEditText = (EditText) view.findViewById(R.id.edit_mac);

        setupView();

        return view;
    }

    private void setupView(){
        timeoutEditText.removeTextChangedListener(this);
        autodelTrustEditText.removeTextChangedListener(this);
        autodelAgeEditText.removeTextChangedListener(this);
        selfDestEditText.removeTextChangedListener(this);
        maxFeedEditText.removeTextChangedListener(this);
        maxMessagesPerExchangeEditText.removeTextChangedListener(this);
        restrictedEditText.removeTextChangedListener(this);
        macEditText.removeTextChangedListener(this);

        profilesGroup.setOnCheckedChangeListener(null);
        profilesGroup.check(currentProfile.getName());
        profilesGroup.setOnCheckedChangeListener(this);

        autodelTrustSeekbar.setOnSeekBarChangeListener(null);
        autodelTrustSeekbar.setProgress(Math.round(autodelMaxTrust * currentProfile.getAutodeleteTrust()));
        autodelTrustEditText.setText(String.valueOf(Math.round(autodelMaxTrust * currentProfile.getAutodeleteTrust())));
        autodelTrustEditText.addTextChangedListener(this);
        autodelTrustSeekbar.setOnSeekBarChangeListener(this);

        autodelAgeSeekbar.setOnSeekBarChangeListener(null);
        autodelAgeSeekbar.setProgress(currentProfile.getAutodeleteAge());
        autodelAgeEditText.setText(String.valueOf(currentProfile.getAutodeleteAge()));
        autodelAgeEditText.addTextChangedListener(this);
        autodelAgeSeekbar.setOnSeekBarChangeListener(this);

        timestampSwitch.setOnCheckedChangeListener(null);
        timestampSwitch.setChecked(currentProfile.isTimestamp());
        timestampSwitch.setOnCheckedChangeListener(this);

        trustSwitch.setOnCheckedChangeListener(null);
        trustSwitch.setChecked(currentProfile.isUseTrust());
        trustSwitch.setOnCheckedChangeListener(this);

        locationSwitch.setOnCheckedChangeListener(null);
        locationSwitch.setChecked(currentProfile.isShareLocation());
        locationSwitch.setOnCheckedChangeListener(this);

        pseudonymSwitch.setOnCheckedChangeListener(null);
        pseudonymSwitch.setChecked(currentProfile.isPseudonyms());
        pseudonymSwitch.setOnCheckedChangeListener(this);

        selfDestEditText.setText(String.valueOf(currentProfile.getTimeboundPeriod()));
        selfDestEditText.addTextChangedListener(this);

        restrictedEditText.setText(String.valueOf(currentProfile.getMinContactsForHop()));
        restrictedEditText.addTextChangedListener(this);

        maxFeedEditText.setText(String.valueOf(currentProfile.getFeedSize()));
        maxFeedEditText.addTextChangedListener(this);

        pseudonymSwitch.setOnCheckedChangeListener(null);
        pseudonymSwitch.setChecked(currentProfile.isAutodelete());
        autodelTrustEditText.setEnabled(currentProfile.isAutodelete());
        autodelAgeEditText.setEnabled(currentProfile.isAutodelete());
        autodelTrustSeekbar.setEnabled(currentProfile.isAutodelete());
        autodelAgeSeekbar.setEnabled(currentProfile.isAutodelete());
        pseudonymSwitch.setOnCheckedChangeListener(this);

        addViaPhoneSwitch.setOnCheckedChangeListener(null);
        addViaPhoneSwitch.setChecked(currentProfile.isFriendsViaBook());
        addViaPhoneSwitch.setOnCheckedChangeListener(this);

        addViaQRSwitch.setOnCheckedChangeListener(null);
        addViaQRSwitch.setChecked(currentProfile.isFriendsViaQR());
        addViaQRSwitch.setOnCheckedChangeListener(this);

        maxMessagesPerExchangeEditText.setText(String.valueOf(currentProfile.getMaxMessages()));
        maxMessagesPerExchangeEditText.addTextChangedListener(this);

        timeoutEditText.setText(String.valueOf(currentProfile.getTimeboundPeriod()));
        timeoutEditText.addTextChangedListener(this);

        String mac = SecurityManager.getStoredMAC(getActivity());
        if(mac != null && mac.length() > 0){
            macEditText.setText(mac);
            macEditText.setTextColor(getResources().getColor(BluetoothAdapter.checkBluetoothAddress(mac) ? android.R.color.black : android.R.color.holo_red_dark));
        } else {
            macEditText.clearComposingText();
            macEditText.setTextColor(getResources().getColor(android.R.color.black));
        }
        macEditText.addTextChangedListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()){
            case R.id.seekbar_autodelete_trust:
                if(fromUser){
                    autodelTrustEditText.setText(String.valueOf(progress));
                    currentProfile.setAutodeleteTrust(progress/100f);
                }
                break;
            case R.id.seekbar_autodelete_age:
                if(fromUser) {
                    autodelAgeEditText.setText(String.valueOf(progress));
                    currentProfile.setAutodeleteAge(progress);
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        boolean isCustom = true;
        switch(checkedId){
            case R.id.radio_profile_strict:
                SecurityManager.setCurrentProfile(getActivity(), SecurityManager.getInstance().getProfile(R.id.radio_profile_strict));
                currentProfile = SecurityManager.getCurrentProfile(getActivity());
                isCustom = false;
                break;
            case R.id.radio_profile_flexible:
                SecurityManager.setCurrentProfile(getActivity(), SecurityManager.getInstance().getProfile(R.id.radio_profile_flexible));
                currentProfile = SecurityManager.getCurrentProfile(getActivity());
                isCustom = false;
                break;
            case R.id.radio_profile_custom:
                // do nothing
                break;
        }

        if(isCustom){
            currentProfile.setName(R.id.radio_profile_custom);
            SecurityManager.setCurrentProfile(getActivity(),currentProfile);
        }

        enableBlocks();

        setupView();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch(buttonView.getId()){
            case R.id.switch_timestamp:
                currentProfile.setTimestamp(isChecked);
                break;
            case R.id.switch_trust:
                currentProfile.setUseTrust(isChecked);
                break;
            case R.id.switch_location_tagging:
                currentProfile.setShareLocation(isChecked);
                break;
            case R.id.switch_pseudonym:
                currentProfile.setPseudonyms(isChecked);
                break;
            case R.id.switch_auto_delete:
                currentProfile.setAutodelete(isChecked);
                break;
            case R.id.switch_add_via_phone:
                currentProfile.setFriendsViaBook(isChecked);
                break;
            case R.id.switch_add_via_qr:
                currentProfile.setFriendsViaQR(isChecked);
                break;
        }

        currentProfile.setName(R.id.radio_profile_custom);
        SecurityManager.setCurrentProfile(getActivity(), currentProfile);

        setupView();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        View v = getActivity().getCurrentFocus();
        if(v == null) return;

        boolean reload = true;

        int valueAsInt = 0;
        try{
            valueAsInt = Integer.parseInt(s.toString());
        } catch (NumberFormatException e){}

        switch(v.getId()){
            case R.id.edit_autodelete_age:
                if(valueAsInt > autodelMaxAge){
                    valueAsInt = autodelMaxAge;
                    autodelAgeEditText.setText(String.valueOf(valueAsInt));
                }
                autodelAgeSeekbar.setProgress(valueAsInt);
                currentProfile.setAutodeleteAge(valueAsInt);
                break;
            case R.id.edit_autodelete_trust:
                if(valueAsInt > autodelMaxTrust){
                    valueAsInt = autodelMaxAge;
                    autodelTrustEditText.setText(String.valueOf(valueAsInt));
                }
                autodelAgeSeekbar.setProgress(valueAsInt);
                currentProfile.setAutodeleteTrust(valueAsInt / 100f);
                break;
            case R.id.edit_self_dest:
                currentProfile.setTimeboundPeriod(valueAsInt);
                break;
            case R.id.edit_restricted:
                currentProfile.setMinContactsForHop(Math.max(1,valueAsInt));
                break;
            case R.id.edit_max_messages:
                currentProfile.setFeedSize(valueAsInt);
                break;
            case R.id.edit_max_messages_p_exchange:
                currentProfile.setMaxMessages(Math.max(1,valueAsInt));
                break;
            case R.id.edit_timeout_p_exchange:
                currentProfile.setCooldown(valueAsInt);
                break;
            case R.id.edit_mac:
                String mac = s != null ? s.toString() : "";
                SecurityManager.setStoredMAC(getActivity(), mac);
                break;
            default:
                reload = false;
                break;
        }

        if(reload) {
            currentProfile.setName(R.id.radio_profile_custom);
            SecurityManager.setCurrentProfile(getActivity(), currentProfile);

            setupView();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void enableBlocks(){
        boolean enable = currentProfile.getName() == R.id.radio_profile_custom;

        generalBlock.setEnabled(enable);
        feedBlock.setEnabled(enable);
        contactsBlock.setEnabled(enable);
        exchangeBlock.setEnabled(enable);
    }
}