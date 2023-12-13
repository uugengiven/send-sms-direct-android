package com.sendsmsandroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.module.annotations.ReactModule;
import android.telephony.SmsManager;
import android.app.PendingIntent;
import android.content.Intent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.content.Context;
import android.app.Activity;
import android.content.IntentFilter;
import android.util.Log;


class SmsTracker {
  public Integer messageId;
  public Integer messageCount;
  public Integer messageCounter;
  public Integer messageSent;
  public Integer messageDelivered;

  public Boolean wasSent;
  public Boolean wasDelivered;

  public Integer errorsSent;
  public Integer errorsDelivered;

  public WritableArray resultsSent;
  public WritableArray resultsDelivered;
  public WritableMap results;

  public Promise promise;


  public SmsTracker(int count, int messageId, Promise promise) {
    this.messageId = messageId;

    messageCount = count;
    messageCounter = count * 2;
    messageSent = count;
    messageDelivered = count;

    results = new WritableNativeMap();
    resultsSent = new WritableNativeArray();
    resultsDelivered = new WritableNativeArray();

    wasSent = false;
    wasDelivered = false;

    errorsSent = 0;
    errorsDelivered = 0;

    this.promise = promise;
  }
}

@ReactModule(name = SendSmsAndroidModule.NAME)
public class SendSmsAndroidModule extends ReactContextBaseJavaModule {
  public static final String NAME = "SendSmsAndroid";
  private final ReactApplicationContext reactContext;
  private final String TAG = "smsandroidactivity";
  private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor() ;
  private final HashMap<Integer, SmsTracker> messages = new HashMap<>();
  private Integer listenerCount = 0;

  private final Map<String, Object> constants = new HashMap<>();
  private final String SENT = "SMS_SENT";
  private final String DELIVERED = "SMS_DELIVERED";
  private final String RESULT_OK = "SMS sent";
  private final String RESULT_ERROR_GENERIC_FAILURE = "Generic failure";
  private final String RESULT_ERROR_NO_SERVICE = "No service";
  private final String RESULT_ERROR_NULL_PDU = "Null PDU";
  private final String RESULT_ERROR_RADIO_OFF = "Radio off";
  private final String RESULT_OK_DELIVERED = "SMS delivered";
  private final String RESULT_CANCELED_DELIVERED = "SMS cancelled";
  private final String SUCCESS = "Success";
  private final String ERROR = "Error";
  private final String PARTIAL_SUCCESS = "Partial success";
  private final String EVENT_DELIVERED = "EventDelivered";

  public SendSmsAndroidModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    constants.put("RESULT_OK", RESULT_OK);
    constants.put("RESULT_ERROR_GENERIC_FAILURE", RESULT_ERROR_GENERIC_FAILURE);
    constants.put("RESULT_ERROR_NO_SERVICE", RESULT_ERROR_NO_SERVICE);
    constants.put("RESULT_ERROR_NULL_PDU", RESULT_ERROR_NULL_PDU);
    constants.put("RESULT_ERROR_RADIO_OFF", RESULT_ERROR_RADIO_OFF);
    constants.put("RESULT_OK_DELIVERED", RESULT_OK_DELIVERED);
    constants.put("RESULT_CANCELED_DELIVERED", RESULT_CANCELED_DELIVERED);
    constants.put("SUCCESS", SUCCESS);
    constants.put("ERROR", ERROR);
    constants.put("PARTIAL_SUCCESS", PARTIAL_SUCCESS);
    constants.put("EVENT_DELIVERED", EVENT_DELIVERED);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @Override
  public Map<String, Object> getConstants() {
    return constants;
  }

  private void receiveSentResults(Integer messageId, String message) {
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      tracker.messageSent -= 1;
      if(message != RESULT_OK)
      {
        tracker.errorsSent += 1;
      }
      tracker.resultsSent.pushString(message);
      if(tracker.messageSent == 0)
      {
        allSent(messageId);
      }
    }
  }

  private void receiveDeliveredResults(Integer messageId, String message) {
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      tracker.messageDelivered -= 1;
      if(!Objects.equals(message, RESULT_OK_DELIVERED))
      {
        tracker.errorsDelivered += 1;
      }
      tracker.resultsDelivered.pushString(message);
      if(tracker.messageDelivered == 0) {
        allDelivered(messageId);
      }
    }
  }

  private void allSent(Integer messageId) {
    SmsTracker tracker = messages.get(messageId);
    if(tracker == null)
    {
      return;
    }
    tracker.promise.resolve(createSentResults(messageId));
    tracker.wasSent = true;
    checkRemove(messageId);
  }

  private void allDelivered(Integer messageId) {
    SmsTracker tracker = messages.get(messageId);
    if(tracker == null)
    {
      return;
    }
    sendEvent(EVENT_DELIVERED, createDeliveredResults(messageId));
    tracker.wasDelivered = true;
    checkRemove(messageId);
  }

  private void checkRemove(Integer messageId)
  {
    SmsTracker tracker = messages.get(messageId);
    if(tracker == null)
    {
      return;
    }
    if(tracker.wasSent && tracker.wasDelivered)
    {
      messages.remove(messageId);
    }
  }

  private WritableMap createDeliveredResults(Integer messageId) {
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      WritableMap results = new WritableNativeMap();
      int resultsReceivedDelivered = tracker.resultsDelivered.size();
      String deliveredResult = ERROR;
      if(resultsReceivedDelivered == tracker.messageCount)
      {
        if(tracker.errorsDelivered == 0)
        {
          deliveredResult = SUCCESS;
        }
        else if(tracker.errorsDelivered < tracker.messageCount)
        {
          deliveredResult = PARTIAL_SUCCESS;
        }
      }
      results.putInt("id", messageId);
      results.putInt("count", tracker.messageCount);
      results.putInt("errors", tracker.errorsDelivered);
      results.putString("result", deliveredResult);
      results.putArray("results", tracker.resultsDelivered);
      return results;
    }
    return null;
  }

  private WritableMap createSentResults(Integer messageId) {
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      WritableMap results = new WritableNativeMap();
      int resultsReceivedSent = tracker.resultsSent.size();
      String sentResult = ERROR;
      if(resultsReceivedSent == tracker.messageCount)
      {
        if(tracker.errorsSent == 0)
        {
          sentResult = SUCCESS;
        }
        else if(tracker.errorsSent < tracker.messageCount)
        {
          sentResult = PARTIAL_SUCCESS;
        }
      }
      results.putInt("id", messageId);
      results.putInt("count", tracker.messageCount);
      results.putInt("errors", tracker.errorsSent);
      results.putString("result", sentResult);
      results.putArray("results", tracker.resultsSent);
      return results;
    }
    return null;
  }

  private void receiveResults(Integer messageId, String message) {
    Log.d(TAG, message);
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      tracker.messageCounter -= 1;

      if (tracker.messageCounter <= 0) {
        resolvePromise(messageId);
      }
    }
  }

  private void resolvePromise(Integer messageId)
  {
    Log.d(TAG, "resolvePromise called for " + messageId);
    SmsTracker tracker = messages.get(messageId);
    if(tracker == null)
    {
      return;
    }
    tracker.promise.resolve(createSentResults(messageId));
  }

  private void sendEvent(String eventName, @Nullable WritableMap params)
  {
    if(listenerCount == 0)
    {
      return;
    }
    Log.d(TAG, "Sending event " + eventName);
    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
  }

  @ReactMethod
  public void addListener(String eventName) {
    listenerCount += 1;
  }

  @ReactMethod
  public void removeListeners(Integer count) {
    listenerCount -= count;
  }

  @ReactMethod
  public void sendSMS(Integer messageId, String phoneNumber, String message, Promise promise) {
    if(messages.containsKey(messageId))
    {
      promise.resolve("Error: message id already in use");
      return;
    }

    ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
    ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();

    Intent sentIntent = new Intent(SENT + messageId);
    PendingIntent sentPI = PendingIntent.getBroadcast(reactContext, 0, sentIntent, PendingIntent.FLAG_IMMUTABLE);
    Intent deliveredIntent = new Intent(DELIVERED + messageId);
    PendingIntent deliveredPI = PendingIntent.getBroadcast(reactContext, 0, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);

    try {
      SmsManager smsManager = SmsManager.getDefault();
      ArrayList<String> messageArray = smsManager.divideMessage(message);

      SmsTracker tracker = new SmsTracker(messageArray.size(), messageId, promise);
      messages.put(messageId, tracker);

      tracker.results.putInt("messageId", messageId);
      tracker.results.putInt("messageCount", tracker.messageCount);


      for (int i = 0; i < messageArray.size(); i++) {
        sentPendingIntents.add(sentPI);
        deliveredPendingIntents.add(deliveredPI);
      }

      //---when the SMS has been sent---
      reactContext.registerReceiver(new BroadcastReceiver(){
          @Override
          public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive called");
            switch (getResultCode())
            {
              case Activity.RESULT_OK:
                receiveSentResults(messageId, RESULT_OK);
                break;
              case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                receiveSentResults(messageId, RESULT_ERROR_GENERIC_FAILURE);
                break;
              case SmsManager.RESULT_ERROR_NO_SERVICE:
                receiveSentResults(messageId, RESULT_ERROR_NO_SERVICE);
                break;
              case SmsManager.RESULT_ERROR_NULL_PDU:
                receiveSentResults(messageId, RESULT_ERROR_NULL_PDU);
                break;
              case SmsManager.RESULT_ERROR_RADIO_OFF:
                receiveSentResults(messageId, RESULT_ERROR_RADIO_OFF);
                break;
            }
          }
      }, new IntentFilter(SENT + messageId));

      //---when the SMS has been delivered---
      reactContext.registerReceiver(new BroadcastReceiver(){
          @Override
          public void onReceive(Context context, Intent intent) {
              switch (getResultCode())
              {
                  case Activity.RESULT_OK:
                      System.out.println(RESULT_OK_DELIVERED);
                      receiveDeliveredResults(messageId, RESULT_OK_DELIVERED);
                      break;
                  case Activity.RESULT_CANCELED:
                      System.out.println(RESULT_CANCELED_DELIVERED);
                      receiveDeliveredResults(messageId, RESULT_CANCELED_DELIVERED);
                      break;
              }
          }
      }, new IntentFilter(DELIVERED + messageId));


      smsManager.sendMultipartTextMessage(phoneNumber, null, messageArray, sentPendingIntents, deliveredPendingIntents);
    }
    catch (Exception e)
    {

        receiveResults(messageId, "Unknown error");
        //throw e;

    }
  }
}
