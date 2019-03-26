import 'dart:async';
import 'dart:convert';
import 'dart:developer';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'payment_details.dart';


typedef GooglePaySuccessCallback = void Function(String token);
typedef GooglePayFailureCallback = void Function();
typedef GooglePayCancelCallback = void Function();

class GooglePay {
  static MethodChannel _channel = MethodChannel('google_pay')..setMethodCallHandler(_nativeCallHandler);
      
  static GooglePaySuccessCallback _googlePaySuccessCallback = null;
  static GooglePayFailureCallback _googlePayFailureCallback = null;
  static GooglePayCancelCallback _googlePayCancelCallback = null;


  static Future openGooglePaySetup(
      {@required String price,
      String currencyCode,
      int priceStatus,
      GooglePaySuccessCallback onGooglePaySuccess,
      GooglePayFailureCallback onGooglePayFailure,
      GooglePayCancelCallback onGooglePayCanceled}) async{

    _googlePaySuccessCallback = onGooglePaySuccess;
    _googlePayFailureCallback = onGooglePayFailure;
    _googlePayCancelCallback = onGooglePayCanceled;

    try {
      var params = <String, dynamic>{
        'price': price
      };
      await _channel.invokeMethod('openGooglePaySetup', params);
    } on PlatformException catch (ex) {
      print('Platform exception in openGooglePaySetup:\n');
      print(ex);
    }
  }

  static Future<dynamic> _nativeCallHandler(MethodCall call) async {
    print('Call to native call handler:\n');
    print(call.method);
    print(call.arguments);
    try {
      switch (call.method) {
        case 'onGooglePayCanceled':
          if (_googlePayCancelCallback != null) {
            print('Executing google pay cancel callback');
            _googlePayCancelCallback();
          }
          break;
        case 'onGooglePaySuccess':
          if (_googlePaySuccessCallback != null) {
            var result = call.arguments;
            _googlePaySuccessCallback(result['token']);
          }
          break;
        case 'onGooglePayFailed':
          if (_googlePayFailureCallback != null) {
            print('Executing google pay failure callback');
            _googlePayFailureCallback();
          }
          break;
        default:
          throw Exception('unknown method called from native');
      }
    } on Exception catch (ex) {
      print('nativeCallHandler caught an exception:\n');
      print(ex);
    }
    return false;
  }

  static Future<String> get platformVersion async {
    String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool> get checkIsReadyToPay async {
    bool isGooglePayAvailable = await _channel.invokeMethod('checkIsReadyToPay');
    return isGooglePayAvailable;
  }
}
