## 개발 환경
- IntelliJ
- Java 17 + Spring Boot 3.1.4
- H2 2.1.214

## 개발 일정
|Date|Todo|
|----|----|
|10/13(금)|요구사항 파악, 기본 개발환경 구축 |
|10/14(토)|테이블 설계, API 구현, 테스트 코드 작성 |
|10/15(일)|멀티 스레드 구현 및 테스트 |
|10/16(월)|전체 테스트 후 제출 |

## 문제 해결
### 데이터 암호화 및 복호화
Jasypt 라이브러리 + PBEWithMD5AndDES 암호화 알고리즘
1. Jasypt 의존성 추가
2. `@EnableEncryptableProperties` 사용하여 암호화 속성 활성화
3. `JasyptStringEncryptor` 빈 사용하여 데이터 암호화 및 복호화 코드 작성
4. EncryptionConfig, EncryptionService 파일 참고

### 멀티 스레드 환경 대비
문제 파악
- 하나의 카드번호로 동시에 결제하는 테스트 코드 작성
    - `CountDownLatch`와 `ExecutorService`를 사용하여 결제건이 병렬로 처리되도록 구현
    - 같은 카드번호를 가지고 동시에 결제를 시도하면 두 결제 모두 성공 처리

<p align="center">
  <img src="./readme/Payment.png" width="350px"></img>
</p>

해결 방법
- 같은 카드번호일 때만 문제가 됨으로 카드번호에 락을 부여하는 방법 선택
- 또한 전체취소/부분취소일 경우 관리번호에 락을 부여하도록 구현

## 빌드 및 실행 방법
### 프로젝트 빌드 및 실행
1. 깃허브에서 프로젝트를 받아온다.
```
git clone https://github.com/unauthorized-401/Payment.git
```

2. 프로젝트를 빌드하고 실행한다.
```
# 프로젝트 빌드
cd Payment
gradlew build

# 프로젝트 실행
cd build/libs
java -jar payment-0.0.1-SNAPSHOT.jar
```

### 테스트
[Swagger 접속](http://localhost:8080/swagger-ui/index.html)

POST /common/payment/pay
```
{
  "cardNumber": "1234567890123456",
  "expirationDate": "1125",
  "cvc": "777",
  "installmentMonths": "12",
  "paymentPrice": 110000,
  "vat": "10000"
}
```

POST /common/payment/retrieve
```
{
    "id": "OWRjZTZlZjctZTdkZC00"
}
```

DELETE /common/payment/cancel
```
{
  "id": "OWRjZTZlZjctZTdkZC00",
  "cancelPrice": 110000,
  "vat": "10000"
}
```

DELETE /common/payment/cancel/partial
```
{
  "id": "OWRjZTZlZjctZTdkZC00",
  "cancelPrice": 11000,
  "vat": "1000"
}
```

## 에러 상태 코드
- AlreadyUsedException (226)
    - 같은 카드번호로 다른 결제가 이미 진행중일 때
    - 같은 관리번호로 전체 취소/부분 취소가 이미 진행중일 때
- ResourceNotFoundException (404)
    - 이미 취소했는데 취소를 또 시도할 때
    - 없는 데이터를 조회하려 할 때
- InvalidParameterException (405)
    - 필수값이 입력되지 않았을 때
- NotSupportException (406)
    - 금액이나 부가가치세가 올바르지 않을 때
    - 남아있는 결제 금액이 없는데 부분 취소를 요청할 때
    - 요청한 부분 취소 결제 금액이 남아있는 결제 금액보다 클 때
    - 요청한 취소 부가가치세가 남아있는 부가가치세보다 클 때
    - 남은 요금은 다 취소됐지만 부가가치세가 남아있을 때