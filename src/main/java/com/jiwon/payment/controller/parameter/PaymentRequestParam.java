package com.jiwon.payment.controller.parameter;

import lombok.Getter;
import lombok.Setter;

/*
    카드결제 API

    Request: 카드번호, 유효기간, CVC, 할부개월수, 결제금액, 부가가치세(옵션)
*/
@Getter @Setter
public class PaymentRequestParam {
    private String cardNumber;

    private String expirationDate;

    private String cvc;

    private String installmentMonths;

    private long paymentPrice;

    private String vat;
}