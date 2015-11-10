package org.denovogroup.rangzen.beta;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.ReadStateTracker;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.objects.RangzenMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Liran on 9/21/2015.
 *
 * A custom receiver of the existing push receiver by parse, this allows custom parsing of received messages,
 * used for injecting RanzgenMessages to users remotely.
 */
public class CustomParsePushReceiver extends ParsePushBroadcastReceiver {

    private static final String RANGZEN_MESSAGE_PERFIX = ";$:";
    private static final String RANGZEN_MESSAGE_POSTFIX = ":$;";
    private static final String MESSAGE_TEXT_KEY = "text";
    private static final String MESSAGE_PRIORITY_KEY = "priority";
    private static final String MESSAGE_ID_KEY = "messageId";
    private static final String DEFAULT_MESSAGE_ID_PREFIX = "pushed_message";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Bundle extras = intent.getExtras();
        try {
            JSONObject pushJson = new JSONObject(extras.getString("com.parse.Data"));
            String pushedContent = pushJson.getString("alert");
            List<RangzenMessage> receivedMessages = parseMessage(pushedContent);
            if(receivedMessages != null){
                MessageStore store = MessageStore.getInstance(context);
                for(RangzenMessage receivedMessage : receivedMessages) {
                    store.addMessage(receivedMessage.text, receivedMessage.priority, true, receivedMessage.mId);
                    ReadStateTracker.setReadState(context, receivedMessage.text, false);
                }

                if(isAppInForeground(context)){
                    Intent newintent = new Intent();
                    newintent.setAction(MessageStore.NEW_MESSAGE);
                    context.getApplicationContext().sendBroadcast(newintent);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static List<RangzenMessage> parseMessage(String pushedContent){
        List<RangzenMessage> rangzenMessageList = new ArrayList<>();

        if(pushedContent == null) return rangzenMessageList;

        while(hasMessage(pushedContent)) {

            int messageStart = pushedContent.indexOf(RANGZEN_MESSAGE_PERFIX) + RANGZEN_MESSAGE_PERFIX.length();
            int messageEnd = pushedContent.indexOf(RANGZEN_MESSAGE_POSTFIX);

            if (messageEnd > messageStart) {
                try {
                    String message = pushedContent.substring(messageStart, messageEnd);
                    JSONObject jsonMessage = new JSONObject(message);

                    //text is mandatory for parsing, the rest can be auto assigned by the system
                    String text = jsonMessage.getString(MESSAGE_TEXT_KEY);
                    Double priority = jsonMessage.optDouble(MESSAGE_PRIORITY_KEY, 1d);

                    Random random = new Random();
                    String myUuid = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
                    String mId = jsonMessage.optString(MESSAGE_ID_KEY, DEFAULT_MESSAGE_ID_PREFIX+"_" + myUuid + "_" + System.currentTimeMillis() + random.nextInt(500));

                    RangzenMessage rangzenMessage = new RangzenMessage.Builder()
                            .mId(mId)
                            .priority(priority)
                            .text(text).build();

                    rangzenMessageList.add(rangzenMessage);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                pushedContent = pushedContent.substring(pushedContent.indexOf(RANGZEN_MESSAGE_POSTFIX) + RANGZEN_MESSAGE_POSTFIX.length());
            } else {
                break;
            }
        }

        return rangzenMessageList;
    }

    private static boolean hasMessage(String pushedContent){
        return pushedContent.contains(RANGZEN_MESSAGE_PERFIX) && pushedContent.contains(RANGZEN_MESSAGE_POSTFIX);
    }

    /** Check if the app have a living instance in the foreground
     *
     * @return true if the app is active and in the foreground, false otherwise
     */
    public boolean isAppInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().contains("org.denovogroup.rangzen");
    }
}
