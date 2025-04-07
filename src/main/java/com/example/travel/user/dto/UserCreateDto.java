package com.example.travel.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserCreateDto {

    @NotBlank(message = "아이디는 필수 정보입니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[A-Za-z\\d]{4,20}$", message = "아이디는 영문, 숫자 조합 (4-20자)이어야 합니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수 정보입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 8~16자의 영문, 숫자를 사용해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 정보입니다.")
    private String passwordConfirm;

    @NotBlank(message = "이름은 필수 정보입니다.")
    private String name;

    @NotBlank(message = "닉네임은 필수 정보입니다.")
    private String nickname;

    @NotBlank(message = "성별은 필수 정보입니다.")
    private String gender;

    @NotNull(message = "생년월일은 필수 정보입니다.")
    private LocalDate birthdate;

    @NotBlank(message = "이메일은 필수 정보입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "reCAPTCHA 검증값이 필요합니다.")
    private String recaptchaResponse;
}
