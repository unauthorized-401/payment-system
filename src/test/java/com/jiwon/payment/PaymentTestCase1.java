package com.jiwon.payment;

import com.jiwon.payment.controller.parameter.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

// 부분취소 API 테스트 케이스 1

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentTestCase1 {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("부분취소 API 테스트 케이스 1")
    void 부분취소_1() {
        // 11000(1000)원 결제 성공
        PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
        paymentRequestParam.setCardNumber("1234567890123456");
        paymentRequestParam.setExpirationDate("1125");
        paymentRequestParam.setCvc("777");
        paymentRequestParam.setInstallmentMonths("12");
        paymentRequestParam.setPaymentPrice(11000);
        paymentRequestParam.setVat("1000");

        String url1 = "http://localhost:" + port + "/common/payment/pay";

        ResponseEntity<PaymentResponseParam> response1 = restTemplate.postForEntity(url1, paymentRequestParam, PaymentResponseParam.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 부분취소 요청 공통 부분
        CancelRequestParam cancelRequestParam = new CancelRequestParam();
        cancelRequestParam.setId(response1.getBody().getId());

        String url2 = "http://localhost:" + port + "/common/payment/cancel/partial";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1100(100)원 취소 성공
        cancelRequestParam.setCancelPrice(1100);
        cancelRequestParam.setVat("100");

        ResponseEntity<PaymentResponseParam> response2 = restTemplate.exchange(
                url2,
                HttpMethod.DELETE,
                new HttpEntity<>(cancelRequestParam, headers),
                PaymentResponseParam.class
        );

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3300원 취소 성공
        cancelRequestParam.setCancelPrice(3300);
        cancelRequestParam.setVat(null);

        ResponseEntity<PaymentResponseParam> response3 = restTemplate.exchange(
                url2,
                HttpMethod.DELETE,
                new HttpEntity<>(cancelRequestParam, headers),
                PaymentResponseParam.class
        );

        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 7000원 실패
        cancelRequestParam.setCancelPrice(7000);
        cancelRequestParam.setVat(null);

        ResponseEntity<PaymentResponseParam> response4 = restTemplate.exchange(
                url2,
                HttpMethod.DELETE,
                new HttpEntity<>(cancelRequestParam, headers),
                PaymentResponseParam.class
        );

        assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        // 6600(700)원 실패
        cancelRequestParam.setCancelPrice(6600);
        cancelRequestParam.setVat("700");

        ResponseEntity<PaymentResponseParam> response5 = restTemplate.exchange(
                url2,
                HttpMethod.DELETE,
                new HttpEntity<>(cancelRequestParam, headers),
                PaymentResponseParam.class
        );

        assertThat(response5.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        // 6600(600)원 취소 성공
        cancelRequestParam.setCancelPrice(6600);
        cancelRequestParam.setVat("600");

        ResponseEntity<PaymentResponseParam> response6 = restTemplate.exchange(
                url2,
                HttpMethod.DELETE,
                new HttpEntity<>(cancelRequestParam, headers),
                PaymentResponseParam.class
        );

        assertThat(response6.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 100원 실패
        cancelRequestParam.setCancelPrice(100);
        cancelRequestParam.setVat(null);

        ResponseEntity<PaymentResponseParam> response7 = restTemplate.exchange(
                url2,
                HttpMethod.DELETE,
                new HttpEntity<>(cancelRequestParam, headers),
                PaymentResponseParam.class
        );

        assertThat(response7.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
