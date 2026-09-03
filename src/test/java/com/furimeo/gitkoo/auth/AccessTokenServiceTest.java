package com.furimeo.gitkoo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccessTokenServiceTest {

    @Test
    void generateRawTokenHasPrefixAndIsHex() {
        String token = AccessTokenService.generateRawToken();
        assertThat(token).startsWith(AccessTokenService.TOKEN_PREFIX);
        String hexPart = token.substring(AccessTokenService.TOKEN_PREFIX.length());
        assertThat(hexPart).hasSize(64); // 32 bytes hex = 64 chars
        assertThat(hexPart).matches("[0-9a-f]+");
    }

    @Test
    void generateRawTokenProducesDifferentValues() {
        String a = AccessTokenService.generateRawToken();
        String b = AccessTokenService.generateRawToken();
        assertThat(a).isNotEqualTo(b);
    }
}
