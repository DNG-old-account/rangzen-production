package org.denovogroup.rangzen.objects;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Liran on 11/18/2015.
 *
 * a wrapper for json objects sent as protobuff message
 */
public class JSONMessage extends Message {

    /**
     * The message's structure, as a json string.
     */
    public final String jsonString;


    public JSONMessage(String jsonString) {
        this.jsonString = jsonString;
    }

    public JSONMessage(JSONObject json) {
        this.jsonString = json.toString();
    }

    public byte[] toByteArray(){
        return jsonString.getBytes();
    }

    public JSONObject jsonObject(){
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
