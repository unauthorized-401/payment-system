package com.jiwon.payment.service.imple;

import com.jiwon.payment.entity.Payment;
import com.jiwon.payment.repository.PaymentRepository;
import com.jiwon.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("PaymentService")
public class PaymentServiceImple<T extends Payment> implements PaymentService<T> {
    @Autowired
    private PaymentRepository<T> paymentRepository;

    @Override
    public void save(T entity) {
        paymentRepository.save(entity);
    }

    @Override
    public T findById(String id) {
        return paymentRepository.findById(id);
    }

    @Override
    public T findByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    @Override
    public void update(String id, long paymentPrice, int vat) {
        paymentRepository.update(id, paymentPrice, vat);
    }
}