package com.gastro.customeraddresshub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderForm {
    @NotBlank(message = "받는 분 성함을 입력해주세요")
    private String recipientName;

    @NotBlank(message = "연락처를 입력해주세요")
    private String recipientPhone;

    @NotBlank(message = "우편번호를 입력해주세요")
    private String zipcode;

    @NotBlank(message = "주소를 입력해주세요")
    private String address;

    @NotBlank(message = "상세주소를 입력해주세요")
    private String detailAddress;

    private String deliveryMemo;
}