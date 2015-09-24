package org.denovogroup.rangzen.ui;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;

import io.fabric.sdk.android.Fabric;
import org.denovogroup.rangzen.backend.FriendStore;
import org.denovogroup.rangzen.backend.MessageStore;
import org.denovogroup.rangzen.backend.StorageBase;
import org.denovogroup.rangzen.beta.CustomParsePushReceiver;
import org.denovogroup.rangzen.objects.RangzenMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Liran on 9/1/2015.
 */
public class RangzenApplication extends Application{

    private static String TAG = "RangzenApplication";

    @Override
    public final void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        /** Initialize Parse */
        Parse.enableLocalDatastore(getApplicationContext());
        Parse.initialize(this, "4XIuXX5JTtAQFQFPJ9M7L1E7o2Tr3oN67bf3hiRU", "02cnF9azewOD0MPqpmfSWpi5TB2XyRTQDY3Rrxno");
        ParsePush.subscribeInBackground("beta");
        ParseInstallation thisInstallation = ParseInstallation.getCurrentInstallation();
        FriendStore store = new FriendStore(getApplicationContext(), StorageBase.ENCRYPTION_DEFAULT);
        //put this device's public id into the installation table to allow private push notification sending
        thisInstallation.put("publicId",store.getPublicDeviceIDString());
        String myPublicId = store.getPublicDeviceIDString();
        thisInstallation.put("readableId",myPublicId.substring(myPublicId.length()-9));
        thisInstallation.saveInBackground();

        /**Try to get friends and messages from parse and register if first run*/
        ParseQuery<ParseObject> query = ParseQuery.getQuery("UserData");
        query.whereEqualTo("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> installationsList, ParseException e) {
                if (e == null) {
                    if (installationsList != null && installationsList.size() > 0) {

                        //get friends
                        FriendStore fStore = new FriendStore(getApplicationContext(), StorageBase.ENCRYPTION_DEFAULT);
                        ArrayList<String> friends = (ArrayList<String>) installationsList.get(0).get("friends");
                        if(friends != null) {
                            for (String friend : friends) {
                                byte[] bytes = fStore.base64ToBytes(friend);
                                // Try to add the friend to the FriendStore, if they're not null.
                                if (bytes != null) {
                                    //this will retern false if the friend already existed
                                    fStore.addFriendBytes(bytes);
                                } else {
                                    //Error adding friend
                                }
                            }
                        }

                        //get initial set of messages
                        String pushedContent = (String) installationsList.get(0).get("messages");
                        List<RangzenMessage> receivedMessages = CustomParsePushReceiver.parseMessage(pushedContent);
                        if (receivedMessages != null) {
                            MessageStore mStore = new MessageStore(getApplicationContext(), StorageBase.ENCRYPTION_DEFAULT);
                            for (RangzenMessage receivedMessage : receivedMessages) {
                                mStore.addMessage(receivedMessage.text, receivedMessage.priority, receivedMessage.mId);
                            }
                        }

                    } else {
                        //user is not registered to parse yet, need to register
                        /* put device public id in the parse database, this is used
                         * in order to make a friends list later on
                        */
                        FriendStore store = new FriendStore(getApplicationContext(), StorageBase.ENCRYPTION_DEFAULT);
                        ParseObject userdata = new ParseObject("UserData");
                        userdata.put("installationId", ParseInstallation.getCurrentInstallation().getInstallationId());
                        userdata.put("publicId", store.getPublicDeviceIDString());
                        userdata.saveInBackground();
                    }
                } else {
                    e.printStackTrace();
                }
            }
        });
    }
}
