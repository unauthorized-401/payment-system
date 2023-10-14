package com.jiwon.payment.repository;

import com.jiwon.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository("PaymentRepository")
public interface PaymentRepository<T extends Payment> extends JpaRepository<T, Long> {
    T findById(String id);
    T findByPaymentId(String paymentId);

    @Modifying
    @Query(value="UPDATE payment SET payment_price=?2, vat=?3 WHERE id=?1", nativeQuery=true)
    void update(String id, long paymentPrice, int vat);
}