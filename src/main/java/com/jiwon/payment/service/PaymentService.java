package com.jiwon.payment.service;

import com.jiwon.payment.entity.Payment;

public interface PaymentService<T extends Payment> {
    void save(T entity);
}