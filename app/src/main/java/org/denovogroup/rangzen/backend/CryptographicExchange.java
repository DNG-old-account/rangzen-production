/*
 * Copyright (c) 2014, De Novo Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.denovogroup.rangzen.backend;

import org.denovogroup.rangzen.backend.Crypto.PrivateSetIntersection;
import org.denovogroup.rangzen.backend.Crypto.PrivateSetIntersection.ServerReplyTuple;
import org.denovogroup.rangzen.beta.NetworkHandler;
import org.denovogroup.rangzen.beta.ReportsMaker;
import org.denovogroup.rangzen.beta.StopWatch;
import org.denovogroup.rangzen.objects.ClientMessage;
import org.denovogroup.rangzen.objects.RangzenMessage;
import org.denovogroup.rangzen.objects.ServerMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.util.Log;

import okio.ByteString;

/**
 * Performs a single exchange with a Rangzen peer, using the PSI protocol
 * to exchange friends in a zero-knowledge fashion.
 *
 * This class is given input and output streams and communicates over them,
 * oblivious to the underlying network communications used.
 */
public class CryptographicExchange extends Exchange {

  /** PSI computation for the half of the exchange where we're the "client". */
  private PrivateSetIntersection mClientPSI;
  
  /** PSI computation for the half of the exchange where we're the "server". */
  private PrivateSetIntersection mServerPSI;

    /** Friends list received from the remote party */
    private ArrayList<byte[]> remoteBlindedFriends;

  /** ClientMessage received from the remote party. */
  private ClientMessage mRemoteClientMessage;

  /** ServerMessage received from the remote party. */
  private ServerMessage mRemoteServerMessage;

  /** Tag appears in Android log messages. */
  private static final String TAG = "CryptographicExchange";
  
  /**
   * Perform the exchange asynchronously, calling back success or failure on
   * the callback that was passed to the constructor.
   */
  @Override
  public void run() {
    // The error-handling here tries to be as simple as possible: if anything
    // goes wrong that causes an exception, we call the .failure() method on the
    // callback. All the subroutines here (init/send/receive client/server message routines)
    // store intermediate results in instance variables rather than passing them
    // as arguments, and they all throw IOException if something goes wrong with
    // sending/receiving (some subparts can throw other exceptions too, which are
    // handled in the same way here, treated as fatal).
    //
    // When these subroutines called in the following try block fail, they set
    // the error message and error state of the exchange to reasonable values
    // before throwing their exceptions.
    try {
      // TODO(lerner): This (initializing PSIs) is costly, so we may want to
      // do this offline if it's making exchanges slow.
      initializePSIObjects();
        //Send client's friends
        sendFriends();
        //receive server's friends
        receiveFriends();
      // Send client message.
      sendClientMessage();
      // Receive client message.
      receiveClientMessage();
      // Send server message in response to remote client message.
      sendServerMessage();
      // Receive server message.
      receiveServerMessage();

      computeSharedFriends();
      
      setExchangeStatus(Status.SUCCESS);
      callback.success(this, getReportId());
    } catch (Exception e) {  // Treat ALL exceptions as fatal.
        Log.e(TAG, "Exception while run()ing CryptographicExchange: " + e);
        if(getExchangeStatus() == Status.ERROR_RECOVERABLE){
            callback.recover(this, getErrorMessage(), getReportId());
        } else {
            // This status setting should be redundant (whoever threw the exception
            // should have set the status to ERROR before throwing the exception) but
            // this maintains the invariant that whenever callback.failure is called
            // the error code is ERROR.
            setExchangeStatus(Status.ERROR);
            callback.failure(this, getErrorMessage(), getReportId());
        }
    }
  }

  /**
   * Initializes the client and server PSI objects with the node's friends.
   */
  private void initializePSIObjects() throws NoSuchAlgorithmException, 
                                             IllegalArgumentException {
    ArrayList<byte[]> friends = friendStore.getAllFriendsBytes();
    try {
      // The clientPSI object manages the interaction in which we're the "client".
      // The serverPSI object manages the interaction in which we're the "server".
      mClientPSI = new PrivateSetIntersection(friends);
      mServerPSI = new PrivateSetIntersection(friends);
    } catch (NoSuchAlgorithmException e) {
      setExchangeStatus(Status.ERROR); 
      setErrorMessage("No such algorithm when creating PrivateSetIntersection." + e);
      throw e;
    }
  }

    /**
     * Construct and send a ClientMessage to the remote party including
     * blinded friends from the friend store.
     */
    private void sendFriends() throws IOException{
        ArrayList<ByteString> blindedFriends = Crypto.byteArraysToStrings(mClientPSI.encodeBlindedItems());
        ClientMessage cm = new ClientMessage.Builder()
                .blindedFriends(blindedFriends)
                .build();
        if(!lengthValueWrite(out, cm)) {
            setExchangeStatus(Status.ERROR);
            setErrorMessage("Length/value write of client friends failed.");
            throw new IOException("Length/value write of client friends failed, but exception is hidden (see Exchange.java)");
        }
    }


    private void receiveFriends() throws IOException{
        mRemoteClientMessage = lengthValueRead(in, ClientMessage.class);

        if (mRemoteClientMessage == null) {
            setExchangeStatus(Status.ERROR);
            setErrorMessage("Remote client friends was not received.");
            throw new IOException("Remote client friends not received.");
        }

        if (mRemoteClientMessage.blindedFriends == null) {
            setExchangeStatus(Status.ERROR);
            setErrorMessage("Remote client friends field was null");
            throw new IOException("Remote client friends field was null");
        }

        // This can't return null because byteStringsToArrays only returns null
        // when passed null, and we already checked that ClientMessage.blindedFriends
        // isn't null.
        remoteBlindedFriends = Crypto.byteStringsToArrays(mRemoteClientMessage.blindedFriends);
    }

  /**
   * Construct and send a ClientMessage to the remote party including messages
   * from the message store.
   */
  private void sendClientMessage() throws IOException {
      //BETA
      StopWatch watch = new StopWatch();
      //create a message pool to be sent and send each message individually to allow partial data recovery in case of connection loss
      boolean success = true;
      List<RangzenMessage> messagesPool = getMessages();
      //notify the recipient how many items we expect to send him.
      RangzenMessage exchangeInfoMessage = new RangzenMessage(Integer.toString(messagesPool.size()),1d, "notifyReceiver");

      if(!lengthValueWrite(out, exchangeInfoMessage)){
          success = false;
      } else {
          for (RangzenMessage message : messagesPool) {
              watch.start();
              List<RangzenMessage> messageWrapper = new ArrayList<>();
              messageWrapper.add(message);
              ClientMessage cm = new ClientMessage.Builder().messages(messageWrapper)
                      .build();
              if (!lengthValueWrite(out, cm)) {
                  success = false;
              }
              //BETA
              watch.stop();
              if(success){
                  Map<String,Object> edits = new HashMap();
                  int succesful = 0;
                  try {
                      succesful = (Integer) ReportsMaker.getBacklogedReport(getReportId()).get(ReportsMaker.EVENT_SUCCESSFUL_KEY);
                  } catch (JSONException e) {
                      e.printStackTrace();
                  }
                  edits.put(ReportsMaker.EVENT_SUCCESSFUL_KEY, succesful+1);
                  ReportsMaker.editReport(getReportId(), edits);
              } else {

                  Map<String,Object> edits = new HashMap();
                  int failed = 0;
                  try {
                      failed = (Integer) ReportsMaker.getBacklogedReport(getReportId()).get(ReportsMaker.EVENT_FAILED_KEY);
                  } catch (JSONException e) {
                      e.printStackTrace();
                  }
                  edits.put(ReportsMaker.EVENT_SUCCESSFUL_KEY, failed+1);
                  ReportsMaker.editReport(getReportId(), edits);
              }

              if (NetworkHandler.getInstance() != null) {
                  String mThisDeviceUUID = "" + UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());
                  for (RangzenMessage msg : cm.messages) {
                      JSONObject report = ReportsMaker.getMessageExchangeReport(System.currentTimeMillis(), mThisDeviceUUID, getPartnerId(), msg.mId, msg.priority, Math.max(0f, ((float) commonFriends) / friendStore.getAllFriends().size()), "" + watch.getElapsedTime());
                      NetworkHandler.getInstance().sendEventReport(report);
                  }
              }
              //BETA END
          }
      }

      if (!success) {
          setExchangeStatus(Status.ERROR);
          setErrorMessage("Length/value write of client message failed.");
          throw new IOException("Length/value write of client message failed, but exception is hidden (see Exchange.java)");
      }
  }

  /**
   * Receive and return a ClientMessage sent by the remote party.
   *
   * @return A ClientMessage sent by the remote party, or null in the case of an error.
   */
  private void receiveClientMessage() throws IOException {
      //the first message received is a hint, telling the us how many messages will be sent
      int messageCount = 0;
      RangzenMessage exchangeInfo = lengthValueRead(in, RangzenMessage.class);
      if(exchangeInfo != null){
          try {
              messageCount = Integer.parseInt(exchangeInfo.text);
          } catch (Exception e){}
      }
      //BETA
      StopWatch watch = new StopWatch();
      String mThisDeviceUUID = ""+ UUID.nameUUIDFromBytes(BluetoothAdapter.getDefaultAdapter().getAddress().getBytes());

      //if recipient list is not instantiated yet create it
      if(mMessagesReceived == null) mMessagesReceived = new ArrayList<>();

      //Define the get single message task
      class ReceiveSingleMessage implements Callable<String>{

          @Override
          public String call() throws Exception {
              mRemoteClientMessage = lengthValueRead(in, ClientMessage.class);

              if (mRemoteClientMessage == null) {
                  return "Remote client message not received.";
              }

              if (mRemoteClientMessage.messages == null) {
                  return "Remote client messages field was null";
              }
              //Add everything passed in the wrapper to the pool
              mMessagesReceived.addAll(mRemoteClientMessage.messages);
              return null;
          }
      }

      //read from the stream until either times out or get all the messages
      ExecutorService executor = Executors.newSingleThreadExecutor();
      while(mMessagesReceived.size() < messageCount) {
          try {
              //BETA
              watch.start();
              String exception = executor.submit(new ReceiveSingleMessage()).get(EXCHANGE_TIMEOUT, TimeUnit.MILLISECONDS);
              // BETA
              watch.stop();
              JSONObject report = ReportsMaker.getMessageExchangeReport(System.currentTimeMillis(), getPartnerId(), mThisDeviceUUID, mMessagesReceived.get(mMessagesReceived.size()-1).mId, mMessagesReceived.get(mMessagesReceived.size()-1).priority, Math.max(0f, ((float) commonFriends) / friendStore.getAllFriends().size()), ""+watch.getElapsedTime());
              if(NetworkHandler.getInstance() != null) NetworkHandler.getInstance().sendEventReport(report);
              //BETA END
              if (exception != null && !exception.isEmpty()) {
                  executor.shutdown();
                  if (mMessagesReceived.isEmpty()) {
                      setExchangeStatus(Status.ERROR);
                  } else {
                      setExchangeStatus(Status.ERROR_RECOVERABLE);
                  }
                  setErrorMessage(exception);
                  throw new IOException(exception);
              }
          } catch (InterruptedException |ExecutionException | TimeoutException e) {
              executor.shutdown();
              if (mMessagesReceived.isEmpty()) {
                  setExchangeStatus(Status.ERROR);
              } else {
                  setExchangeStatus(Status.ERROR_RECOVERABLE);
              }
              setErrorMessage("Message receiving timed out");
              throw new IOException ("Message receiving timed out");
          }
      }
      executor.shutdown();
  }

  /**
   * Construct a response to the given remote client's ClientMessage and send that
   * response to the remote party.
   */
  private void sendServerMessage() throws NoSuchAlgorithmException, 
                                          IOException {
      if (mRemoteClientMessage == null) {
      throw new IOException("Remote client message was null in sendServerMessage.");
    } else if (remoteBlindedFriends == null) {
      throw new IOException("Remove client message blinded friends is null in sendServerMessage.");
    }

    // Calculate responses that appear in the ServerMessage.
    ServerReplyTuple srt;
    try { 
      srt = mServerPSI.replyToBlindedItems(remoteBlindedFriends);
    } catch (NoSuchAlgorithmException e) {
      Log.wtf(TAG, "No such algorithm in replyToBlindedItems: " + e);
      setExchangeStatus(Status.ERROR);
      setErrorMessage("PSI subsystem is broken, NoSuchAlgorithmException");
      throw e;
    } catch (IllegalArgumentException e) {
      Log.wtf(TAG, "Null passed to replyToBlindedItems on serverPSI? " + e);
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Bad argument to server PSI subsystem. (null remoteBlindedItems?)");
      throw e;
    }

    // Format and create ServerMessage.
    ArrayList<ByteString> doubleBlindedStrings = Crypto.byteArraysToStrings(srt.doubleBlindedItems);
    ArrayList<ByteString> hashedBlindedStrings = Crypto.byteArraysToStrings(srt.hashedBlindedItems);
    ServerMessage sm = new ServerMessage.Builder()    
                                        .doubleBlindedFriends(doubleBlindedStrings)
                                        .hashedBlindedFriends(hashedBlindedStrings)
                                        .build(); 

    // Write out the ServerMessage.
    boolean success = lengthValueWrite(out, sm);
    if (!success) {
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Length/value write of server message failed.");
      throw new IOException("Length/value write of server message failed, but exception is hidden (see Exchange.java)");
    }
  }

  /**
   * Receive server response from remote party.
   *
   * @return A ServerMessage representing the remote party's server message.
   */
  private void receiveServerMessage() throws IOException {
    mRemoteServerMessage = lengthValueRead(in, ServerMessage.class);
    if (mRemoteServerMessage == null) {
      setExchangeStatus(Status.ERROR);
      setErrorMessage("Remote server message was not received.");
      throw new IOException("Remote server message was not received.");
    }
  }

  /**
   * Compute the number of shared friends from the PSI operation and store
   * that number in an instance variable.
   */
  private void computeSharedFriends() throws NoSuchAlgorithmException {
    commonFriends = mClientPSI.getCardinality(getSRTFromServerTuple());
  }

  /**
   * Deserialize the contents of the ServerMessage into a ServerReplyTuple.
   */
  private ServerReplyTuple getSRTFromServerTuple() {
    ArrayList<byte[]> doubleBlindedItems = 
      Crypto.byteStringsToArrays(mRemoteServerMessage.doubleBlindedFriends);
    ArrayList<byte[]> hashedBlindedItems = 
      Crypto.byteStringsToArrays(mRemoteServerMessage.hashedBlindedFriends);
    
    // Since ServerReplyTuple is an inner non-static class, it can't be instantiated
    // without an instance of PrivateSetIntersection, which is it its outer class.
    // Thus we have to use mClientPSI.new.
    return mClientPSI.new ServerReplyTuple(doubleBlindedItems, hashedBlindedItems);
  }


  /**
   * Pass-through constructor to superclass constructor.
   */
  public CryptographicExchange(InputStream in, OutputStream out, boolean asInitiator, 
                               FriendStore friendStore, MessageStore messageStore, 
                               ExchangeCallback callback, String reportId, String partnerId) throws IllegalArgumentException {
    super(in, out, asInitiator, friendStore, messageStore, callback, reportId, partnerId);
  }
}
