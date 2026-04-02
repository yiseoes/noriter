package com.noriter.infrastructure.encryption;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * API 키 암호화/복호화 서비스
 * 참조: NFR-05 API 키 암호화 저장, NT-SYS-001
 *
 * AES-128 대칭키 암호화 사용 (키는 환경변수 또는 application.yml에서 관리)
 */
@Log4j2
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    // TODO: 운영 환경에서는 환경변수로 주입 (현재는 고정 키)
    private static final String SECRET_KEY = "NoriterSecKey16!";  // 16바이트 = AES-128

    public String encrypt(String plainText) {
        log.debug("[암호화] 평문 암호화 시작 - 길이={}자", plainText.length());
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String result = Base64.getEncoder().encodeToString(encrypted);
            log.debug("[암호화] 암호화 완료 - 결과 길이={}자", result.length());
            return result;
        } catch (Exception e) {
            log.error("[암호화] 암호화 실패 - error={}", e.getMessage());
            throw new RuntimeException("암호화 실패", e);
        }
    }

    public String decrypt(String encryptedText) {
        log.debug("[복호화] 복호화 시작 - 길이={}자", encryptedText.length());
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            String result = new String(decrypted, StandardCharsets.UTF_8);
            log.debug("[복호화] 복호화 완료 - 결과 길이={}자", result.length());
            return result;
        } catch (Exception e) {
            log.error("[복호화] 복호화 실패 - error={}", e.getMessage());
            throw new RuntimeException("복호화 실패", e);
        }
    }
}
