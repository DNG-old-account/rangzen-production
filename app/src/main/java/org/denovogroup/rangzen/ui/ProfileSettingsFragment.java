package org.denovogroup.rangzen.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

/**
 * Created by Liran on 12/2/2015.
 */
public class ProfileSettingsFragment extends Fragment implements TextView.OnEditorActionListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.profile_settings_frag, container, false);

        ((EditText) v.findViewById(R.id.psedudonymText)).setText(SecurityManager.getCurrentPseudonym(getActivity()));
        ((EditText) v.findViewById(R.id.psedudonymText)).setOnEditorActionListener(this);

        ((ImageView) v.findViewById(R.id.qrCode)).setImageBitmap(getQRCodeFromPublicId());

        return v;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        switch(v.getId()){
            case R.id.psedudonymText:
                SecurityManager.setCurrentPseudonym(getActivity(), v.getText().toString());
                break;
        }
        return false;
    }

    private Bitmap getQRCodeFromPublicId(){
        QRCodeWriter writer = new QRCodeWriter();
        int qrSizeInDp = Utils.dpToPx(250, getActivity());

        try {
            FriendStore store = FriendStore.getInstance(getActivity());
            BitMatrix matrix = writer.encode(store.getPublicDeviceIDString(getActivity(), StorageBase.ENCRYPTION_DEFAULT), BarcodeFormat.QR_CODE, qrSizeInDp, qrSizeInDp);
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

            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }
}
