package com.gastro.customeraddresshub.service;

import com.gastro.customeraddresshub.entity.Cart;
import com.gastro.customeraddresshub.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;

    /**
     * 고객의 장바구니 조회 (없으면 새로 생성)
     */
    public Cart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart(customerId);
                    return cartRepository.save(newCart);
                });
    }

    /**
     * 장바구니 저장/업데이트
     */
    public Cart saveCart(Cart cart) {
        return cartRepository.save(cart);
    }

    /**
     * 장바구니 비우기
     */
    public void clearCart(Long customerId) {
        cartRepository.findByCustomerId(customerId)
                .ifPresent(cart -> {
                    cart.clear();
                    cartRepository.save(cart);
                });
    }

    /**
     * 장바구니 삭제 (주문 완료 후)
     */
    public void deleteCart(Long customerId) {
        cartRepository.findByCustomerId(customerId)
                .ifPresent(cartRepository::delete);
    }
}