package com.sendsmsandroid;

import androidx.annotation.NonNull;

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
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import android.content.Context;
import android.app.Activity;
import android.content.IntentFilter;
import android.util.Log;


class SmsTracker {
  public Integer messageId;
  public Integer messageCount;
  public Integer messageSent;
  public Integer messageDelivered;

  public WritableArray resultsSent;
  public WritableArray resultsDelivered;
  public WritableMap results;

  public Promise promise;


  public SmsTracker(int count, int messageId, Promise promise) {
    this.messageId = messageId;

    messageCount = count * 2;
    messageSent = count;
    messageDelivered = count;

    results = new WritableNativeMap();
    resultsSent = new WritableNativeArray();
    resultsDelivered = new WritableNativeArray();

    this.promise = promise;
  }
}

@ReactModule(name = SendSmsAndroidModule.NAME)
public class SendSmsAndroidModule extends ReactContextBaseJavaModule {
  public static final String NAME = "SendSmsAndroid";
  private final ReactApplicationContext reactContext;
  private final String TAG = "smsandroidactivity";

  private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor() ;

  private final Hashtable<Integer, SmsTracker> messages = new Hashtable<>();

  public SendSmsAndroidModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  private void receiveSentResults(Integer messageId, String message) {
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      tracker.messageSent -= 1;
      tracker.resultsSent.pushString(message);
      receiveResults(messageId, message);
    }
  }

  private void receiveDeliveredResults(Integer messageId, String message) {
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      tracker.messageDelivered -= 1;
      tracker.resultsDelivered.pushString(message);
      receiveResults(messageId, message);
    }
  }

  private void receiveResults(Integer messageId, String message) {
    Log.d(TAG, message);
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      tracker.messageCount -= 1;

      if (tracker.messageCount <= 0) {
        resolvePromise(messageId);
      }
    }
  }

  private void resolvePromise(Integer messageId)
  {
    SmsTracker tracker = messages.get(messageId);
    if(tracker != null) {
      tracker.results.putArray("SentResults", tracker.resultsSent);
      tracker.results.putArray("DeliveredResults", tracker.resultsDelivered);
      tracker.promise.resolve(tracker.results);
    }
  }

  @ReactMethod
  public void sendSMS(Integer messageId, String phoneNumber, String message, Integer timeout, Promise promise) {
    if(messages.containsKey(messageId))
    {
      promise.resolve("Error: message id already in use");
      return;
    }

    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
    ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();

    Intent sentIntent = new Intent(SENT + messageId);
    PendingIntent sentPI = PendingIntent.getBroadcast(reactContext, 0, sentIntent, PendingIntent.FLAG_MUTABLE);
    Intent deliveredIntent = new Intent(DELIVERED + messageId);
    PendingIntent deliveredPI = PendingIntent.getBroadcast(reactContext, 0, deliveredIntent, PendingIntent.FLAG_MUTABLE);

    Runnable task = () -> {
      if(messages.containsKey(messageId))
      {
        Log.d(TAG, "Removing " + messageId.toString());
        resolvePromise(messageId);
        messages.remove(messageId);
      }
    };
    ses.schedule(task, timeout, TimeUnit.MILLISECONDS);

    try {
      SmsManager smsManager = SmsManager.getDefault();
      ArrayList<String> messageArray = smsManager.divideMessage(message);

      SmsTracker tracker = new SmsTracker(messageArray.size(), messageId, promise);
      messages.put(messageId, tracker);

      tracker.results.putInt("messageId", messageId);
      tracker.results.putInt("messageCount", tracker.messageSent);


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
                receiveSentResults(messageId, "SMS sent");
                break;
              case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                receiveSentResults(messageId, "Generic failure");
                break;
              case SmsManager.RESULT_ERROR_NO_SERVICE:
                receiveSentResults(messageId, "No service");
                break;
              case SmsManager.RESULT_ERROR_NULL_PDU:
                receiveSentResults(messageId, "Null PDU");
                break;
              case SmsManager.RESULT_ERROR_RADIO_OFF:
                receiveSentResults(messageId, "Radio off");
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
                      System.out.println("SMS delivered");
                      receiveDeliveredResults(messageId, "SMS delivered");
                      break;
                  case Activity.RESULT_CANCELED:
                      System.out.println("SMS not delivered");
                      receiveDeliveredResults(messageId, "SMS cancelled");
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
