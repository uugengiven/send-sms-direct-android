package com.sendsmsandroid;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import android.telephony.SmsManager;
import android.app.PendingIntent;
import android.content.Intent;
import java.util.ArrayList;
import android.content.BroadcastReceiver;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import android.content.Context;
import android.app.Activity;
import android.content.IntentFilter;
import android.util.Log;



@ReactModule(name = SendSmsAndroidModule.NAME)
public class SendSmsAndroidModule extends ReactContextBaseJavaModule {
  public static final String NAME = "SendSmsAndroid";
  private final ReactApplicationContext reactContext;
  private final String TAG = "smsandroidactivity";
  private Integer messageCount = 0;
  private Promise _promise;
  private WritableMap results = new WritableNativeMap();

  public SendSmsAndroidModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  public void multiply(double a, double b, Promise promise) {
    promise.resolve(a * b);
  }

  private void receiveResults(String messageId, String message) {
    results.putString("messageId" + messageId, messageId);
    results.putString("message" + messageId, message);
    Log.d(TAG, "received messageId " + messageId);
    Log.d(TAG, "messageCount = " + messageCount.toString());
    this.messageCount--;

    if (this.messageCount <= 0) {
      _promise.resolve(results);
      //_promise.resolve("bleh");
    }
  }

  @ReactMethod
  public void sendSMS(String phoneNumber, String message, Promise promise) {
    results = new WritableNativeMap();
    this._promise = promise;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
    ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
    PendingIntent sentPI;
    PendingIntent deliveredPI;

    try
    {
      SmsManager smsManager = SmsManager.getDefault();
      ArrayList<String> messageArray = smsManager.divideMessage(message);
      this.messageCount = messageArray.size() * 2;
      results.putString("messageCount", messageCount.toString());

      for (Integer i = 0; i < messageArray.size(); i++) {
        Intent intent = new Intent(SENT);
        intent.putExtra("messageId", i.toString());
        sentPI = PendingIntent.getBroadcast(reactContext, 0, intent, PendingIntent.FLAG_MUTABLE);
        intent = new Intent(DELIVERED);
        intent.putExtra("messageId", i.toString());
        Log.d(TAG, "Adding messageId " + i.toString());
        deliveredPI = PendingIntent.getBroadcast(reactContext, 0,intent, PendingIntent.FLAG_MUTABLE);
        sentPendingIntents.add(sentPI);
        deliveredPendingIntents.add(deliveredPI);
      }

      //---when the SMS has been sent---
      reactContext.registerReceiver(new BroadcastReceiver(){
          @Override
          public void onReceive(Context context, Intent intent) {
              Log.d(TAG, "onReceive called " + intent.getStringExtra("messageId"));
              switch (getResultCode())
              {
                case Activity.RESULT_OK:
                  receiveResults(intent.getStringExtra("messageId"), "SMS sent");
                  break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                  receiveResults(intent.getStringExtra("messageId"), "Generic failure");
                  break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                  receiveResults(intent.getStringExtra("messageId"), "No service");
                  break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                  receiveResults(intent.getStringExtra("messageId"), "Null PDU");
                  break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                  receiveResults(intent.getStringExtra("messageId"), "Radio off");
                  break;
              }
          }
      }, new IntentFilter(SENT));

      //---when the SMS has been delivered---
      reactContext.registerReceiver(new BroadcastReceiver(){
          @Override
          public void onReceive(Context context, Intent intent) {
              switch (getResultCode())
              {
                  case Activity.RESULT_OK:
                      System.out.println("SMS delivered");
                      receiveResults(intent.getStringExtra("messageId"), "SMS delivered");
                      break;
                  case Activity.RESULT_CANCELED:
                      System.out.println("SMS not delivered");
                      receiveResults(intent.getStringExtra("messageId"), "SMS not delivered");
                      break;
              }
          }
      }, new IntentFilter(DELIVERED));


      smsManager.sendMultipartTextMessage(phoneNumber, null, messageArray, sentPendingIntents, deliveredPendingIntents);
    }
    catch (Exception e)
    {

        receiveResults("-1", "Unknown error");
        //throw e;

    }
  }
}
