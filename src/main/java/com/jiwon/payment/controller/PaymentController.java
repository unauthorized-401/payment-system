package com.jiwon.payment.controller;

import com.jiwon.payment.controller.parameter.*;
import com.jiwon.payment.entity.Payment;
import com.jiwon.payment.common.EncryptionService;
import com.jiwon.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;

@Slf4j
@RestController @Tag(name="Payment")
@RequestMapping(value={"/common/payment"})
@Controller
public class PaymentController {
    @Autowired
    private PaymentService<Payment> paymentService;

    // TODO: 각 API들 에러 코드 정의하여 적용
    // TODO: 입력값 예외처리
    // TODO: 개발자용 로그 주석 삭제

    /*
        카드결제 API

        Request: 카드번호, 유효기간, CVC, 할부개월수, 결제금액, 부가가치세(옵션)
        Response: 관리번호, string 데이터
    */
    @Operation(tags={"Payment"}, summary="Store payment information")
    @PostMapping(value="pay")
    public ResponseEntity<PaymentResponseParam> cardPaying(@RequestBody(required = true) PaymentRequestParam paymentRequestParam) {
        try {
            // Check request
            log.info("======== CardPaying API ========");
            log.info("Card Number: {}", paymentRequestParam.getCardNumber());
            log.info("Card Expiration Date: {}", paymentRequestParam.getExpirationDate());
            log.info("Card CVC: {}", paymentRequestParam.getCvc());
            log.info("Card Installment Months: {}", paymentRequestParam.getInstallmentMonths());
            log.info("Card Payment Price: {}", paymentRequestParam.getPaymentPrice());
            log.info("Card VAT: {}", paymentRequestParam.getVat());
            log.info("================================");

            // 관리번호 생성
            String manage_num = generateUniqueId();

            // 부가가치세 계산
            int vat_price = computeVat(paymentRequestParam);

            // 카드번호, 유효기간, CVC 암호화
            String encryptData = dataEncryption(paymentRequestParam);

            // 카드사로 보낼 데이터 생성
            String string_data = generatePaymentStringData(paymentRequestParam, manage_num, vat_price, encryptData);

            // Create Response
            PaymentResponseParam paymentResponseParam = new PaymentResponseParam();
            paymentResponseParam.setId(manage_num);
            paymentResponseParam.setData(string_data);

            // Create Header
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            header.add("data_length", "446");                   // 데이터 길이를 제외한 총 길이
            header.add("data_type", "PAYMENT");                 // 승인(PAYMENT), 취소(CANCEL)
            header.add("manage_num", manage_num);                           // 관리번호

            // PAYMENT Table
            Payment payment = new Payment();
            payment.setId(manage_num);
            payment.setData(encryptData);
            payment.setType(Payment.PAYMENT_TYPE.PAYMENT);
            payment.setInstallmentMonths(paymentRequestParam.getInstallmentMonths());
            payment.setPaymentPrice(paymentRequestParam.getPaymentPrice());
            payment.setVat(vat_price);
            payment.setStringData(string_data);
            paymentService.save(payment);

            return ResponseEntity.ok().headers(header).body(paymentResponseParam);

        } catch (Exception e) {
            log.error("Exception : {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();
        }
    }

    /*
        결제취소 API

        Request: 관리번호, 취소금액, 부가가치세(옵션)
        Response: 관리번호, string 데이터
    */
    @Operation(tags={"Payment"}, summary="Cancel all payments")
    @PostMapping(value="cancel")
    public ResponseEntity<CancelResponseParam> cardCanceling(@RequestBody(required = true) CancelRequestParam cancelRequestParam) {
        try {
            // Check request
            log.info("======== CardCanceling API ========");
            log.info("Card ID: {}", cancelRequestParam.getId());
            log.info("Card Cancel Price: {}", cancelRequestParam.getCancelPrice());
            log.info("Card VAT: {}", cancelRequestParam.getVat());
            log.info("===================================");

            Optional<Payment> optionalPayment = Optional.ofNullable(paymentService.findById(cancelRequestParam.getId()));

            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                String data = payment.getData();

                // TODO: 해당 결제에 대한 취소건이 이미 있는지 확인

                // TODO: 복호화 과정 함수로 따로 빼기
                // 카드번호, 유효기간, CVC 복호화
                String decryptData = EncryptionService.decryptData(data);

                StringTokenizer tokens = new StringTokenizer(decryptData, "|");
                String card_num = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String expire_date = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String cvc = tokens.hasMoreTokens() ? tokens.nextToken() : "";

                // 취소로 상태 변경
                Payment newPayment = new Payment();
                newPayment.setType(Payment.PAYMENT_TYPE.CANCEL);

                if (cancelRequestParam.getCancelPrice() != payment.getPaymentPrice()) {
                    // TODO: 결제 금액과 취소 금액이 동일하지 않아 취소할 수 없음, 에러처리
                }

                // 할부개월수는 0으로 저장
                newPayment.setInstallmentMonths(0);
                newPayment.setCancelPrice(cancelRequestParam.getCancelPrice());

                // 부가가치세 값이 없는 경우 결제데이터의 부가가치세 금액으로 취소
                if (cancelRequestParam.getVat() == 0) newPayment.setVat(payment.getVat());
                else newPayment.setVat(cancelRequestParam.getVat());

                if (cancelRequestParam.getVat() != 0 && cancelRequestParam.getVat() != payment.getVat()) {
                    // TODO: 부가가치세 금액이 맞지 않아 취소할 수 없음, 에러처리
                }

                // 원거래 관리번호, 암호화 데이터 저장
                newPayment.setPaymentId(payment.getId());
                newPayment.setData(payment.getData());

                // 관리번호 생성
                String manage_num = generateUniqueId();
                newPayment.setId(manage_num);

                // 카드사로 보낼 데이터 생성
                String string_data = generateCancelStringData(payment, manage_num, card_num, expire_date, cvc);
                newPayment.setStringData(string_data);

                // Create Response
                CancelResponseParam cancelResponseParam = new CancelResponseParam();
                cancelResponseParam.setId(manage_num);
                cancelResponseParam.setData(string_data);

                // Create Header
                HttpHeaders header = new HttpHeaders();
                header.setContentType(MediaType.APPLICATION_JSON);
                header.add("data_length", "446");                   // 데이터 길이를 제외한 총 길이
                header.add("data_type", "CANCEL");                 // 승인(PAYMENT), 취소(CANCEL)
                header.add("manage_num", manage_num);                           // 관리번호

                // PAYMENT Table
                paymentService.save(newPayment);

                return ResponseEntity.ok().headers(header).body(cancelResponseParam);

            } else {
                // TODO: 없는 데이터인 경우 Exception 처리

                // 결제에 대한 전체취소는 1번만 가능
            }

            return null;

        } catch (Exception e) {
            log.error("Exception : {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();
        }
    }

    /*
        결제정보조회 API

        Request: 관리번호
        Response: 관리번호, 카드정보(카드번호, 유효기간, CVC), 결제/취소 구분, 금액정보(결제/취소 금액, 부가가치세)
    */
    @Operation(tags={"Payment"}, summary="Get payment information")
    @PostMapping(value="retrieve")
    public ResponseEntity<RetrieveResponseParam> retrieving(@RequestBody(required = true) RetrieveRequestParam retrieveRequestParam) {
        try {
            Optional<Payment> optionalPayment = Optional.ofNullable(paymentService.findById(retrieveRequestParam.getId()));

            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();

                RetrieveResponseParam retrieveResponseParam = new RetrieveResponseParam();
                retrieveResponseParam.setId(payment.getId());

                // 카드번호, 유효기간, CVC 복호화
                String decryptData = EncryptionService.decryptData(payment.getData());

                StringTokenizer tokens = new StringTokenizer(decryptData, "|");
                String card_num = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String expire_date = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String cvc = tokens.hasMoreTokens() ? tokens.nextToken() : "";

                retrieveResponseParam.setCardNumber(card_num);
                retrieveResponseParam.setExpirationPeriod(expire_date);
                retrieveResponseParam.setCvc(cvc);

                retrieveResponseParam.setType(payment.getType());

                if (payment.getType() == Payment.PAYMENT_TYPE.PAYMENT) retrieveResponseParam.setPrice(payment.getPaymentPrice());
                else retrieveResponseParam.setPrice(payment.getCancelPrice());

                retrieveResponseParam.setVat(payment.getVat());

                return ResponseEntity.ok().body(retrieveResponseParam);

            } else {
                // TODO: 없는 데이터인 경우 Exception 처리

                // 결제에 대한 전체취소는 1번만 가능
            }

            return null;

        } catch (Exception e) {
            log.error("Exception : {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();
        }
    }

    /*
        부분취소 API
    */
    @Operation(tags={"Payment"}, summary="Cancel partial payment")
    @PutMapping(value="cancel/partial")
    public ResponseEntity cardPartialCanceling() {
        return new ResponseEntity(HttpStatus.OK);
    }

    // 관리번호 (Unique ID, 20자리) 생성
    public static String generateUniqueId() {
        UUID uuid = UUID.randomUUID();
        String base64encoded = Base64.getUrlEncoder().encodeToString(uuid.toString().getBytes());

        // Base64 인코딩 후 20자리로 자름
        return base64encoded.substring(0, 20);
    }

    // 부가가치세 값이 안 들어왔을 때 계산
    public static int computeVat(PaymentRequestParam paymentRequestParam) {
        // 결제금액 / 11, 소수점 이하 반올림
        if (paymentRequestParam.getVat() == 0) {
            return (int) Math.round(paymentRequestParam.getPaymentPrice() / 11);

        } else {
            return paymentRequestParam.getVat();
        }
    }

    // 카드번호, 유효기간, CVC 데이터를 암호화
    public static String dataEncryption(PaymentRequestParam paymentRequestParam) {
        String card_num = paymentRequestParam.getCardNumber();
        String expire_date = paymentRequestParam.getExpirationDate();
        String cvc = paymentRequestParam.getCvc();

        String full_data = card_num.concat("|").concat(expire_date).concat("|").concat(cvc);
        full_data = EncryptionService.encryptData(full_data);

        return full_data;
    }

    // 카드사로 전송하는 string 데이터 생성 (Payment)
    public static String generatePaymentStringData(PaymentRequestParam paymentRequestParam, String manage_num, int vat_price, String encryptData) {
        String data = " 446PAYMENT   ";

        // 관리번호 20자리
        data = data.concat(manage_num);

        // 카드번호 10-16자리
        String card_num = paymentRequestParam.getCardNumber();
        int card_num_length = card_num.length();
        String card_align = " ".repeat(Math.max(0, 20 - card_num_length));

        data = data.concat(card_num);
        data = data.concat(card_align);

        // 할부개월수 2자리
        data = data.concat("0");
        data = data.concat(String.valueOf(paymentRequestParam.getInstallmentMonths()));

        // 유효기간 4자리
        data = data.concat(paymentRequestParam.getExpirationDate());

        // CVC 3자리
        data = data.concat(paymentRequestParam.getCvc());

        // 결제금액 3-10자리
        String pay_num = String.valueOf(paymentRequestParam.getPaymentPrice());
        int pay_num_length = pay_num.length();
        String pay_align = " ".repeat(Math.max(0, 10 - pay_num_length));

        data = data.concat(pay_align);
        data = data.concat(pay_num);

        // 부가가치세 (옵션)
        String vat = String.valueOf(vat_price);
        int vat_length = vat.length();
        String vat_align = "0".repeat(Math.max(0, 10 - vat_length));

        data = data.concat(vat_align);
        data = data.concat(vat);

        // 원거래 관리번호 (결제시에는 공백)
        String origin_manage_num = " ".repeat(Math.max(0, 20));

        data = data.concat(origin_manage_num);

        // 카드데이터
        int encrypt_length = encryptData.length();
        String encrypt_align = " ".repeat(Math.max(0, 300 - encrypt_length));

        data = data.concat(encryptData);
        data = data.concat(encrypt_align);

        // 나머지 빈칸으로 채움 (총 450자리)
        int total_length = data.length();
        String total_align = " ".repeat(Math.max(0, 450 - total_length));

        data = data.concat(total_align);

        return data;
    }

    // 카드사로 전송하는 string 데이터 생성 (Cancel)
    public static String generateCancelStringData(Payment payment, String manage_num, String card_num, String expire_date, String cvc) {
        String data = " 446CANCEL    ";

        // 관리번호 20자리
        data = data.concat(manage_num);

        // 카드번호 10-16자리
        int card_num_length = card_num.length();
        String card_align = " ".repeat(Math.max(0, 20 - card_num_length));

        data = data.concat(card_num);
        data = data.concat(card_align);

        // 할부개월수 2자리
        data = data.concat("0");
        data = data.concat(String.valueOf(payment.getInstallmentMonths()));

        // 유효기간 4자리
        data = data.concat(expire_date);

        // CVC 3자리
        data = data.concat(cvc);

        // 결제취소금액 3-10자리
        String pay_num = String.valueOf(payment.getPaymentPrice());
        int pay_num_length = pay_num.length();
        String pay_align = " ".repeat(Math.max(0, 10 - pay_num_length));

        data = data.concat(pay_align);
        data = data.concat(pay_num);

        // 부가가치세 (옵션)
        String vat = String.valueOf(payment.getVat());
        int vat_length = vat.length();
        String vat_align = "0".repeat(Math.max(0, 10 - vat_length));

        data = data.concat(vat_align);
        data = data.concat(vat);

        // 원거래 관리번호 (결제시에는 공백)
        data = data.concat(payment.getId());

        // 카드데이터
        String encrypt_data = payment.getData();
        int encrypt_length = encrypt_data.length();
        String encrypt_align = " ".repeat(Math.max(0, 300 - encrypt_length));

        data = data.concat(encrypt_data);
        data = data.concat(encrypt_align);

        // 나머지 빈칸으로 채움 (총 450자리)
        int total_length = data.length();
        String total_align = " ".repeat(Math.max(0, 450 - total_length));

        data = data.concat(total_align);

        return data;
    }
}
