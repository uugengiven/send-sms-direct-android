import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-send-sms-android' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const SendSmsAndroid = NativeModules.SendSmsAndroid
  ? NativeModules.SendSmsAndroid
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function sendSMS(
  phoneNumber: string,
  message: string,
  timeout: number = 10000
): Promise<SmsResponse> {
  const id = Math.floor(Math.random() * 1000000000); // this allows us to have intents for each message inside android
  return SendSmsAndroid.sendSMS(id, phoneNumber, message, timeout);
}

// create typescript type for the propmise return that holds a number id and string message
export type SmsResponse = {
  sentResponse: string[];
  deliveredResponse: string[];
};
