package com.jiwon.payment.controller;

import com.jiwon.payment.controller.parameter.*;
import com.jiwon.payment.entity.Payment;
import com.jiwon.payment.exceptions.AlreadyUsedException;
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
            PaymentResponseParam paymentResponseParam = paymentService.createPayment(paymentRequestParam);

            // Create Header
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            header.add("data_length", "446");
            header.add("data_type", "PAYMENT");
            header.add("manage_num", paymentResponseParam.getId());

            return ResponseEntity.ok().headers(header).body(paymentResponseParam);

        } catch (AlreadyUsedException e) {
            log.error("AlreadyUsedException : Status Code({})", HttpStatus.IM_USED);

            return ResponseEntity.status(HttpStatus.IM_USED)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

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
            CancelResponseParam cancelResponseParam = paymentService.cancelPayment(cancelRequestParam);

            // Create Header
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            header.add("data_length", "446");
            header.add("data_type", "CANCEL");
            header.add("manage_num", cancelResponseParam.getId());

            return ResponseEntity.ok().headers(header).body(cancelResponseParam);

        } catch (AlreadyUsedException e) {
            log.error("AlreadyUsedException : Status Code({})", HttpStatus.IM_USED);

            return ResponseEntity.status(HttpStatus.IM_USED)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

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
            RetrieveResponseParam retrieveResponseParam = paymentService.searchPayment(retrieveRequestParam);

            return ResponseEntity.ok().body(retrieveResponseParam);

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
            CancelResponseParam cancelResponseParam = paymentService.partialCancelPayment(cancelRequestParam);

            // Create Header
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            header.add("data_length", "446");
            header.add("data_type", "CANCEL");
            header.add("manage_num", cancelResponseParam.getId());

            return ResponseEntity.ok().headers(header).body(cancelResponseParam);

        } catch (AlreadyUsedException e) {
            log.error("AlreadyUsedException : Status Code({})", HttpStatus.IM_USED);

            return ResponseEntity.status(HttpStatus.IM_USED)
                    .header("X-Exception-Type", e.getClass().toString())
                    .header("X-Exception-Cause", e.getMessage())
                    .build();

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