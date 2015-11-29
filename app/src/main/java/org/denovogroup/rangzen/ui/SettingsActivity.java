package org.denovogroup.rangzen.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.FriendStore;
import org.denovogroup.rangzen.backend.SecurityManager;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.backend.Utils;

/**
 * Created by Liran on 11/17/2015.
 *
 * An Activity which enable the user to set his own preferences such as security
 */
public class SettingsActivity extends ActionBarActivity implements SeekBar.OnSeekBarChangeListener, TextView.OnEditorActionListener {

    private int security_profiles;
    private float priority_threshold;

    SeekBar privacySeeker;
    SeekBar priorityThresholdSeeker;
    LinearLayout seekerTitles;
    TextView privacyDetailsTv;
    TextView priorityThresholdTv;
    TextView pseudonymTv;
    ImageView qrDisplay;
    String[] profiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        privacySeeker = (SeekBar) findViewById(R.id.privacySeekBar);
        priorityThresholdSeeker = (SeekBar) findViewById(R.id.priorityThresholdSeekBar);
        seekerTitles = (LinearLayout) findViewById(R.id.seekbarTitles);
        privacyDetailsTv = (TextView) findViewById(R.id.schemeDetails);
        priorityThresholdTv = (TextView) findViewById(R.id.priorityThresholdTitle);
        pseudonymTv = (EditText) findViewById(R.id.psedudonymText);
        qrDisplay = (ImageView) findViewById(R.id.qrCode);

        initView();

        getStoredSettings();
        updateView();
    }

    /** build the dynamic view parts such as the security profile seeker */
    private void initView() {
        pseudonymTv.setText(SecurityManager.getCurrentPseudonym(this));
        pseudonymTv.setOnEditorActionListener(this);
        privacySeeker.setOnSeekBarChangeListener(this);
        profiles = SecurityManager.getInstance().getProfiles();

        seekerTitles.removeAllViews();
        for(int i=0; i<profiles.length; i++){

            TextView tv = new TextView(this);
            tv.setText(profiles[i]);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if(i == 0){
                tv.setGravity(Gravity.LEFT);
            } else if( i == profiles.length -1){
                tv.setGravity(Gravity.RIGHT);
            } else {
                tv.setGravity(Gravity.CENTER);
            }

            seekerTitles.addView(tv);
        }

        priorityThresholdSeeker.setOnSeekBarChangeListener(this);

        QRCodeWriter writer = new QRCodeWriter();
        float density = getResources().getDisplayMetrics().density;
        int qrSizeInDp = Utils.dpToPx(250, this);

        try {
            FriendStore store = new FriendStore(this,StorageBase.ENCRYPTION_DEFAULT);
            BitMatrix matrix = writer.encode(store.getPublicDeviceIDString(), BarcodeFormat.QR_CODE, qrSizeInDp, qrSizeInDp);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++)
            {
                int offset = y * width;
                for (int x = 0; x < width; x++)
                {
                    pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            qrDisplay.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.privacySeekBar:
                if (fromUser) {
                    int position = convertProgressToIndex(progress);
                    int newProgress = (int) getPrivacySeekerSectionSize() * position;

                    seekBar.setProgress(newProgress);
                    privacyDetailsTv.setText(SecurityManager.getInstance().getProfile(position).getDescription());

                    security_profiles = position;
                    updateView();
                }
                break;
            case R.id.priorityThresholdSeekBar:
                priorityThresholdTv.setText(getString(R.string.priority_threshold_title)+" ("+progress+")");
                break;
        }
    }

    private int convertProgressToIndex(int progress){
        double section = getPrivacySeekerSectionSize();
        double prev = Math.floor(progress / section);
        double next = Math.ceil(progress / section);
        return (progress - section * prev > section * next - progress) ? (int) next : (int) prev;
    }

    private double getPrivacySeekerSectionSize(){
        return ((double) privacySeeker.getMax()) / (profiles.length-1);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.privacySeekBar:
                SecurityManager.setCurrentProfile(this, security_profiles);
                updateView();
                break;
            case R.id.priorityThresholdSeekBar:
                SecurityManager.setCurrentAutodeleteThreshold(this, seekBar.getProgress() / 100f);
                break;
        }
    }

    public void getStoredSettings(){
        security_profiles = SecurityManager.getCurrentProfile(this);
        onProgressChanged(privacySeeker, privacySeeker.getProgress(), true);
        priority_threshold = SecurityManager.getCurrentAutodeleteThreshold(this);
    }

    private void updateView(){
        privacySeeker.setProgress((int) Math.round(getPrivacySeekerSectionSize() * security_profiles));
        priorityThresholdSeeker.setProgress(Math.round(priority_threshold * 100));
        priorityThresholdSeeker.setEnabled(SecurityManager.getInstance().getProfile(security_profiles).isAutodelete());
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        switch(v.getId()){
            case R.id.psedudonymText:
                SecurityManager.setCurrentPseudonym(this, v.getText().toString());
                break;
        }
        return false;
    }
}
