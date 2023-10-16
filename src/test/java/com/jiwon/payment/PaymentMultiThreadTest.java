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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

// Thread-Safe 테스트

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentMultiThreadTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentService<Payment> paymentService;

    @Autowired
    private PaymentRepository<Payment> paymentRepository;

    @Test
    @DisplayName("하나의 카드번호로 동시에 결제")
    void 동시결제() throws InterruptedException {
        // 모든 결제 요청이 완료될 때까지 대기
        CountDownLatch latch = new CountDownLatch(2);

        // 병렬로 여러 결제 요청 처리
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        int[] returnCodes = new int[2];

        for (int i = 0; i < 2; i++) {
            final int index = i;

            executorService.submit(() -> {
                try {
                    // Given
                    PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
                    paymentRequestParam.setCardNumber("1234567890123456");
                    paymentRequestParam.setExpirationDate("1125");
                    paymentRequestParam.setCvc("777");
                    paymentRequestParam.setInstallmentMonths("12");
                    paymentRequestParam.setPaymentPrice(110000);
                    paymentRequestParam.setVat("10000");

                    String url = "http://localhost:" + port + "/common/payment/pay";

                    // When
                    ResponseEntity<PaymentResponseParam> response = restTemplate.postForEntity(url, paymentRequestParam, PaymentResponseParam.class);
                    returnCodes[index] = response.getStatusCode().value();

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        for(int code: returnCodes) {
            System.out.println("code: "+code);
        }

        // Then: 한 요청은 성공하고 한 요청은 실패해야 함
        boolean hasSuccessResponse = Arrays.stream(returnCodes).anyMatch(code -> code == 200);
        boolean hasErrorResponse = Arrays.stream(returnCodes).anyMatch(code -> code == 226);

        assertThat(hasSuccessResponse && hasErrorResponse).isTrue();
    }

    @Test
    @DisplayName("하나의 결제 관리번호에 대해 동시에 전체취소")
    void 동시전체취소() throws InterruptedException {
        // 모든 전체취소 요청이 완료될 때까지 대기
        CountDownLatch latch = new CountDownLatch(2);

        // 병렬로 여러 전체취소 요청 처리
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
        paymentRequestParam.setCardNumber("1234567890123456");
        paymentRequestParam.setExpirationDate("1125");
        paymentRequestParam.setCvc("777");
        paymentRequestParam.setInstallmentMonths("12");
        paymentRequestParam.setPaymentPrice(110000);
        paymentRequestParam.setVat("10000");

        String url = "http://localhost:" + port + "/common/payment/pay";
        ResponseEntity<PaymentResponseParam> response = restTemplate.postForEntity(url, paymentRequestParam, PaymentResponseParam.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        int[] returnCodes = new int[2];

        for (int i = 0; i < 2; i++) {
            final int index = i;

            executorService.submit(() -> {
                try {
                    // Given
                    CancelRequestParam cancelRequestParam = new CancelRequestParam();
                    cancelRequestParam.setId(response.getBody().getId());
                    cancelRequestParam.setCancelPrice(paymentRequestParam.getPaymentPrice());

                    String url2 = "http://localhost:" + port + "/common/payment/cancel";

                    // When
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<CancelRequestParam> requestEntity = new HttpEntity<>(cancelRequestParam, headers);

                    ResponseEntity<PaymentResponseParam> response2 = restTemplate.exchange(
                            url2,
                            HttpMethod.DELETE,
                            requestEntity,
                            PaymentResponseParam.class
                    );

                    returnCodes[index] = response2.getStatusCode().value();

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        for(int code: returnCodes) {
            System.out.println("code: "+code);
        }

        // Then: 한 요청은 성공하고 한 요청은 실패해야 함
        boolean hasSuccessResponse = Arrays.stream(returnCodes).anyMatch(code -> code == 200);
        boolean hasErrorResponse = Arrays.stream(returnCodes).anyMatch(code -> code == 226);

        assertThat(hasSuccessResponse && hasErrorResponse).isTrue();
    }

    @Test
    @DisplayName("하나의 결제 관리번호에 대해 동시에 부분취소")
    void 동시부분취소() throws InterruptedException {
        // 모든 부분취소 요청이 완료될 때까지 대기
        CountDownLatch latch = new CountDownLatch(2);

        // 병렬로 여러 부분취소 요청 처리
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        PaymentRequestParam paymentRequestParam = new PaymentRequestParam();
        paymentRequestParam.setCardNumber("1234567890123456");
        paymentRequestParam.setExpirationDate("1125");
        paymentRequestParam.setCvc("777");
        paymentRequestParam.setInstallmentMonths("12");
        paymentRequestParam.setPaymentPrice(110000);
        paymentRequestParam.setVat("10000");

        String url = "http://localhost:" + port + "/common/payment/pay";
        ResponseEntity<PaymentResponseParam> response = restTemplate.postForEntity(url, paymentRequestParam, PaymentResponseParam.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        int[] returnCodes = new int[2];

        for (int i = 0; i < 2; i++) {
            final int index = i;

            executorService.submit(() -> {
                try {
                    // Given
                    CancelRequestParam cancelRequestParam = new CancelRequestParam();
                    cancelRequestParam.setId(response.getBody().getId());
                    cancelRequestParam.setCancelPrice(100);

                    String url2 = "http://localhost:" + port + "/common/payment/cancel/partial";

                    // When
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<CancelRequestParam> requestEntity = new HttpEntity<>(cancelRequestParam, headers);

                    ResponseEntity<PaymentResponseParam> response2 = restTemplate.exchange(
                            url2,
                            HttpMethod.DELETE,
                            requestEntity,
                            PaymentResponseParam.class
                    );

                    returnCodes[index] = response2.getStatusCode().value();

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        for(int code: returnCodes) {
            System.out.println("code: "+code);
        }

        // Then: 한 요청은 성공하고 한 요청은 실패해야 함
        boolean hasSuccessResponse = Arrays.stream(returnCodes).anyMatch(code -> code == 200);
        boolean hasErrorResponse = Arrays.stream(returnCodes).anyMatch(code -> code == 226);

        assertThat(hasSuccessResponse && hasErrorResponse).isTrue();
    }
}
