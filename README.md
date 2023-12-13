# react-native-send-sms-android

Directly sends SMS if possible in Android, returns results as a promise.

## Installation

```sh
npm install react-native-send-sms-android
```

## Usage

```js
import { SendSms, type SmsResponse } from 'react-native-send-sms-android';

// ...

const logResult = (smsResult: SmsResponse) => {
    console.log(`SMS sent successfully: ${smsResult.success}`);
    console.log(smsResult);
  };

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
