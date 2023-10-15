package com.jiwon.payment.repository;

import com.jiwon.payment.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PaymentRepositoryTest {
    @Autowired
    private PaymentRepository<Payment> paymentRepository;

    public Payment returnPayment() {
        Payment payment = new Payment();
        payment.setId("M2NhNjczZTAtZjRjOS00");
        payment.setPaymentId("0D2zu93Z2Hks83D4A3fl");
        payment.setData("VafrJ3eKTF8oPJwibkMFpjpFCluevC/oSLC15k8PGw0mCv7rqGoCSg==");
        payment.setType(Payment.PAYMENT_TYPE.PAYMENT);
        payment.setInstallmentMonths(0);
        payment.setPaymentPrice(110000);
        payment.setCancelPrice(0);
        payment.setVat(10000);
        payment.setStringData(" 446PAYMENT   M2NhNjczZTAtZjRjOS001234567890123456    001125777    1100000000010000                    VafrJ3eKTF8oPJwibkMFpjpFCluevC/oSLC15k8PGw0mCv7rqGoCSg==                                                                                                                                                                                                                                                                                                   ");
        return payment;
    }

    @Test
    @DisplayName("Payment 객체가 데이터베이스에 잘 저장되는지 확인")
    void save() {
        // Given
        Payment payment = returnPayment();

        // When
        paymentRepository.save(payment);

        // Then
        Payment result = paymentRepository.findById(payment.getId());
        assertThat(payment.getId()).isEqualTo(result.getId());
    }

    @Test
    @DisplayName("Payment 객체를 Payment ID를 통해 잘 받아오는지 확인")
    void findByPaymentId() {
        // Given
        Payment payment = returnPayment();
        paymentRepository.save(payment);

        // When
        Payment result = paymentRepository.findByPaymentId("0D2zu93Z2Hks83D4A3fl");

        // Then
        assertThat(payment.getId()).isEqualTo(result.getId());
    }

    @Test
    @DisplayName("Payment Price와 VAT값이 잘 업데이트 되는지 확인")
    void update() {
        // Given
        Payment payment = returnPayment();
        paymentRepository.save(payment);

        // When
        paymentRepository.update(payment.getId(), 120000, 11000);

        // Then
        Payment result = paymentRepository.findById(payment.getId());
        assertThat(result.getPaymentPrice()).isEqualTo(120000);
        assertThat(result.getVat()).isEqualTo(11000);
    }
}
