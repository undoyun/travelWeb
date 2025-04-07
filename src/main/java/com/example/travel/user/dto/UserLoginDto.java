package com.example.travel.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDto {

    @NotBlank(message = "아이디는 필수 정보입니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수 정보입니다.")
    private String password;
}
