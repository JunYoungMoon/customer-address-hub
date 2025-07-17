package com.gastro.customeraddresshub.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private int galbiQuantity = 0;

    @Column(nullable = false)
    private int spamQuantity = 0;

    @Column(nullable = false)
    private int totalAmount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isEmpty() {
        return galbiQuantity == 0 && spamQuantity == 0;
    }

    public int getTotalItemCount() {
        return galbiQuantity + spamQuantity;
    }

    public void clear() {
        this.galbiQuantity = 0;
        this.spamQuantity = 0;
        this.totalAmount = 0;
    }

    // 생성자
    public Cart(Long customerId) {
        this.customerId = customerId;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}