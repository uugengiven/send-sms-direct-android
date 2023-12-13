# react-native-send-sms-android

Directly sends SMS if possible in Android. Supports sending to a single number only. If you need delivery confirmation, you can add a listener for delivery reports.

## Installation

```sh
npm install react-native-send-sms-android
```

## Usage

```js
import { SendSms, addDeliveryListener, type SmsResponse } from 'react-native-send-sms-android';

// ...

const logResult = (smsResult: SmsResponse) => {
  console.log(smsResult);
};

const deliveryListener = (smsResult: SmsResponse) => {
  console.log(smsResult);
};

addDeliveryListener(deliveryListener);

const clickyButton = () => {
  sendSMS(
    '555-555-5555', // number to send SMS to, only supports sending to single number
    'Hello there, this is potentially a very long text message and will properly be broken down into parts.', // message of SMS
  ).then(logResult);
};
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
