package org.denovogroup.rangzen.objects;

import java.io.Serializable;

/**
 * Created by Liran on 12/24/2015.
 */
public class BetaExchangeHistory implements Serializable{
    public int receivedMessages;
    public int newMessages;
    public long timestamp;

    public BetaExchangeHistory(int receivedMessages, int newMessages, long timestamp) {
        this.receivedMessages = receivedMessages;
        this.newMessages = newMessages;
        this.timestamp = timestamp;
    }
}
