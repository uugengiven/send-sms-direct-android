import { NativeModules, Platform, NativeEventEmitter } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-send-sms-android' doesn't seem to be linked. Make sure: \n\n` +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const SendSmsAndroid =
  Platform.OS === 'android' && NativeModules.SendSmsAndroid
    ? NativeModules.SendSmsAndroid
    : new Proxy(
        {},
        {
          get() {
            throw new Error(LINKING_ERROR);
          },
        }
      );

export function addDeliveryListener(callback: Function): Function {
  const eventEmitter = new NativeEventEmitter(NativeModules.SendSmsAndroid);
  let eventListener = eventEmitter.addListener('EventDelivered', (event) => {
    callback(event);
  });

  // Removes the listener once unmounted
  return () => {
    eventListener.remove();
  };
}

export function SendSms(
  phoneNumber: string,
  message: string
): Promise<SmsResponse> {
  if (Platform.OS === 'ios') {
    return Promise.reject('iOS not supported');
  }
  const id = Math.floor(Math.random() * 10000000); // this allows us to have intents for each message inside android
  return SendSmsAndroid.sendSMS(id, phoneNumber, message);
}

export type SmsResponse = {
  id: number;
  count: number;
  errors: number;
  results: string[];
  result: string;
};
