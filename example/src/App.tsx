import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import {
  multiply,
  sendSMS,
  type SmsResponse,
} from 'react-native-send-sms-android';

export default function App() {
  const [result, setResult] = React.useState<number | undefined>();

  React.useEffect(() => {
    multiply(3, 7).then(setResult);
  }, []);

  const logResult = (smsResult: SmsResponse) => {
    console.log(smsResult);
  };

  const clickyButton = () => {
    sendSMS(
      '555-555-5555',
      'Hello there, this is a test message that is pretty long. It should go on and on for a while. Perhapse we should talk about something interesting. Like the weather. Or maybe the latest episode of the mandalorian. I wish to see the baby. Look, Werner Herzog is maybe the best. Maybe. Probably. Am I over 160 characters yet? I gotta be close. I wonder if I can hit like 160 words while doing this. Seems like a bad idea. I should probably just stop. And yet, I have to continue because I have yet to have it break it into multiple messages?  I wonder if it will do that.  I guess we will find out.'
    ).then(logResult);
  };

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
      <Text onPress={clickyButton}>Clicky</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
