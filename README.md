# 카카오페이 손해보험 사전과제
사전과제 1 결제시스템

결제요청을 받아 카드사와 통신하는 인터페이스를 제공하는 결제시스템
- 임베디드 데이터베이스 H2 사용
- 카드결제, 결제취소, 결제정보 조회 REST API 구현
- 단위 테스트로 각 기능 검증
- 선택 문제 부분취소 API 구현 후 테스트 케이스 확인

## 개발 환경
- IntelliJ
- Java 17 + Spring Boot 3.1.4
- H2 2.1.214

## 개발 일정
| Date | Todo                              |
|------|-----------------------------------|
| 10/13 (금) | 요구사항 파악, 프로젝트 생성, 기본 개발환경 구축      |
| 10/14 (토) | 테이블 설계, 필수 및 선택 API 구현, 테스트 코드 작성 |
| 10/15 (일) | 멀티 스레드 구현 및 테스트                   |
| 10/16 (월) | 전체 테스트 후 제출                       |

<details>
<summary>투두리스트</summary>

- [x] 요구사항 파악
- [x] 프로젝트 생성
- [x] 기본 개발환경 구축
  - [x] gitignore 파일 생성
  - [x] readme 파일 생성
  - [x] h2 데이터베이스 연동
  - [x] swagger 연동
- [x] 테이블 설계
- [ ] 필수 API 3개 구현
  - [x] 카드 결제 API
    - [x] API 내부 동작 설계
    - [x] 데이터 암호화 진행
    - [x] 데이터베이스 저장
  - [ ] 결제 취소 API
  - [ ] 결제 정보조회 API
- [ ] 필수 API 테스트 코드 작성
  - [ ] 카드 결제 API
  - [ ] 결제 취소 API
  - [ ] 결제 정보조회 API
- [ ] 선택 API 1개 구현
  - [ ] 부분 취소 API
- [ ] 선택 API 테스트 코드 작성
  - [ ] 부분 취소 API
- [ ] 전체 테스트 후 제출
  - [ ] 요구사항 검토
  - [ ] 전체 테스트
  - [ ] 제출
</details>

## 테이블 설계
테이블명 : PAYMENT

| |COLUMN NAME|INFORMATION|
|-|----------|-------|
|관리번호|ID|UNIQUE ID, 20자리|
|카드번호|CARD_NUMBER|10-16자리|
|유효기간|EXPIRATION_DATE|4자리, MMYY|
|CVC|CVC|3자리|
|결제/취소 구분|TYPE|PAYMENT/CANCEL/PARTIAL_CANCEL|
|할부개월수|INSTALLMENT_MONTHS|0-12, 0은 일시불|
|결제금액|PAYMENT_PRICE|100원 이상 10억원 이하 숫자|
|취소금액|CANCEL_PRICE|100원 이상 10억원 이하 숫자|
|부가가치세|VAT|결제금액/11, 소수점 이하 반올림|
|카드사데이터|DATA|공통헤더 부문 + 데이터 부문|
|결과|RESULT|성공/실패|
|설명|INFORMATION|성공 혹은 실패 금액 및 이유|

## 문제 해결

### 카드번호, 유효기간, CVC 데이터 암호화 및 복호화
Jasypt 라이브러리 + PBEWithMD5AndDES 암호화 알고리즘
1. Jasypt 의존성 추가
2. `@EnableEncryptableProperties` 어노테이션 사용하여 암호화 속성 활성화
3. `JasyptStringEncryptor` 빈 사용하여 데이터 암호화 및 복호화 코드 작성
4. EncryptionConfig, EncryptionService 파일 참고

## 빌드 및 실행 방법

### Swagger를 이용한 테스트
1. [Swagger](http://localhost:8080/swagger-ui/index.html)에 접속한다.
2. 아래 예시 JSON 데이터를 이용해 API들을 테스트한다.

POST /common/payment/api
```
{
  "cardNumber": "1234567890123456",
  "expirationDate": "1125",
  "cvc": "777",
  "installmentMonths": 0,
  "paymentPrice": 110000,
  "vat": 10000
}
```