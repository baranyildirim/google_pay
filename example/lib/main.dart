import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:google_pay/google_pay.dart';

void main() => runApp(MyApp());


class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _googlePayToken = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await GooglePay.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              Text('Running on: $_platformVersion\n'),
              Text('Google pay token: $_googlePayToken\n'),
              FlatButton(
                child: Text("Google Pay Button"),
                onPressed: onButtonPressed,
              )
          ]    
          ),
        ),
      ),
    );
  }

  void onButtonPressed() async{
    setState((){_googlePayToken = "Fetching";});
    try {
      await GooglePay.openGooglePaySetup(
          price: "5.0",
          onGooglePaySuccess: onSuccess,
          onGooglePayFailure: onFailure,
          onGooglePayCanceled: onCancelled);
      setState((){_googlePayToken = "Done Fetching";});
    } on PlatformException catch (ex) {
      setState((){_googlePayToken = "Failed Fetching";});
    }
    
  }

  void onSuccess(dynamic args){ 
    setState((){_googlePayToken = "Success";});
  }

  void onFailure(){ 
    setState((){_googlePayToken = "Failure";});
  }

  void onCancelled(){ 
    setState((){_googlePayToken = "Cancelled";});
  }
}
