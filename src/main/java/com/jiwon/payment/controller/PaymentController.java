package com.jiwon.payment.controller;

import com.jiwon.payment.controller.parameter.PaymentParam;
import com.jiwon.payment.controller.parameter.ResultParam;
import com.jiwon.payment.entity.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.UUID;

@Slf4j
@RestController @Tag(name="Payment")
@RequestMapping(value={"/common/payment"})
@Controller
public class PaymentController {
    /*
        카드결제 API

        Request: 카드번호, 유효기간, CVC, 할부개월수, 결제금액, 부가가치세(옵션)
        Response: 관리번호, string 데이터
    */
    @Operation(tags={"Payment"}, summary="Store payment information")
    @PostMapping(value="pay")
    public ResponseEntity<ResultParam> cardPaying(@RequestBody(required = true) PaymentParam paymentParam) {
        try {
            // Check request
            log.info("======== CardPaying API ========");
            log.info("Card Number: {}", paymentParam.getCardNumber());
            log.info("Card Expiration Date: {}", paymentParam.getExpirationDate());
            log.info("Card CVC: {}", paymentParam.getCvc());
            log.info("Card Installment Months: {}", paymentParam.getInstallmentMonths());
            log.info("Card Payment Price: {}", paymentParam.getPaymentPrice());
            log.info("Card VAT: {}", paymentParam.getVat());
            log.info("================================");

            // Create number and data
            String manage_num = generateUniqueId();
            int vat_price = computeVat(paymentParam);
            String string_data = generateStringData(paymentParam, "PAYMENT", manage_num, vat_price);

            // Create Response
            ResultParam resultParam = new ResultParam();
            resultParam.setId(manage_num);
            resultParam.setData(string_data);

            // Create Header
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            header.add("data_length", "446");                   // 데이터 길이를 제외한 총 길이
            header.add("data_type", "PAYMENT");                 // 승인(PAYMENT), 취소(CANCEL)
            header.add("manage_num", manage_num);                           // 관리번호

            // PAYMENT Table
            // TODO: 데이터베이스에 저장
            Payment payment = new Payment();
            payment.setId(manage_num);
            payment.setCardNumber(paymentParam.getCardNumber());
            payment.setExpirationDate(paymentParam.getExpirationDate());
            payment.setCvc(paymentParam.getCvc());
            payment.setType(Payment.PAYMENT_TYPE.PAYMENT);
            payment.setInstallmentMonths(paymentParam.getInstallmentMonths());
            payment.setPaymentPrice(paymentParam.getPaymentPrice());
            payment.setVat(vat_price);
            payment.setData(string_data);
            payment.setResult(Payment.RESULT_TYPE.SUCCESS);
            payment.setInformation(addCommas(paymentParam.getPaymentPrice()) + "(" + addCommas(vat_price) + ")원 결제 성공");

            return ResponseEntity.ok().headers(header).body(resultParam);

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
    @DeleteMapping(value="cancel")
    public ResponseEntity cardCanceling() {
        return new ResponseEntity(HttpStatus.OK);
    }

    /*
        결제정보조회 API

        Request: 관리번호
        Response: 관리번호, 카드정보(카드번호, 유효기간, CVC), 결제/취소 구분, 금액정보(결제/취소 금액, 부가가치세)
    */
    @Operation(tags={"Payment"}, summary="Get payment information")
    @GetMapping(value="retrieve")
    public ResponseEntity retrieving() {
        return new ResponseEntity(HttpStatus.OK);
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
    public static int computeVat(PaymentParam paymentParam) {
        // 결제금액 / 11, 소수점 이하 반올림
        if (paymentParam.getVat() == 0) {
            return (int) Math.round(paymentParam.getPaymentPrice() / 11);

        } else {
            return paymentParam.getVat();
        }
    }

    // 카드번호, 유효기간, CVC 데이터를 암호화
    // TODO: 암호화 진행
    public static String dataEncryption(PaymentParam paymentParam) {
        String card_num = paymentParam.getCardNumber();
        String expire_date = paymentParam.getExpirationDate();
        String cvc = paymentParam.getCvc();

        String full_data = card_num.concat(expire_date).concat(cvc);

        return full_data;
    }

    // 카드사로 전송하는 string 데이터 생성
    public static String generateStringData(PaymentParam paymentParam, String type, String manage_num, int vat_price) {
        String data = " 446";

        if (type.equals("PAYMENT")) {
            data = data.concat("PAYMENT   ");

            // 관리번호 20자리
            data = data.concat(manage_num);

            // 카드번호 10-16자리
            String card_num = paymentParam.getCardNumber();
            int card_num_length = card_num.length();
            String card_align = " ".repeat(Math.max(0, 20 - card_num_length));

            data = data.concat(card_num);
            data = data.concat(card_align);

            // 할부개월수 2자리
            data = data.concat("0");
            data = data.concat(String.valueOf(paymentParam.getInstallmentMonths()));

            // 유효기간 4자리
            data = data.concat(paymentParam.getExpirationDate());

            // CVC 3자리
            data = data.concat(paymentParam.getCvc());

            // 결제금액 3-10자리
            String pay_num = String.valueOf(paymentParam.getPaymentPrice());
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
            String encryptData = dataEncryption(paymentParam);
            int encrypt_length = encryptData.length();
            String encrypt_align = " ".repeat(Math.max(0, 300 - encrypt_length));

            data = data.concat(encryptData);
            data = data.concat(encrypt_align);

            // 나머지 빈칸으로 채움 (총 450자리)
            int total_length = data.length();
            String total_align = " ".repeat(Math.max(0, 450 - total_length));

            data = data.concat(total_align);

        } else {

        }

        return data;
    }

    // 세자리 수마다 콤마(,)를 붙여주는 함수
    public static String addCommas(long number) {
        int origin_number = (int) number;
        String strNumber = String.valueOf(origin_number);
        StringBuilder result = new StringBuilder();

        int count = 0;
        for (int i = strNumber.length() - 1; i >= 0; i--) {
            result.insert(0, strNumber.charAt(i));
            count++;

            // 매 세 자리마다 콤마 추가
            if (count == 3 && i > 0) {
                result.insert(0, ',');
                count = 0;
            }
        }

        return result.toString();
    }
}
