package com.jiwon.payment;

import com.jiwon.payment.controller.parameter.*;
import com.jiwon.payment.entity.Payment;
import com.jiwon.payment.repository.PaymentRepository;
import com.jiwon.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

// API 정상작동 테스트 (Thread-Safe 테스트)

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApplicationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentService<Payment> paymentService;

    @Autowired
    private PaymentRepository<Payment> paymentRepository;

    @Test
    @DisplayName("API 정상 프로세스 확인")
    void 정상프로세스() {
        // 카드결제 Given
        PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
        paymentRequestParam.setCardNumber("1234567890123456");
        paymentRequestParam.setExpirationDate("1125");
        paymentRequestParam.setCvc("777");
        paymentRequestParam.setInstallmentMonths("12");
        paymentRequestParam.setPaymentPrice(110000);
        paymentRequestParam.setVat("10000");

        String url1 = "http://localhost:" + port + "/common/payment/pay";

        // 카드결제 When
        ResponseEntity<PaymentResponseParam> response1 = restTemplate.postForEntity(url1, paymentRequestParam, PaymentResponseParam.class);

        // 카드결제 Then
        Payment result1 = paymentRepository.findById(response1.getBody().getId());

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response1.getBody().getId()).isEqualTo(result1.getId());
        assertThat(result1.getType()).isEqualTo(Payment.PAYMENT_TYPE.PAYMENT);

        // 결제취소 Given
        CancelRequestParam cancelRequestParam = new CancelRequestParam();
        cancelRequestParam.setId(result1.getId());
        cancelRequestParam.setCancelPrice(paymentRequestParam.getPaymentPrice());

        String url2 = "http://localhost:" + port + "/common/payment/cancel";

        // 결제취소 When
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CancelRequestParam> requestEntity = new HttpEntity<>(cancelRequestParam, headers);

        ResponseEntity<PaymentResponseParam> response2 = restTemplate.exchange(
                url2,
                HttpMethod.DELETE,
                requestEntity,
                PaymentResponseParam.class
        );

        // 결제취소 Then
        Payment result2 = paymentRepository.findById(response2.getBody().getId());

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getBody().getId()).isEqualTo(result2.getId());
        assertThat(result2.getType()).isEqualTo(Payment.PAYMENT_TYPE.CANCEL);

        // 결제조회 Given
        RetrieveRequestParam retrieveRequestParam = new RetrieveRequestParam();
        retrieveRequestParam.setId(response1.getBody().getId());

        String url3 = "http://localhost:" + port + "/common/payment/retrieve";

        // 결제조회 When
        ResponseEntity<RetrieveResponseParam> response3 = restTemplate.postForEntity(url3, retrieveRequestParam, RetrieveResponseParam.class);

        // 결제조회 Then
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response3.getBody().getId()).isEqualTo(result1.getId());
    }
}
