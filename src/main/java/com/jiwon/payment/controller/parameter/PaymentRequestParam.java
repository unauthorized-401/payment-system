package com.jiwon.payment.controller.parameter;

import lombok.Getter;
import lombok.Setter;

/*
    카드결제 API

    Request: 카드번호, 유효기간, CVC, 할부개월수, 결제금액, 부가가치세(옵션)
*/
@Getter @Setter
public class PaymentRequestParam {
    // TODO: NULL 값 허용, 값 범위 제한

    private String cardNumber;

    private String expirationDate;

    private String cvc;

    private int installmentMonths;

    private long paymentPrice;

    private int vat;
}