package com.inha.pro.safetynevi.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 회원가입 요청 DTO
 * - 정규식(@Pattern)을 통한 입력값 유효성 검증 포함
 */
@Getter
@Setter
@ToString
public class MemberSignupDto {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{4,12}$", message = "아이디는 영문, 숫자 4~12자여야 합니다.")
    private String userId;

    @NotBlank
    // 주요 포털 도메인(네이버, 카카오, 다음, 구글)만 허용
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@(naver\\.com|kakao\\.com|daum\\.net|gmail\\.com)$",
            message = "네이버, 카카오, 다음, 구글 이메일만 사용 가능합니다.")
    private String email;

    @NotBlank
    // 비밀번호 복잡도: 8자 이상, 대문자/숫자/특수문자 포함
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 8자 이상이며, 대문자/숫자/특수문자를 포함해야 합니다.")
    private String password;

    @NotBlank
    @Pattern(regexp = "^[가-힣a-zA-Z]{2,20}$", message = "이름은 한글 또는 영문 2~20자여야 합니다.")
    private String name;

    @NotBlank
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,10}$", message = "닉네임은 특수문자 제외 2~10자여야 합니다.")
    private String nickname;

    private String address;
    private String detailAddress;
    private String areaName;
    private Double latitude;
    private Double longitude;

    // 빈 값이거나 010 형식의 11자리 숫자만 허용
    @Pattern(regexp = "^$|^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    private String emergencyPhone;

    private Integer pwQuestion;
    private String pwAnswer;
}