package com.noriter.infrastructure.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionServiceTest {

    private final EncryptionService encryptionService = new EncryptionService();

    @Test
    @DisplayName("평문을 암호화하고 복호화하면 원본과 동일하다")
    void encryptAndDecrypt_returnsOriginal() {
        String original = "sk-ant-api03-test12345";

        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo(original);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("같은 평문을 암호화하면 같은 결과가 나온다")
    void encrypt_sameInput_sameOutput() {
        String text = "test-api-key";

        String enc1 = encryptionService.encrypt(text);
        String enc2 = encryptionService.encrypt(text);

        assertThat(enc1).isEqualTo(enc2);
    }

    @Test
    @DisplayName("한글 문자열도 암호화/복호화된다")
    void encryptAndDecrypt_koreanText() {
        String korean = "안녕하세요 API 키입니다";

        String encrypted = encryptionService.encrypt(korean);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(korean);
    }
}
