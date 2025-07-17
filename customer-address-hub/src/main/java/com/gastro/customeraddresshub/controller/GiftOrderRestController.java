package com.gastro.customeraddresshub.controller;

import com.gastro.customeraddresshub.dto.CartUpdateRequest;
import com.gastro.customeraddresshub.entity.Cart;
import com.gastro.customeraddresshub.entity.Customer;
import com.gastro.customeraddresshub.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GiftOrderRestController {

    private final CartService cartService;
    private static final String SESSION_CART_KEY = "cart";

    // 공통 메서드: 고객 인증 확인
    private Customer getAuthenticatedCustomer(HttpSession session) {
        return (Customer) session.getAttribute("customer");
    }

    // 하이브리드 방식: 세션에서 먼저 조회, 없으면 DB에서 조회 후 세션에 저장
    private Cart getCartFromSession(HttpSession session, Long customerId) {
        Cart sessionCart = (Cart) session.getAttribute(SESSION_CART_KEY);

        // 세션에 장바구니가 있고 고객 ID가 일치하면 세션 데이터 사용
        if (sessionCart != null && sessionCart.getCustomerId().equals(customerId)) {
            log.debug("세션에서 장바구니 조회 - 고객 ID: {}", customerId);
            return sessionCart;
        }

        // 세션에 없거나 다른 고객 데이터면 DB에서 조회 후 세션에 저장
        Cart dbCart = cartService.getOrCreateCart(customerId);
        session.setAttribute(SESSION_CART_KEY, dbCart);
        log.debug("DB에서 장바구니 조회 후 세션 저장 - 고객 ID: {}", customerId);
        return dbCart;
    }

    // 하이브리드 방식: 세션과 DB 모두 업데이트
    private Cart saveCartToSessionAndDB(Cart cart, HttpSession session) {
        try {
            // DB에 저장
            Cart savedCart = cartService.saveCart(cart);
            // 세션에도 업데이트
            session.setAttribute(SESSION_CART_KEY, savedCart);
            log.debug("장바구니 세션 및 DB 저장 완료 - 고객 ID: {}", savedCart.getCustomerId());
            return savedCart;
        } catch (Exception e) {
            log.error("장바구니 저장 실패", e);
            throw e;
        }
    }

    // 세션 장바구니 무효화
    private void invalidateSessionCart(HttpSession session) {
        session.removeAttribute(SESSION_CART_KEY);
        log.debug("세션 장바구니 무효화");
    }

    // 공통 메서드: 성공 응답 생성
    private ResponseEntity<Map<String, Object>> createSuccessResponse(Cart cart, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cart", cart);
        response.put("totalItems", cart.getTotalItemCount());
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    // 공통 메서드: 에러 응답 생성
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 장바구니 상태 조회
     * GET /api/cart/status
     */
    @GetMapping("/cart/status")
    public ResponseEntity<Cart> getCartStatus(HttpSession session) {
        Customer customer = getAuthenticatedCustomer(session);
        if (customer == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        try {
            Cart cart = getCartFromSession(session, customer.getId());
            log.info("장바구니 상태 조회 - 고객 ID: {}, 갈비: {}, 스팸: {}",
                    customer.getId(), cart.getGalbiQuantity(), cart.getSpamQuantity());
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            log.error("장바구니 상태 조회 실패 - 고객 ID: " + customer.getId(), e);
            invalidateSessionCart(session); // 오류 시 세션 캐시 무효화
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 장바구니 수량 수정 (추가/차감) - POST 방식
     * POST /api/cart/modify
     */
    @PostMapping("/cart/modify")
    public ResponseEntity<Map<String, Object>> modifyCart(
            @RequestParam int galbiQuantity,
            @RequestParam int spamQuantity,
            @RequestParam String action,
            HttpSession session) {

        Customer customer = getAuthenticatedCustomer(session);
        if (customer == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        // 입력값 유효성 검증
        if (galbiQuantity < 0 || spamQuantity < 0) {
            return createErrorResponse("수량은 0 이상이어야 합니다.");
        }

        if (!"add".equals(action) && !"subtract".equals(action)) {
            return createErrorResponse("잘못된 액션입니다.");
        }

        try {
            // 세션에서 장바구니 조회 (하이브리드 방식)
            Cart cart = getCartFromSession(session, customer.getId());

            log.info("장바구니 수정 전 - 고객 ID: {}, 갈비: {}, 스팸: {}",
                    customer.getId(), cart.getGalbiQuantity(), cart.getSpamQuantity());

            if ("add".equals(action)) {
                int newGalbiQty = cart.getGalbiQuantity() + galbiQuantity;
                int newSpamQty = cart.getSpamQuantity() + spamQuantity;

                // 최대 수량 체크
                if (newGalbiQty > 20 || newSpamQty > 20) {
                    return createErrorResponse("최대 주문 수량은 20개입니다.");
                }

                cart.setGalbiQuantity(newGalbiQty);
                cart.setSpamQuantity(newSpamQty);

            } else { // subtract
                cart.setGalbiQuantity(Math.max(0, cart.getGalbiQuantity() - galbiQuantity));
                cart.setSpamQuantity(Math.max(0, cart.getSpamQuantity() - spamQuantity));
            }

            // 세션과 DB 모두 업데이트 (하이브리드 방식)
            cart = saveCartToSessionAndDB(cart, session);

            log.info("장바구니 수정 후 - 고객 ID: {}, 갈비: {}, 스팸: {}",
                    customer.getId(), cart.getGalbiQuantity(), cart.getSpamQuantity());

            String actionText = "add".equals(action) ? "추가" : "제거";
            return createSuccessResponse(cart, "장바구니가 성공적으로 " + actionText + "되었습니다.");

        } catch (Exception e) {
            log.error("장바구니 수정 실패 - 고객 ID: " + customer.getId(), e);
            invalidateSessionCart(session); // 오류 시 세션 캐시 무효화
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 장바구니 수량 수정 (추가/차감) - POST 방식 (기존 엔드포인트 유지)
     * POST /api/cart/update
     */
    @PostMapping("/cart/update")
    public ResponseEntity<Map<String, Object>> updateCart(
            @RequestParam int galbiQuantity,
            @RequestParam int spamQuantity,
            @RequestParam String action,
            HttpSession session) {

        // 동일한 로직으로 처리
        return modifyCart(galbiQuantity, spamQuantity, action, session);
    }

    /**
     * 장바구니 수량 직접 설정 (절대값으로 설정)
     * PUT /api/cart/update
     */
    @PutMapping("/cart/update")
    public ResponseEntity<Map<String, Object>> updateCartDirect(
            @RequestBody CartUpdateRequest request,
            HttpSession session) {

        Customer customer = getAuthenticatedCustomer(session);
        if (customer == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        // 수량 유효성 검증
        if (request.getGalbi() < 0 || request.getSpam() < 0) {
            return createErrorResponse("수량은 0 이상이어야 합니다.");
        }

        if (request.getGalbi() > 20 || request.getSpam() > 20) {
            return createErrorResponse("최대 주문 수량은 20개입니다.");
        }

        try {
            // 세션에서 장바구니 조회 (하이브리드 방식)
            Cart cart = getCartFromSession(session, customer.getId());

            log.info("장바구니 직접 설정 전 - 고객 ID: {}, 갈비: {}, 스팸: {}",
                    customer.getId(), cart.getGalbiQuantity(), cart.getSpamQuantity());

            cart.setGalbiQuantity(request.getGalbi());
            cart.setSpamQuantity(request.getSpam());

            // 세션과 DB 모두 업데이트 (하이브리드 방식)
            cart = saveCartToSessionAndDB(cart, session);

            log.info("장바구니 직접 설정 후 - 고객 ID: {}, 갈비: {}, 스팸: {}",
                    customer.getId(), cart.getGalbiQuantity(), cart.getSpamQuantity());

            return createSuccessResponse(cart, "장바구니가 성공적으로 업데이트되었습니다.");

        } catch (Exception e) {
            log.error("장바구니 직접 설정 실패 - 고객 ID: " + customer.getId(), e);
            invalidateSessionCart(session); // 오류 시 세션 캐시 무효화
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 장바구니 비우기
     * DELETE /api/cart/clear
     */
    @DeleteMapping("/cart/clear")
    public ResponseEntity<Map<String, Object>> clearCart(HttpSession session) {
        Customer customer = getAuthenticatedCustomer(session);
        if (customer == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            // DB에서 장바구니 비우기
            cartService.clearCart(customer.getId());
            Cart cart = cartService.getOrCreateCart(customer.getId());

            // 세션에도 업데이트
            session.setAttribute(SESSION_CART_KEY, cart);

            log.info("장바구니 비우기 완료 - 고객 ID: {}", customer.getId());
            return createSuccessResponse(cart, "장바구니가 비워졌습니다.");

        } catch (Exception e) {
            log.error("장바구니 비우기 실패 - 고객 ID: " + customer.getId(), e);
            invalidateSessionCart(session); // 오류 시 세션 캐시 무효화
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 상품 제거
     * DELETE /api/cart/remove/{product}
     */
    @DeleteMapping("/cart/remove/{product}")
    public ResponseEntity<Map<String, Object>> removeProduct(
            @PathVariable String product,
            HttpSession session) {

        Customer customer = getAuthenticatedCustomer(session);
        if (customer == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        if (!"galbi".equals(product) && !"spam".equals(product)) {
            return createErrorResponse("잘못된 상품입니다.");
        }

        try {
            // 세션에서 장바구니 조회 (하이브리드 방식)
            Cart cart = getCartFromSession(session, customer.getId());

            if ("galbi".equals(product)) {
                cart.setGalbiQuantity(0);
            } else {
                cart.setSpamQuantity(0);
            }

            // 세션과 DB 모두 업데이트 (하이브리드 방식)
            cart = saveCartToSessionAndDB(cart, session);

            String productName = "galbi".equals(product) ? "갈비 선물세트" : "스팸 선물세트";
            log.info("상품 제거 완료 - 고객 ID: {}, 상품: {}", customer.getId(), productName);

            return createSuccessResponse(cart, productName + "가 장바구니에서 제거되었습니다.");

        } catch (Exception e) {
            log.error("상품 제거 실패 - 고객 ID: " + customer.getId() + ", 상품: " + product, e);
            invalidateSessionCart(session); // 오류 시 세션 캐시 무효화
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 세션 장바구니 강제 동기화 (필요시 사용)
     * POST /api/cart/sync
     */
    @PostMapping("/cart/sync")
    public ResponseEntity<Map<String, Object>> syncCart(HttpSession session) {
        Customer customer = getAuthenticatedCustomer(session);
        if (customer == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            // 세션 캐시 무효화 후 DB에서 다시 로드
            invalidateSessionCart(session);
            Cart cart = getCartFromSession(session, customer.getId());

            log.info("장바구니 동기화 완료 - 고객 ID: {}", customer.getId());
            return createSuccessResponse(cart, "장바구니가 동기화되었습니다.");

        } catch (Exception e) {
            log.error("장바구니 동기화 실패 - 고객 ID: " + customer.getId(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }
}