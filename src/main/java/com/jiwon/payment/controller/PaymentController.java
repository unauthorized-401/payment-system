package com.jiwon.payment.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @Tag(name="Payment")
@RequestMapping(value={"/common/payment"})
@Controller
public class PaymentController {
    /*
        카드결제 API
    */
    @PostMapping(value="pay")
    public ResponseEntity cardPaying() {
        return new ResponseEntity(HttpStatus.OK);
    }

    /*
        결제취소 API
    */
    @PostMapping(value="cancel")
    public ResponseEntity cardCanceling() {
        return new ResponseEntity(HttpStatus.OK);
    }


    /*
        결제정보조회 API
    */
    @GetMapping(value="retrieve")
    public ResponseEntity retrieving() {
        return new ResponseEntity(HttpStatus.OK);
    }


    /*
        부분취소 API
    */
    @PostMapping(value="cancel/partial")
    public ResponseEntity cardPartialCanceling() {
        return new ResponseEntity(HttpStatus.OK);
    }
}
