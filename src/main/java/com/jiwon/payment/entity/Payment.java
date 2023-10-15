package com.jiwon.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Payment {
    // 관리번호: Unique ID, 20자리
    @Id
    private String id;

    // 원거래 관리번호 (취소시에만)
    private String paymentId;

    // 카드번호, 유효기간, CVC 암호화 데이터
    private String data;

    // 결제/취소 구분
    private PAYMENT_TYPE type;

    // 할부개월수: 00-12, 00은 일시불
    private String installmentMonths;

    // 결제금액: 100원 이상 10억원 이하 숫자
    private long paymentPrice;

    // 취소금액: 100원 이상 10억원 이하 숫자
    private long cancelPrice;

    // 부가가치세: 결제금액/11, 소수점 이하 반올림, 옵션
    private int vat;

    // 카드사데이터: 공통헤더 부문 + 데이터 부문
    @Column(length=450)
    private String stringData;

    public enum PAYMENT_TYPE {
        PAYMENT,        // 결제
        CANCEL          // 취소
    }
}