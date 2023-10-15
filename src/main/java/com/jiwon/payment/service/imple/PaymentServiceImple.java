package com.jiwon.payment.service.imple;

import com.jiwon.payment.common.CommonFunction;
import com.jiwon.payment.common.EncryptionService;
import com.jiwon.payment.controller.parameter.*;
import com.jiwon.payment.entity.Payment;
import com.jiwon.payment.exceptions.NotSupportException;
import com.jiwon.payment.exceptions.ResourceNotFoundException;
import com.jiwon.payment.repository.PaymentRepository;
import com.jiwon.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service("PaymentService")
public class PaymentServiceImple<T extends Payment> implements PaymentService<T> {
    @Value("${jasypt.encryptor.password}")
    private String password;

    @Autowired
    private PaymentRepository<T> paymentRepository;

    // 카드번호 락
    private final ConcurrentHashMap<String, Lock> cardNumberLocks = new ConcurrentHashMap<>();

    // 관리번호 락
    private final ConcurrentHashMap<String, Lock> manageNumberLocks = new ConcurrentHashMap<>();

    @Override
    public PaymentResponseParam createPayment(PaymentRequestParam paymentRequestParam) {
        Lock cardLock = cardNumberLocks.computeIfAbsent(paymentRequestParam.getCardNumber(), k -> new ReentrantLock());

        try {
            cardLock.lock();

            CommonFunction.checkPaymentParam(paymentRequestParam);

            // 관리번호 생성
            String manage_num = CommonFunction.generateUniqueId();

            // 부가가치세 계산
            int vat_price = CommonFunction.computeVat(paymentRequestParam.getPaymentPrice(), paymentRequestParam.getVat());

            // 카드번호, 유효기간, CVC 암호화
            String encryptData = CommonFunction.dataEncryption(paymentRequestParam, password);

            // 카드사로 보낼 데이터 생성
            String string_data =
                    CommonFunction.generatePaymentStringData(paymentRequestParam, manage_num, vat_price, encryptData);

            // Create Response
            PaymentResponseParam paymentResponseParam = new PaymentResponseParam();
            paymentResponseParam.setId(manage_num);
            paymentResponseParam.setData(string_data);

            // PAYMENT Table
            Payment payment = new Payment();
            payment.setId(manage_num);
            payment.setData(encryptData);
            payment.setType(Payment.PAYMENT_TYPE.PAYMENT);
            payment.setInstallmentMonths(paymentRequestParam.getInstallmentMonths());
            payment.setPaymentPrice(paymentRequestParam.getPaymentPrice());
            payment.setVat(vat_price);
            payment.setStringData(string_data);

            paymentRepository.save((T) payment);

            // 예시로 4초간 처리 시간을 가정
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return paymentResponseParam;

        } finally {
            cardLock.unlock();
        }
    }

    @Override
    public RetrieveResponseParam searchPayment(RetrieveRequestParam retrieveRequestParam) {
        Optional<Payment> optionalPayment =
                Optional.ofNullable(paymentRepository.findById(retrieveRequestParam.getId()));

        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();

            RetrieveResponseParam retrieveResponseParam = new RetrieveResponseParam();
            retrieveResponseParam.setId(payment.getId());

            // 카드번호, 유효기간, CVC 복호화
            String decryptData = EncryptionService.decryptData(payment.getData(), password);

            StringTokenizer tokens = new StringTokenizer(decryptData, "|");

            String card_num = tokens.hasMoreTokens() ? tokens.nextToken() : "";
            card_num = CommonFunction.stringMasking(card_num);
            String expire_date = tokens.hasMoreTokens() ? tokens.nextToken() : "";
            String cvc = tokens.hasMoreTokens() ? tokens.nextToken() : "";

            retrieveResponseParam.setCardNumber(card_num);
            retrieveResponseParam.setExpirationPeriod(expire_date);
            retrieveResponseParam.setCvc(cvc);

            retrieveResponseParam.setType(payment.getType());

            if (payment.getType() == Payment.PAYMENT_TYPE.PAYMENT) retrieveResponseParam.setPrice(payment.getPaymentPrice());
            else retrieveResponseParam.setPrice(payment.getCancelPrice());

            retrieveResponseParam.setVat(payment.getVat());
            retrieveResponseParam.setInstallmentMonths(payment.getInstallmentMonths());

            return retrieveResponseParam;

        } else {
            // 없는 데이터인 경우 Exception 처리
            log.error("The data is not correct.");
            throw new ResourceNotFoundException();
        }
    }

    @Override
    public CancelResponseParam cancelPayment(CancelRequestParam cancelRequestParam) {
        Lock idLock = manageNumberLocks.computeIfAbsent(cancelRequestParam.getId(), k -> new ReentrantLock());

        try {
            idLock.lock();

            CommonFunction.checkCancelParam(cancelRequestParam);

            Optional<Payment> optionalPayment =
                    Optional.ofNullable(paymentRepository.findById(cancelRequestParam.getId()));

            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                String data = payment.getData();

                // 해당 결제에 대한 취소건이 이미 있는지 확인 (PAYMENT_ID 확인)
                // 결제에 대한 전체취소는 1번만 가능
                Optional<Payment> checkCancelHistory =
                        Optional.ofNullable(paymentRepository.findByPaymentId(cancelRequestParam.getId()));

                if (checkCancelHistory.isPresent()) {
                    log.error("This payment is already canceled completely.");
                    throw new ResourceNotFoundException(cancelRequestParam.getId());
                }

                // 카드번호, 유효기간, CVC 복호화
                String decryptData = EncryptionService.decryptData(data, password);

                StringTokenizer tokens = new StringTokenizer(decryptData, "|");
                String card_num = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String expire_date = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String cvc = tokens.hasMoreTokens() ? tokens.nextToken() : "";

                // 취소로 상태 변경
                Payment newPayment = new Payment();
                newPayment.setType(Payment.PAYMENT_TYPE.CANCEL);

                // 결제 금액과 취소 금액이 동일하지 않아 취소할 수 없음, 에러처리
                if (cancelRequestParam.getCancelPrice() != payment.getPaymentPrice()) {
                    throw new NotSupportException(String.valueOf(cancelRequestParam.getCancelPrice()));
                }

                // 할부개월수는 0으로 저장
                newPayment.setInstallmentMonths("00");
                newPayment.setCancelPrice(cancelRequestParam.getCancelPrice());

                // 부가가치세 값이 없는 경우 결제데이터의 부가가치세 금액으로 취소 진행
                if (Optional.ofNullable(cancelRequestParam.getVat()).isEmpty()) {
                    newPayment.setVat(payment.getVat());

                } else {
                    // 부가가치세 금액이 맞지 않아 취소할 수 없음, 에러처리
                    if (Integer.valueOf(cancelRequestParam.getVat()) != payment.getVat()) {
                        log.error("The vat is not correct.");
                        throw new NotSupportException(String.valueOf(cancelRequestParam.getVat()));
                    }
                    newPayment.setVat(Integer.valueOf(cancelRequestParam.getVat()));
                }

                // 원거래 관리번호, 암호화 데이터 저장
                newPayment.setPaymentId(payment.getId());
                newPayment.setData(payment.getData());

                // 관리번호 생성
                String manage_num = CommonFunction.generateUniqueId();
                newPayment.setId(manage_num);

                // 카드사로 보낼 데이터 생성
                String string_data = CommonFunction.generateCancelStringData(payment, manage_num, card_num, expire_date, cvc);
                newPayment.setStringData(string_data);

                // Create Response
                CancelResponseParam cancelResponseParam = new CancelResponseParam();
                cancelResponseParam.setId(manage_num);
                cancelResponseParam.setData(string_data);

                // PAYMENT Table
                paymentRepository.save((T) newPayment);

                return cancelResponseParam;

            } else {
                // 없는 데이터인 경우 Exception 처리
                log.error("The data is not correct.");
                throw new ResourceNotFoundException();
            }

        } finally {
            idLock.unlock();
        }
    }

    @Override
    public CancelResponseParam partialCancelPayment(CancelRequestParam cancelRequestParam) {
        Lock idLock = manageNumberLocks.computeIfAbsent(cancelRequestParam.getId(), k -> new ReentrantLock());

        try {
            idLock.lock();

            CommonFunction.checkCancelParam(cancelRequestParam);

            Optional<Payment> optionalPayment =
                    Optional.ofNullable(paymentRepository.findById(cancelRequestParam.getId()));

            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                String data = payment.getData();

                // 해당 결제에 대한 취소건이 이미 있는지 확인 (PAYMENT_ID 확인)
                // 결제에 대한 전체취소는 1번만 가능
                Optional<Payment> checkCancelHistory =
                        Optional.ofNullable(paymentRepository.findByPaymentId(cancelRequestParam.getId()));

                if (checkCancelHistory.isPresent()) {
                    log.error("This payment is already canceled completely.");
                    throw new ResourceNotFoundException(cancelRequestParam.getId());
                }

                // 남아있는 결제금액이 없는데 부분 취소를 요청하면 실패
                if (payment.getPaymentPrice() == 0) {
                    log.error("There's no price to cancel.");
                    throw new NotSupportException();
                }

                // 요청한 취소 결제 금액이 남아있는 결제 금액보다 크면 실패
                if (cancelRequestParam.getCancelPrice() > payment.getPaymentPrice()) {
                    log.error("The request price is bigger than remained price.");
                    throw new NotSupportException(String.valueOf(cancelRequestParam.getCancelPrice()));
                }

                // 업데이트된 남은 결제 금액
                long newPaymentPrice = payment.getPaymentPrice() - cancelRequestParam.getCancelPrice();

                // 부가가치세 계산
                // 부가가치세가 null일 경우 자동계산
                int vat_price = CommonFunction.computeVat(cancelRequestParam.getCancelPrice(), cancelRequestParam.getVat());

                // 요청한 취소 부가가치세가 남아있는 부가가치세보다 크면 실패
                if (vat_price > Integer.valueOf(payment.getVat())) {
                    // 단, 마지막 취소이며 요청한 부가가치세가 null일 경우는 제외
                    if (newPaymentPrice == 0 && cancelRequestParam.getVat() == null) {
                        vat_price = Integer.valueOf(payment.getVat());

                    } else {
                        log.error("The request vat is bigger than remained vat.");
                        throw new NotSupportException(cancelRequestParam.getVat());
                    }
                }

                // 남은 요금은 다 취소됐지만 부가가치세가 남아있으면 실패
                if (newPaymentPrice == 0 && vat_price < Integer.valueOf(payment.getVat())) {
                    log.error("The vat is not enough.");
                    throw new NotSupportException(cancelRequestParam.getVat());
                }

                // 업데이트된 남은 부가가치세
                int newVat = Integer.valueOf(payment.getVat()) - vat_price;

                // 카드번호, 유효기간, CVC 복호화
                String decryptData = EncryptionService.decryptData(data, password);

                StringTokenizer tokens = new StringTokenizer(decryptData, "|");
                String card_num = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String expire_date = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String cvc = tokens.hasMoreTokens() ? tokens.nextToken() : "";

                // 취소로 상태 변경
                Payment newPayment = new Payment();
                newPayment.setType(Payment.PAYMENT_TYPE.CANCEL);

                // 할부개월수는 0으로 저장
                newPayment.setInstallmentMonths("00");
                newPayment.setCancelPrice(payment.getPaymentPrice());
                newPayment.setVat(payment.getVat());

                // 원거래 관리번호, 암호화 데이터 저장
                newPayment.setPaymentId(payment.getId());
                newPayment.setData(payment.getData());

                // 관리번호 생성
                String manage_num = CommonFunction.generateUniqueId();
                newPayment.setId(manage_num);

                // 요청된 부분 취소 금액과 부가가치세를 카드사로 보내야 함
                payment.setCancelPrice(cancelRequestParam.getCancelPrice());
                payment.setVat(vat_price);

                // 카드사로 보낼 데이터 생성
                String string_data = CommonFunction.generateCancelStringData(payment, manage_num, card_num, expire_date, cvc);
                newPayment.setStringData(string_data);

                // Create Response
                CancelResponseParam cancelResponseParam = new CancelResponseParam();
                cancelResponseParam.setId(manage_num);
                cancelResponseParam.setData(string_data);

                // 만약 부분취소로 모든 금액이 다 취소됐다면
                if (newPaymentPrice == 0 && newVat == 0) {
                    paymentRepository.save((T)newPayment);

                } else {
                    // 결제 금액과 부가가치세만 업데이트
                    paymentRepository.update(payment.getId(), newPaymentPrice, newVat);
                }

                // 예시로 4초간 처리 시간을 가정
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return cancelResponseParam;

            } else {
                // 없는 데이터인 경우 Exception 처리
                log.error("The data is not correct.");
                throw new ResourceNotFoundException();
            }

        } finally {
            idLock.unlock();
        }
    }
}