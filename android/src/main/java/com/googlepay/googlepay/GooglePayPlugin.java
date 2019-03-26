package com.googlepay.googlepay;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolvableResult;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.model.Token;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** GooglePayPlugin */
public class GooglePayPlugin implements MethodCallHandler {
    private static int LOAD_PAYMENT_DATA_REQUEST_CODE = 42;
    private PaymentsClient _paymentsClient;
    private PaymentMethodTokenizationParameters _tokenizationParameters;
    private Activity _activity;

    private GooglePayPlugin(Activity activity) {
        _activity = activity;
        _paymentsClient = Wallet.getPaymentsClient(_activity,
                new Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .build());
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "google_pay");
        channel.setMethodCallHandler(new GooglePayPlugin(registrar.activity()));
        // Register callback when google pay activity is dismissed
        registrar.addActivityResultListener(new PluginRegistry.ActivityResultListener() {
            @Override public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            PaymentData paymentData = PaymentData.getFromIntent(data);
                            // You can get some data on the user's card, such as the brand and last 4 digits
                            CardInfo info = paymentData.getCardInfo();
                            // You can also pull the user address from the PaymentData object.
                            UserAddress address = paymentData.getShippingAddress();
                            // This is the raw JSON string version of your Stripe token.
                            String rawToken = paymentData.getPaymentMethodToken().getToken();
                            // Now that you have a Stripe token object, charge that by using the id
                            Token stripeToken = Token.fromString(rawToken);
                            Map<String, Object> arguments = new LinkedHashMap<>();
                            arguments.put("token", stripeToken.getId());

                            if (stripeToken != null) {
                                channel.invokeMethod("onGooglePaySuccess", arguments);
                            }
                            else {
                                channel.invokeMethod("onGooglePayFailed", null);
                            }
                            break;
                        case Activity.RESULT_CANCELED:
                            channel.invokeMethod("onGooglePayCanceled", null);
                            break;
                        case AutoResolveHelper.RESULT_ERROR:
                            channel.invokeMethod("onGooglePayFailed", null);
                            break;
                        default:
                            channel.invokeMethod("onGooglePayFailed", null);
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
          result.success("Android " + android.os.Build.VERSION.RELEASE);
        }
        if (call.method.equals("initializeGooglePay")) {
          initalizeGooglePay(result, call.argument("stripeKey"));
        }
        if (call.method.equals("checkIsReadyToPay")) {
          getIsReadyToPay(result);
        }
        if (call.method.equals("openGooglePaySetup")){
          openGooglePaySetup(result, call.argument("price"));
        }
        else {
          result.notImplemented();
        }
    }

    private void initalizeGooglePay(Result result, String stripeKey) {
        _tokenizationParameters = createTokenizationParameters(stripeKey);
        result.success(null);
    }

    private void getIsReadyToPay(Result result){
        IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        Task<Boolean> task = _paymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(
                task1 -> {
                    try {
                        boolean task1Result =
                                task1.getResult(ApiException.class);
                        if(task1Result) {
                            result.success(true);
                        } else {
                            result.success(false);
                        }
                    } catch (ApiException exception) {
                        result.success(false);
                    }
                });
    }

    private void openGooglePaySetup(Result result, String price){
        PaymentDataRequest request = createPaymentDataRequest(price);
        if (request != null) {
            AutoResolveHelper.resolveTask(
                    _paymentsClient.loadPaymentData(request),
                    _activity,
                    LOAD_PAYMENT_DATA_REQUEST_CODE);
        }
        result.success(null);
}

    private PaymentDataRequest createPaymentDataRequest(String price) {
        if (_tokenizationParameters == null)
            return null;

        PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                        .setTransactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                        .setTotalPrice(price)
                                        .setCurrencyCode("USD")
                                        .build())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .addAllowedCardNetworks(Arrays.asList(
                                                WalletConstants.CARD_NETWORK_AMEX,
                                                WalletConstants.CARD_NETWORK_DISCOVER,
                                                WalletConstants.CARD_NETWORK_VISA,
                                                WalletConstants.CARD_NETWORK_MASTERCARD))
                                        .build());

        request.setPaymentMethodTokenizationParameters(_tokenizationParameters);
        return request.build();
    }

    private PaymentMethodTokenizationParameters createTokenizationParameters(String stripeKey) {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "stripe")
                .addParameter("stripe:publishableKey", stripeKey)
                .addParameter("stripe:version", "2018-11-08")
                .build();
    }
}

