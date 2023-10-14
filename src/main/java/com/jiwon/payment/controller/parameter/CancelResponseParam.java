package com.jiwon.payment.controller.parameter;

import lombok.Getter;
import lombok.Setter;

/*
    결제취소 API

    Response: 관리번호, string 데이터
*/
@Getter @Setter
public class CancelResponseParam {
    private String id;

    private String data;
}