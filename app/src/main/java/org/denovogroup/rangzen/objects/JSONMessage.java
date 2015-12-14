package org.denovogroup.rangzen.objects;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import org.json.JSONObject;

import static com.squareup.wire.Message.Datatype.STRING;
import static com.squareup.wire.Message.Label.REQUIRED;

/**
 * Created by Liran on 11/18/2015.
 *
 * a wrapper for json objects sent as protobuff message
 */
public class JSONMessage extends Message {

    /**
     * The message's structure, as a json string.
     */
    @ProtoField(tag = 1, type = STRING, label = REQUIRED)
    public final String jsonString;


    public JSONMessage(String jsonString) {
        this.jsonString = jsonString;
    }

    public JSONMessage(JSONObject json) {
        this.jsonString = json.toString();
    }

    private JSONMessage(Builder builder) {
        this(builder.jsonString);
        setBuilder(builder);
    }

    public static final class Builder extends Message.Builder<JSONMessage> {

        public String jsonString;

        public Builder() {
        }

        public Builder(JSONMessage message) {
            super(message);
            if (message == null) return;
            this.jsonString = message.jsonString;
        }

        /**
         * The message's structure, as a json string.
         */
        public Builder text(String text) {
            this.jsonString = text;
            return this;
        }

        @Override
        public JSONMessage build() {
            checkRequiredFields();
            return new JSONMessage(this);
        }
    }
}
