// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: /Users/barathraghavan/code/rangzen/rangzen/buck-out/gen/proto-repo/compile_protobufs__srcs/RangzenMessage.proto
package org.denovogroup.rangzen.objects;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;
import org.json.JSONException;
import org.json.JSONObject;

import static com.squareup.wire.Message.Datatype.DOUBLE;
import static com.squareup.wire.Message.Datatype.INT32;
import static com.squareup.wire.Message.Datatype.STRING;
import static com.squareup.wire.Message.Label.OPTIONAL;
import static com.squareup.wire.Message.Label.REQUIRED;

/**
 * Representation of a single Rangzen message with text and priority.
 */
public final class RangzenMessage extends Message {

  public static final String DEFAULT_TEXT = "";
  public static final Double DEFAULT_TRUST = 0.01D;
    public static final int DEFAULT_PRIORITY = 0;
    public static final String DEFAULT_PSEUDONYM = "";
    public static final String DEFAULT_TIMESTAMP = "";

    public static final String TEXT_KEY = "text";
    public static final String PRIORITY_KEY = "priority";
    public static final String PSEUDONYM_KEY = "pseudonym";

  /**
   * The message's text, as a String.
   */
  @ProtoField(tag = 1, type = STRING, label = REQUIRED)
  public final String text;

  /**
   * The message's trust, as a double.
   */
  @ProtoField(tag = 2, type = DOUBLE, label = REQUIRED)
  public final Double trust;

    /**
     * The message's priority, as a double.
     */
    @ProtoField(tag = 3, type = INT32, label = OPTIONAL)
    public final Integer priority;

    /**
     * The message's sender name, as a String.
     */
    @ProtoField(tag = 4, type = STRING, label = OPTIONAL)
    public final String pseudonym;

    /**
     * The message's timestamp, as a String.
     */
    @ProtoField(tag = 5, type = STRING, label = OPTIONAL)
    public final String timestamp;

  public RangzenMessage(String text, Double trust, Integer priority, String pseudonym, String timestamp) {
    this.text = text;
    this.trust = trust;
    this.priority = priority;
      this.pseudonym = pseudonym;
      this.timestamp = timestamp;
  }

    public RangzenMessage(String text, Double trust, Integer priority, String pseudonym) {
        this.text = text;
        this.trust = trust;
        this.priority = priority;
        this.pseudonym = pseudonym;
        this.timestamp = DEFAULT_TIMESTAMP;
    }

    public RangzenMessage(String text, Double trust) {
        this.text = text;
        this.trust = trust;
        this.priority = DEFAULT_PRIORITY;
        this.pseudonym = DEFAULT_PSEUDONYM;
        this.timestamp = DEFAULT_TIMESTAMP;
    }

  private RangzenMessage(Builder builder) {
    this(builder.text, builder.trust, builder.priority, builder.pseudonym, builder.timestamp);
    setBuilder(builder);
  }

    public static RangzenMessage fromJSON(int securityProfile, JSONObject json){

        return new RangzenMessage(
                json.optString(TEXT_KEY, DEFAULT_TEXT),
                DEFAULT_TRUST,
                json.optInt(PRIORITY_KEY, DEFAULT_PRIORITY),
                json.optString(PSEUDONYM_KEY, DEFAULT_PSEUDONYM),
                SecurityManager.getInstance().getProfile(securityProfile).isTimestamp() ?
                        Utils.convertTimestampToDateString(false, System.currentTimeMillis()) : ""
                );
                //TODO opt location
    }

    public static RangzenMessage fromJSON(int securityProfile, String jsonString){
        JSONObject json;
        try {
            json = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            json = new JSONObject();
        }
        return fromJSON(securityProfile, json);
    }

    /** convert the message into a json object using the supplied security profile restrictions */
    public JSONObject toJSON(int securityProfile){
        JSONObject result = new JSONObject();
        try {
            result.put(TEXT_KEY, this.text);
            result.put(PRIORITY_KEY, this.priority + Utils.makeNoise(0d, 0.003d));

            SecurityProfile profile = SecurityManager.getInstance().getProfile(securityProfile);

            //put optional items based on security profile settings
            if(profile.isPseudonyms()) result.put(PSEUDONYM_KEY, this.pseudonym);
            //if(profile.isShareLocation()) result.put(TEXT_KEY, this.location); //TODO location

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof RangzenMessage)) return false;
    RangzenMessage o = (RangzenMessage) other;
    return equals(text, o.text)
        && equals(priority, o.priority);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = text != null ? text.hashCode() : 0;
      result = result * 37 + (priority != null ? priority.hashCode() : 0);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<RangzenMessage> {

    public String text;
    public Double trust;
      public Integer priority;
      public String pseudonym;
      public String timestamp;

    public Builder() {
    }

    public Builder(RangzenMessage message) {
      super(message);
      if (message == null) return;
      this.text = message.text;
        this.trust = message.trust;
      this.priority = message.priority;
      this.pseudonym = message.pseudonym;
        this.timestamp = message.timestamp;
    }

    /**
     * The message's text, as a String.
     */
    public Builder text(String text) {
      this.text = text;
      return this;
    }

      /**
       * The message's trust, as a double.
       */
      public Builder trust(Double trust) {
          this.trust = trust;
          return this;
      }

    /**
     * The message's priority, as an integer.
     */
    public Builder priority(Integer priority) {
      this.priority = priority;
      return this;
    }

      /**
       * The message's sender name, as a string.
       */
      public Builder pseudonym(String pseudonym) {
          this.pseudonym = pseudonym;
          return this;
      }

      /**
       * The message's timestamp, as a string.
       */
      public Builder timestamp(String timestamp) {
          this.timestamp = timestamp;
          return this;
      }

    @Override
    public RangzenMessage build() {
      checkRequiredFields();
      return new RangzenMessage(this);
    }
  }
}
