package com.gastro.customeraddresshub.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    private String zipcode;
    private String address;
    private String detailAddress;
    private String deliveryMemo;

    private int galbiQuantity = 0;
    private int spamQuantity = 0;

    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private String status = "주문완료";
}
