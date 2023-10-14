package com.jiwon.payment.common;

import com.jiwon.payment.configuration.EncryptionConfig;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

public class EncryptionService extends EncryptionConfig {
    public static String encryptData(String text, String password) {
        StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
        jasypt.setPassword(password);

        return jasypt.encrypt(text);
    }

    public static String decryptData(String encryptedText, String password) {
        StandardPBEStringEncryptor jasypt = new StandardPBEStringEncryptor();
        jasypt.setPassword(password);

        return jasypt.decrypt(encryptedText);
    }
}
