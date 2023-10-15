package com.jiwon.payment.controller.parameter;

import com.jiwon.payment.entity.Payment;
import lombok.Getter;
import lombok.Setter;

/*
    결제정보조회 API

    Response: 관리번호, 카드정보(카드번호, 유효기간, CVC), 결제/취소 구분, 금액정보(결제/취소 금액, 부가가치세)
*/
@Getter @Setter
public class RetrieveResponseParam {
    private String id;

    private String cardNumber;

    private String expirationPeriod;

    private String cvc;

    private Payment.PAYMENT_TYPE type;

    private long price;

    private int vat;

    private String installmentMonths;
}