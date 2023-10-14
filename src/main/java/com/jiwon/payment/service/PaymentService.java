package com.jiwon.payment.service;

import com.jiwon.payment.entity.Payment;

public interface PaymentService<T extends Payment> {
    void save(T entity);

    T findById(String id);
    T findByPaymentId(String paymentId);

    void update(String id, long paymentPrice, int vat);
}