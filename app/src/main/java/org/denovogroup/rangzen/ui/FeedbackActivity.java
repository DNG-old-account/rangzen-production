package org.denovogroup.rangzen.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;

import org.denovogroup.rangzen.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Liran on 1/12/2016.
 */
public class FeedbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.feedback_activity);

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmailSendingForm(
                        ((CheckBox)findViewById(R.id.includeLog)).isChecked()
                );
            }
        });

    }

    private void openEmailSendingForm(boolean includeLog){
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getString(R.string.feedback_email), null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Rangzen Feedback");
        intent.putExtra(Intent.EXTRA_TEXT, "Dear Rangzen support representative");


        if(includeLog) {
            File log_filename = new File(Environment.getExternalStorageDirectory() + "/device_log.txt");
            log_filename.delete();

            //get device info
            String userData = "";

            try {
                PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
                userData += "Application: Ranzgen v" + info.versionName + " (" + info.versionCode + ")\n";
            } catch (PackageManager.NameNotFoundException e) {
            }

            userData += "OS version: " + android.os.Build.VERSION.SDK_INT + "\n";
            userData += "Device: " + android.os.Build.DEVICE + "\n";
            userData += "Model: " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")\n";

            try {
                log_filename.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(log_filename, true));
                writer.write(userData);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                //get log from device
                String cmd = "logcat -d -f" + log_filename.getAbsolutePath();
                Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                e.printStackTrace();
            }

            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(log_filename));
        }

        startActivity(Intent.createChooser(intent, "Send mail using..."));
    }
}
