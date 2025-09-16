package com.inha.pro.safetynevi.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void 다섯번_실패하면_계정이_잠긴다() {
        for (int i = 0; i < 5; i++) service.loginFailed("user1");
        assertThat(service.isBlocked("user1")).isTrue();
    }

    @Test
    void 네번_실패까지는_잠기지_않는다() {
        for (int i = 0; i < 4; i++) service.loginFailed("user2");
        assertThat(service.isBlocked("user2")).isFalse();
    }

    @Test
    void 로그인에_성공하면_실패_카운트가_초기화된다() {
        for (int i = 0; i < 5; i++) service.loginFailed("user3");
        service.loginSucceeded("user3");
        assertThat(service.isBlocked("user3")).isFalse();
    }

    @Test
    void 대소문자가_달라도_같은_계정으로_집계한다() {
        for (int i = 0; i < 3; i++) service.loginFailed("UserX");
        for (int i = 0; i < 2; i++) service.loginFailed("userx");
        assertThat(service.isBlocked("USERX")).isTrue();
    }
}
