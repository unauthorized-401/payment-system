package com.jiwon.payment.controller.parameter;

import lombok.Getter;
import lombok.Setter;

/*
    카드결제 API

    Response: 관리번호, string 데이터
*/
@Getter @Setter
public class PaymentResponseParam {
    private String id;

    private String data;
}