package com.jiwon.payment;

import com.jiwon.payment.controller.PaymentController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentApplicationTests {
	@Autowired
	private PaymentController paymentController;

	/*
		↓↓↓↓↓ 정상 프로세스 테스트 ↓↓↓↓↓
	*/

	@Test
	@DisplayName("카드결제 API 정상 프로세스 확인")
	void 카드결제() {
		// Given

		// When

		// Then
	}

	@Test
	@DisplayName("결제취소 API 정상 프로세스 확인")
	void 결제취소() {
		// Given

		// When

		// Then
	}

	@Test
	@DisplayName("결제정보조회 API 정상 프로세스 확인")
	void 결제정보조회() {
		// Given

		// When

		// Then
	}

	/*
		↓↓↓↓↓ 부분취소 API 테스트 케이스 ↓↓↓↓↓
	*/

	@Test
	@DisplayName("부분취소 API 테스트 케이스 1")
	void 부분취소_1() {
		// Given

		// When

		// Then
	}

	@Test
	@DisplayName("부분취소 API 테스트 케이스 2")
	void 부분취소_2() {
		// Given

		// When

		// Then
	}

	@Test
	@DisplayName("부분취소 API 테스트 케이스 3")
	void 부분취소_3() {
		// Given

		// When

		// Then
	}
}
