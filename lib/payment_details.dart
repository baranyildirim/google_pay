class PaymentDetails {
  String token;
  
  PaymentDetails(
      {this.token,
      });

  factory PaymentDetails.fromJson(Map<String, dynamic> json) {
    return PaymentDetails(
        token: json["token"]);
  }
}

