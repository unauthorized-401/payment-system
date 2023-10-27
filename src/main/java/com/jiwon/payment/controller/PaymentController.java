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
    public ResponseEntity<Object> cardPaying(
            @RequestBody(required = true) PaymentRequestParam paymentRequestParam) {
        try {
            PaymentResponseParam paymentResponseParam = paymentService.createPayment(paymentRequestParam);

            HttpHeaders header = createResponseHeaders(paymentResponseParam.getId());
            return ResponseEntity.ok().headers(header).body(paymentResponseParam);

        } catch (AlreadyUsedException e) {
            return handleException(e, HttpStatus.IM_USED);
        } catch (InvalidParameterException e) {
            return handleException(e, HttpStatus.METHOD_NOT_ALLOWED);
        } catch (Exception e) {
            return handleException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
        결제취소 API

        Request: 관리번호, 취소금액, 부가가치세(옵션)
        Response: 관리번호, string 데이터
    */
    @Operation(tags={"Payment"}, summary="Cancel all payments")
    @DeleteMapping(value="cancel")
    public ResponseEntity<Object> cardCanceling(
            @RequestBody(required = true) CancelRequestParam cancelRequestParam) {
        try {
            CancelResponseParam cancelResponseParam = paymentService.cancelPayment(cancelRequestParam);

            HttpHeaders header = createResponseHeaders(cancelResponseParam.getId());
            return ResponseEntity.ok().headers(header).body(cancelResponseParam);

        } catch (AlreadyUsedException e) {
            return handleException(e, HttpStatus.IM_USED);
        } catch (ResourceNotFoundException e) {
            return handleException(e, HttpStatus.NOT_FOUND);
        } catch (InvalidParameterException e) {
            return handleException(e, HttpStatus.METHOD_NOT_ALLOWED);
        } catch (NotSupportException e) {
            return handleException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return handleException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
        결제정보조회 API

        Request: 관리번호
        Response: 관리번호, 카드정보(카드번호, 유효기간, CVC), 결제/취소 구분, 금액정보(결제/취소 금액, 부가가치세)
    */
    @Operation(tags={"Payment"}, summary="Get payment information")
    @PostMapping(value="retrieve")
    public ResponseEntity<Object> retrieving(
            @RequestBody(required = true) RetrieveRequestParam retrieveRequestParam) {
        try {
            RetrieveResponseParam retrieveResponseParam = paymentService.searchPayment(retrieveRequestParam);

            return ResponseEntity.ok().body(retrieveResponseParam);

        } catch (ResourceNotFoundException e) {
            return handleException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return handleException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
        부분취소 API

        Request: 관리번호, 취소금액, 부가가치세(옵션)
        Response: 관리번호, string 데이터
    */
    @Operation(tags={"Payment"}, summary="Cancel partial payment")
    @DeleteMapping(value="cancel/partial")
    public ResponseEntity<Object> cardPartialCanceling(
            @RequestBody(required = true) CancelRequestParam cancelRequestParam) {
        try {
            CancelResponseParam cancelResponseParam = paymentService.partialCancelPayment(cancelRequestParam);

            HttpHeaders header = createResponseHeaders(cancelResponseParam.getId());
            return ResponseEntity.ok().headers(header).body(cancelResponseParam);

        } catch (AlreadyUsedException e) {
            return handleException(e, HttpStatus.IM_USED);
        } catch (ResourceNotFoundException e) {
            return handleException(e, HttpStatus.NOT_FOUND);
        } catch (InvalidParameterException e) {
            return handleException(e, HttpStatus.METHOD_NOT_ALLOWED);
        } catch (NotSupportException e) {
            return handleException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return handleException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpHeaders createResponseHeaders(String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("data_length", "446");
        headers.add("data_type", "PAYMENT");
        headers.add("manage_num", id);
        return headers;
    }

    private ResponseEntity<Object> handleException(Exception e, HttpStatus status) {
        log.error("Exception: {}", e.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Exception-Type", e.getClass().toString());
        headers.add("X-Exception-Cause", e.getMessage());

        return ResponseEntity.status(status)
                .headers(headers)
                .build();
    }
}