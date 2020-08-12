# google_pay

Google Pay for Flutter. Currently works with Stripe.

Planned:
* Better documentation
* Square support

## Getting Started

The package is very simple.

Before using GooglePay, you need to initalize it with your Stripe token:

```
await GooglePay.initializeGooglePay(<YOUR_TOKEN_HERE>);
```

Then, you can use GooglePay with three callbacks:

```
await GooglePay.openGooglePaySetup(
          price: "5.0",
          onGooglePaySuccess: onSuccess,
          onGooglePayFailure: onFailure,
          onGooglePayCanceled: onCancelled);
```

`onGooglePaySuccess(String token)` : Triggered when a payment goes through, provides a Google Pay token as the first argument to callback.

`onGooglePayFailure()`: Triggered when a payment fails.

`onGooglePayCancelled()` : Triggered when a user cancels payment.

