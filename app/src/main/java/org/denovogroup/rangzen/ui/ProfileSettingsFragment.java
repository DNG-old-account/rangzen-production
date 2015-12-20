package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

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

        v.findViewById(R.id.export_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportFeed();
            }
        });

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

    private void exportFeed(){

        AsyncTask<Void, Void, Boolean> exportTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                Cursor cursor = MessageStore.getInstance(getActivity()).getMessagesCursor(false, false,1000);
                cursor.moveToFirst();

                int messageColIndex = cursor.getColumnIndex(MessageStore.COL_MESSAGE);
                int timestampColIndex = cursor.getColumnIndex(MessageStore.COL_TIMESTAMP);
                int trustColIndex = cursor.getColumnIndex(MessageStore.COL_TRUST);
                int likesColIndex = cursor.getColumnIndex(MessageStore.COL_LIKES);
                int pseudoColIndex = cursor.getColumnIndex(MessageStore.COL_PSEUDONYM);

                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+File.separator+"Rangzen exports");
                if(!dir.exists()){
                    dir.mkdirs();
                }

                File file = new File(dir, "export"+Utils.convertTimestampToDateStringCompact(false, System.currentTimeMillis())+".csv");

                try {
                    FileWriter fos = new FileWriter(file);
                    BufferedWriter bos = new BufferedWriter(fos);

                    SecurityProfile profile = SecurityManager.getCurrentProfile(getActivity());

                    final String newLine = System.getProperty("line.separator");

                    String colTitlesLine = ""
                            + (profile.isTimestamp() ? "\""+MessageStore.COL_TIMESTAMP+ "\"," : "")
                            + "\""+MessageStore.COL_TRUST+"\","
                            + "\""+MessageStore.COL_LIKES+"\","
                            + (profile.isPseudonyms() ? "\""+MessageStore.COL_PSEUDONYM +"\"," : "")
                            + "\"" + MessageStore.COL_MESSAGE+"\"";

                    bos.write(colTitlesLine);
                    bos.write(newLine);

                    while(!cursor.isAfterLast()){
                        String line = ""
                                + (profile.isTimestamp() ? "\""+(Utils.convertTimestampToDateStringCompact(false,cursor.getLong(timestampColIndex))+ "\",") : "")
                                + "\""+(cursor.getFloat(trustColIndex)*100)+"\","
                                + "\""+(cursor.getInt(likesColIndex))+"\","
                                + (profile.isPseudonyms() ? "\""+(cursor.getString(pseudoColIndex)) +"\"," : "")
                                + "\"" + formatMessageForCSV(cursor.getString(messageColIndex))+"\"";
                        bos.write(line);
                        bos.write(newLine); //due to a bug in windows notepad text will be displayed as a long string instead of multiline, this is a note-pad specific problem
                        cursor.moveToNext();
                    }
                    cursor.close();
                    bos.flush();
                    bos.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            AlertDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.export);
                builder.setMessage(R.string.export_message);
                builder.setCancelable(false);
                ProgressBar bar = new ProgressBar(getActivity());
                builder.setView(bar);
                dialog = builder.show();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if(dialog != null) dialog.dismiss();
                Toast.makeText(getActivity(),aBoolean ? "Export successful" : "Oops, cannot export", Toast.LENGTH_LONG).show();
            }
        };

        exportTask.execute();
    }

    private String formatMessageForCSV(String rawString){
        String csvString = rawString.replaceAll("[\r\n\"]", " ");
        return csvString;
    }
}
