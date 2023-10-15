package com.jiwon.payment.service;

import com.jiwon.payment.controller.parameter.*;
import com.jiwon.payment.entity.Payment;
import com.jiwon.payment.exceptions.InvalidParameterException;
import com.jiwon.payment.exceptions.ResourceNotFoundException;
import com.jiwon.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

// 비즈니스 로직에 대한 신뢰성 테스트

@SpringBootTest
public class PaymentServiceTest {
    @Autowired
    private PaymentService<Payment> paymentService;

    @Autowired
    private PaymentRepository<Payment> paymentRepository;

    @Test
    @DisplayName("부가가치세는 결제 금액보다 작은 금액으로 들어오도록 설계")
    void 부가가치세() {
        // Given
        PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
        paymentRequestParam.setCardNumber("1234567890123456");
        paymentRequestParam.setExpirationDate("1125");
        paymentRequestParam.setCvc("777");
        paymentRequestParam.setInstallmentMonths("12");
        paymentRequestParam.setPaymentPrice(110000);
        paymentRequestParam.setVat("1100000");

        // When
        InvalidParameterException e = assertThrows(InvalidParameterException.class,
                () -> paymentService.createPayment(paymentRequestParam));

        // Then
        assertThat(e.getMessage()).isEqualTo("Invalid Parameter : 'vat'");
    }

    @Test
    @DisplayName("카드정보 암호화 복호화 기능 테스트")
    void 카드정보_암복호화() {
        // Given
        PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
        paymentRequestParam.setCardNumber("1234567890123456");
        paymentRequestParam.setExpirationDate("1125");
        paymentRequestParam.setCvc("777");
        paymentRequestParam.setInstallmentMonths("12");
        paymentRequestParam.setPaymentPrice(110000);
        paymentRequestParam.setVat("10000");

        // When
        PaymentResponseParam paymentResponseParam = paymentService.createPayment(paymentRequestParam);

        RetrieveRequestParam retrieveRequestParam = new RetrieveRequestParam();
        retrieveRequestParam.setId(paymentResponseParam.getId());

        // Then
        RetrieveResponseParam retrieveResponseParam = paymentService.searchPayment(retrieveRequestParam);

        assertThat(retrieveResponseParam.getCardNumber()).isEqualTo("123456*******456");
        assertThat(retrieveResponseParam.getExpirationPeriod()).isEqualTo("1125");
        assertThat(retrieveResponseParam.getCvc()).isEqualTo("777");
    }

    @Test
    @DisplayName("특정 결제건에 대한 전체취소는 한번만 가능")
    void 전체취소_중복() {
        // Given
        PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
        paymentRequestParam.setCardNumber("1234567890123456");
        paymentRequestParam.setExpirationDate("1125");
        paymentRequestParam.setCvc("777");
        paymentRequestParam.setInstallmentMonths("12");
        paymentRequestParam.setPaymentPrice(110000);
        paymentRequestParam.setVat("10000");

        PaymentResponseParam paymentResponseParam = paymentService.createPayment(paymentRequestParam);

        // When
        CancelRequestParam cancelRequestParam = new CancelRequestParam();
        cancelRequestParam.setId(paymentResponseParam.getId());
        cancelRequestParam.setCancelPrice(paymentRequestParam.getPaymentPrice());

        paymentService.cancelPayment(cancelRequestParam);

        // Then
        // 전체취소 재시도
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.cancelPayment(cancelRequestParam));

        assertThat(e.getMessage()).isEqualTo("This payment is already canceled completely.");
    }

    @Test
    @DisplayName("전체 취소 시 할부개월수 데이터는 00으로 저장")
    void 전체취소_할부개월수() {
        // Given
        PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
        paymentRequestParam.setCardNumber("1234567890123456");
        paymentRequestParam.setExpirationDate("1125");
        paymentRequestParam.setCvc("777");
        paymentRequestParam.setInstallmentMonths("12");
        paymentRequestParam.setPaymentPrice(110000);
        paymentRequestParam.setVat("10000");

        PaymentResponseParam paymentResponseParam = paymentService.createPayment(paymentRequestParam);

        // When
        CancelRequestParam cancelRequestParam = new CancelRequestParam();
        cancelRequestParam.setId(paymentResponseParam.getId());
        cancelRequestParam.setCancelPrice(paymentRequestParam.getPaymentPrice());

        CancelResponseParam cancelResponseParam = paymentService.cancelPayment(cancelRequestParam);

        RetrieveRequestParam retrieveRequestParam = new RetrieveRequestParam();
        retrieveRequestParam.setId(cancelResponseParam.getId());

        RetrieveResponseParam retrieveResponseParam = paymentService.searchPayment(retrieveRequestParam);

        // Then
        assertThat(retrieveResponseParam.getInstallmentMonths()).isEqualTo("00");
    }
}
