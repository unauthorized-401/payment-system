package com.jiwon.payment.repository;

import com.jiwon.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("PaymentRepository")
public interface PaymentRepository<T extends Payment> extends JpaRepository<T, Long> {
    T findById(String id);
}