package com.gastro.customeraddresshub.repository;

import com.gastro.customeraddresshub.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByNameAndPhone(String name, String phone);
}