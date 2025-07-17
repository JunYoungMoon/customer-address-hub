package com.gastro.customeraddresshub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CustomerForm {
    @NotBlank(message = "이름을 입력해주세요")
    private String name;

    @NotBlank(message = "휴대폰 번호를 입력해주세요")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String phone;
}