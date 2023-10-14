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

    // 카드번호: 10-16자리
    private String cardNumber;

    // 유효기간: 4자리, MMYY
    private String expirationDate;

    // CVC: 3자리
    private String cvc;

    // 결제/취소 구분
    private PAYMENT_TYPE type;

    // 할부개월수: 0-12, 0은 일시불
    private int installmentMonths;

    // 결제금액: 100원 이상 10억원 이하 숫자
    private long paymentPrice;

    // 취소금액: 100원 이상 10억원 이하 숫자
    private long cancelPrice;

    // 부가가치세: 결제금액/11, 소수점 이하 반올림, 옵션
    private long vat;

    // 카드사데이터: 공통헤더 부문 + 데이터 부문
    @Column(length=450)
    private String data;

    // 결과: 성공/실패
    private RESULT_TYPE result;

    // 설명: 성공 혹은 실패 금액 및 이유
    private String information;

    public enum PAYMENT_TYPE {
        PAYMENT,        // 결제
        CANCEL          // 취소
    }

    public enum RESULT_TYPE {
        SUCCESS,        // 성공
        FAIL            // 실패
    }
}