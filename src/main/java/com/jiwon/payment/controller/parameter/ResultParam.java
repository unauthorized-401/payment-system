package com.jiwon.payment.controller.parameter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/*
    카드결제/결제취소 API

    Response: 관리번호, string 데이터
*/
@Getter @Setter
public class ResultParam {
    private String id;

    private String data;
}