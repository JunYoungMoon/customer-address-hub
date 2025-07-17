package com.gastro.customeraddresshub.repository;

import com.gastro.customeraddresshub.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Order findByOrderNumber(String orderNumber);
}
