package org.denovogroup.rangzen.beta;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.objects.RangzenMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

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
    private static final String DEFAULT_MESSAGE_ID_PREFIX = "pushed_message_";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Bundle extras = intent.getExtras();
        try {
            JSONObject pushJson = new JSONObject(extras.getString("com.parse.Data"));
            String pushedContent = pushJson.getString("alert");
            RangzenMessage receivedMessage = parseMessage(pushedContent);
            if(receivedMessage != null){
                MessageStore store = new MessageStore(context, StorageBase.ENCRYPTION_DEFAULT);
                store.addMessage(receivedMessage.text, receivedMessage.priority, receivedMessage.mId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private RangzenMessage parseMessage(String pushedContent){
        RangzenMessage rangzenMessage = null;

        boolean hasMessage = pushedContent.contains(RANGZEN_MESSAGE_PERFIX) && pushedContent.contains(RANGZEN_MESSAGE_POSTFIX);

        int messageStart = pushedContent.indexOf(RANGZEN_MESSAGE_PERFIX)+RANGZEN_MESSAGE_PERFIX.length();
        int messageEnd = pushedContent.indexOf(RANGZEN_MESSAGE_POSTFIX);

        if(hasMessage && messageEnd > messageStart){
            try{
                String message = pushedContent.substring(messageStart,messageEnd);
                JSONObject jsonMessage = new JSONObject(message);

                //text is mandatory for parsing, the rest can be auto assigned by the system
                String text = jsonMessage.getString(MESSAGE_TEXT_KEY);
                Double priority = jsonMessage.optDouble(MESSAGE_PRIORITY_KEY, 1d);

                Random random = new Random();
                String mId = jsonMessage.optString(MESSAGE_ID_KEY, DEFAULT_MESSAGE_ID_PREFIX+System.currentTimeMillis()+random.nextInt(500));

                rangzenMessage = new RangzenMessage.Builder()
                        .mId(mId)
                        .priority(priority)
                        .text(text).build();

            } catch (JSONException e){
                e.printStackTrace();
            }
        }

        return rangzenMessage;
    }
}
