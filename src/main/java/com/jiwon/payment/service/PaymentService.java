package com.jiwon.payment.service;

import com.jiwon.payment.controller.parameter.*;
import com.jiwon.payment.entity.Payment;

public interface PaymentService<T extends Payment> {
    PaymentResponseParam createPayment(PaymentRequestParam paymentRequestParam);

    CancelResponseParam cancelPayment(CancelRequestParam cancelRequestParam);

    RetrieveResponseParam searchPayment(RetrieveRequestParam retrieveRequestParam);

    CancelResponseParam partialCancelPayment(CancelRequestParam cancelRequestParam);
}