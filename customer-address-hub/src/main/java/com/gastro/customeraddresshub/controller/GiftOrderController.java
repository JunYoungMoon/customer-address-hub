package com.gastro.customeraddresshub.controller;

import com.gastro.customeraddresshub.dto.CustomerForm;
import com.gastro.customeraddresshub.dto.OrderForm;
import com.gastro.customeraddresshub.entity.Cart;
import com.gastro.customeraddresshub.entity.Customer;
import com.gastro.customeraddresshub.entity.Order;
import com.gastro.customeraddresshub.service.CartService;
import com.gastro.customeraddresshub.service.CustomerService;
import com.gastro.customeraddresshub.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class GiftOrderController {

    private final CustomerService customerService;
    private final OrderService orderService;
    private final CartService cartService;

    // 공통 메서드: 세션에서 장바구니 가져오기
    private Cart getCartFromDatabase(Long customerId) {
        return cartService.getOrCreateCart(customerId);
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("customerForm", new CustomerForm());
        return "login";
    }

    @PostMapping("/customer/verify")
    public String verifyCustomer(@Valid @ModelAttribute CustomerForm customerForm,
                                 BindingResult bindingResult,
                                 HttpSession session) {

        if (bindingResult.hasErrors()) {
            return "login";
        }

        // 고객 정보 확인 (DB에서 조회)
        Customer customer = customerService.findByNameAndPhone(
                customerForm.getName(), customerForm.getPhone());

        if (customer == null) {
            bindingResult.rejectValue("phone", "customer.notFound",
                    "등록되지 않은 고객 정보입니다.");
            return "login";
        }

        // 세션에 고객 정보 저장
        session.setAttribute("customer", customer);

        return "redirect:/products";
    }

    @GetMapping("/products")
    public String productList(Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("customer");
        if (customer == null) {
            return "redirect:/login";
        }

        Cart cart = getCartFromDatabase(customer.getId());

        model.addAttribute("customer", customer);
        model.addAttribute("cart", cart);
        return "products";
    }

    @GetMapping("/cart")
    public String cartView(Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("customer");
        if (customer == null) {
            return "redirect:/login";
        }

        Cart cart = getCartFromDatabase(customer.getId());

        model.addAttribute("customer", customer);
        model.addAttribute("cart", cart);
        return "cart";
    }

    @GetMapping("/address")
    public String addressForm(Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("customer");
        Cart cart = (Cart) session.getAttribute("cart");

        if (customer == null) {
            return "redirect:/login";
        }

        if (cart == null || cart.isEmpty()) {
            return "redirect:/products";
        }

        model.addAttribute("customer", customer);
        model.addAttribute("cart", cart);
        model.addAttribute("orderForm", new OrderForm());

        return "address";
    }

    @PostMapping("/order/complete")
    public String completeOrder(@Valid @ModelAttribute OrderForm orderForm,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model) {

        Customer customer = (Customer) session.getAttribute("customer");

        if (customer == null) {
            return "redirect:/login";
        }

        Cart cart = getCartFromDatabase(customer.getId());

        if (cart.isEmpty()) {
            return "redirect:/products";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("customer", customer);
            model.addAttribute("cart", cart);
            return "address";
        }

        // 주문 생성
        Order order = new Order();
        order.setOrderNumber("ORD-" + LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + String.format("%03d", (int)(Math.random() * 1000)));
        order.setCustomerId(customer.getId());
        order.setOrderDate(LocalDateTime.now());
        order.setRecipientName(orderForm.getRecipientName());
        order.setRecipientPhone(orderForm.getRecipientPhone());
        order.setZipcode(orderForm.getZipcode());
        order.setAddress(orderForm.getAddress());
        order.setDetailAddress(orderForm.getDetailAddress());
        order.setDeliveryMemo(orderForm.getDeliveryMemo());
        order.setGalbiQuantity(cart.getGalbiQuantity());
        order.setSpamQuantity(cart.getSpamQuantity());
        order.setStatus("주문완료");

        // 주문 저장
        orderService.save(order);

        // 주문 완료 후 장바구니 삭제
        cartService.deleteCart(customer.getId());

        model.addAttribute("customer", customer);
        model.addAttribute("order", order);

        return "complete";
    }
}