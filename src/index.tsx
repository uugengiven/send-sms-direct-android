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

export function multiply(a: number, b: number): Promise<number> {
  return SendSmsAndroid.multiply(a, b);
}

export function sendSMS(
  phoneNumber: string,
  message: string
): Promise<SmsResponse> {
  return SendSmsAndroid.sendSMS(phoneNumber, message);
}

// create typescript type for the propmise return that holds a number id and string message
export type SmsResponse = {
  id: number;
  message: string;
};
