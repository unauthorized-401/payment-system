package com.jiwon.payment.controller.parameter;

import lombok.Getter;
import lombok.Setter;

/*
    결제취소 API

    Request: 관리번호, 취소금액, 부가가치세(옵션)
*/
@Getter @Setter
public class CancelParam {
    // TODO: NULL 값 허용, 값 범위 제한

    private String id;

    private long cancelPrice;

    private int vat;
}