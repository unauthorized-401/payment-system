package com.jiwon.payment.common;

import com.jiwon.payment.configuration.EncryptionConfig;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Value;

public class EncryptionService extends EncryptionConfig {
    // TODO: 암호화 키 환경변수로 지정

    public static String encryptData(String text) {
        StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
        jasypt.setPassword("Jiwon");

        return jasypt.encrypt(text);
    }

    public static String decryptData(String encryptedText) {
        StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
        jasypt.setPassword("Jiwon");

        return jasypt.decrypt(encryptedText);
    }
}
