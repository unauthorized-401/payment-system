package com.jiwon.payment.controller;

import com.jiwon.payment.common.CommonFunction;
import com.jiwon.payment.controller.parameter.*;
import com.jiwon.payment.entity.Payment;
import com.jiwon.payment.common.EncryptionService;
import com.jiwon.payment.exceptions.InvalidParameterException;
import com.jiwon.payment.exceptions.NotSupportException;
import com.jiwon.payment.exceptions.ResourceNotFoundException;
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

import java.util.Optional;
import java.util.StringTokenizer;

@Slf4j
@RestController @Tag(name="Payment")
@RequestMapping(value={"/common/payment"})
@Controller
public class PaymentController {
    @Autowired
    private PaymentService<Payment> paymentService;

    /*
        카드결제 API

        Request: 카드번호, 유효기간, CVC, 할부개월수, 결제금액, 부가가치세(옵션)
        Response: 관리번호, string 데이터
    */
    @Operation(tags={"Payment"}, summary="Store payment information")
    @PostMapping(value="pay")
    public ResponseEntity<PaymentResponseParam> cardPaying(
                                            @RequestBody(required = true) PaymentRequestParam paymentRequestParam) {
        try {
            CommonFunction.checkPaymentParam(paymentRequestParam);

            // 관리번호 생성
            String manage_num = CommonFunction.generateUniqueId();

            // 부가가치세 계산
            int vat_price = CommonFunction.computeVat(paymentRequestParam.getPaymentPrice(), paymentRequestParam.getVat());

            // 카드번호, 유효기간, CVC 암호화
            String encryptData = CommonFunction.dataEncryption(paymentRequestParam);

            // 카드사로 보낼 데이터 생성
            String string_data =
                    CommonFunction.generatePaymentStringData(paymentRequestParam, manage_num, vat_price, encryptData);

            // Create Response
            PaymentResponseParam paymentResponseParam = new PaymentResponseParam();
            paymentResponseParam.setId(manage_num);
            paymentResponseParam.setData(string_data);

            // Create Header
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            header.add("data_length", "446");
            header.add("data_type", "PAYMENT");
            header.add("manage_num", manage_num);

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

        } catch (InvalidParameterException e) {
            log.error("HttpClientErrorException : Status Code({})", HttpStatus.METHOD_NOT_ALLOWED);

            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

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
    public ResponseEntity<CancelResponseParam> cardCanceling(
                                            @RequestBody(required = true) CancelRequestParam cancelRequestParam) {
        try {
            CommonFunction.checkCancelParam(cancelRequestParam);

            Optional<Payment> optionalPayment =
                    Optional.ofNullable(paymentService.findById(cancelRequestParam.getId()));

            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                String data = payment.getData();

                // 해당 결제에 대한 취소건이 이미 있는지 확인 (PAYMENT_ID 확인)
                // 결제에 대한 전체취소는 1번만 가능
                Optional<Payment> checkCancelHistory =
                        Optional.ofNullable(paymentService.findByPaymentId(cancelRequestParam.getId()));

                if (checkCancelHistory.isPresent()) {
                    throw new ResourceNotFoundException(cancelRequestParam.getId());
                }

                // 카드번호, 유효기간, CVC 복호화
                String decryptData = EncryptionService.decryptData(data);

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
                newPayment.setInstallmentMonths(0);
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

                // Create Header
                HttpHeaders header = new HttpHeaders();
                header.setContentType(MediaType.APPLICATION_JSON);
                header.add("data_length", "446");
                header.add("data_type", "CANCEL");
                header.add("manage_num", manage_num);

                // PAYMENT Table
                paymentService.save(newPayment);

                return ResponseEntity.ok().headers(header).body(cancelResponseParam);

            } else {
                // 없는 데이터인 경우 Exception 처리
                log.error("The data is not correct.");
                throw new ResourceNotFoundException();
            }

        } catch (ResourceNotFoundException e) {
            log.error("ResourceNotFoundException : Status Code({})", HttpStatus.NOT_FOUND);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

        } catch (InvalidParameterException e) {
            log.error("InvalidParameterException : Status Code({})", HttpStatus.METHOD_NOT_ALLOWED);

            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

        } catch (NotSupportException e) {
            log.error("NotSupportException : Status Code({})", HttpStatus.NOT_ACCEPTABLE);

            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

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
    public ResponseEntity<RetrieveResponseParam> retrieving(
                                            @RequestBody(required = true) RetrieveRequestParam retrieveRequestParam) {
        try {
            Optional<Payment> optionalPayment =
                    Optional.ofNullable(paymentService.findById(retrieveRequestParam.getId()));

            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();

                RetrieveResponseParam retrieveResponseParam = new RetrieveResponseParam();
                retrieveResponseParam.setId(payment.getId());

                // 카드번호, 유효기간, CVC 복호화
                String decryptData = EncryptionService.decryptData(payment.getData());

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

                return ResponseEntity.ok().body(retrieveResponseParam);

            } else {
                // 없는 데이터인 경우 Exception 처리
                log.error("The data is not correct.");
                throw new ResourceNotFoundException();
            }

        } catch (ResourceNotFoundException e) {
            log.error("ResourceNotFoundException : Status Code({})", HttpStatus.NOT_FOUND);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

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

        Request: 관리번호, 취소금액, 부가가치세(옵션)
        Response: 관리번호, string 데이터
    */
    @Operation(tags={"Payment"}, summary="Cancel partial payment")
    @DeleteMapping(value="cancel/partial")
    public ResponseEntity<CancelResponseParam> cardPartialCanceling(
                                            @RequestBody(required = true) CancelRequestParam cancelRequestParam) {
        try {
            CommonFunction.checkCancelParam(cancelRequestParam);

            Optional<Payment> optionalPayment =
                    Optional.ofNullable(paymentService.findById(cancelRequestParam.getId()));

            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                String data = payment.getData();

                // 해당 결제에 대한 취소건이 이미 있는지 확인 (PAYMENT_ID 확인)
                // 결제에 대한 전체취소는 1번만 가능
                Optional<Payment> checkCancelHistory =
                        Optional.ofNullable(paymentService.findByPaymentId(cancelRequestParam.getId()));

                if (checkCancelHistory.isPresent()) {
                    log.error("Thid id({}) is already canceled completely.");
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
                String decryptData = EncryptionService.decryptData(data);

                StringTokenizer tokens = new StringTokenizer(decryptData, "|");
                String card_num = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String expire_date = tokens.hasMoreTokens() ? tokens.nextToken() : "";
                String cvc = tokens.hasMoreTokens() ? tokens.nextToken() : "";

                // 취소로 상태 변경
                Payment newPayment = new Payment();
                newPayment.setType(Payment.PAYMENT_TYPE.CANCEL);

                // 할부개월수는 0으로 저장
                newPayment.setInstallmentMonths(0);
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

                // Create Header
                HttpHeaders header = new HttpHeaders();
                header.setContentType(MediaType.APPLICATION_JSON);
                header.add("data_length", "446");
                header.add("data_type", "CANCEL");
                header.add("manage_num", manage_num);

                // 만약 부분취소로 모든 금액이 다 취소됐다면
                if (newPaymentPrice == 0 && newVat == 0) {
                    paymentService.save(newPayment);

                } else {
                    // 결제 금액과 부가가치세만 업데이트
                    paymentService.update(payment.getId(), newPaymentPrice, newVat);
                }

                return ResponseEntity.ok().headers(header).body(cancelResponseParam);

            } else {
                // 없는 데이터인 경우 Exception 처리
                log.error("The data is not correct.");
                throw new ResourceNotFoundException();
            }

        } catch (ResourceNotFoundException e) {
            log.error("ResourceNotFoundException : Status Code({})", HttpStatus.NOT_FOUND);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

        } catch (InvalidParameterException e) {
            log.error("InvalidParameterException : Status Code({})", HttpStatus.METHOD_NOT_ALLOWED);

            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

        } catch (NotSupportException e) {
            log.error("NotSupportException : Status Code({})", HttpStatus.NOT_ACCEPTABLE);

            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Exception : {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();
        }
    }
}