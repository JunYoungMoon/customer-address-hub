package com.gastro.customeraddresshub.service;

import com.gastro.customeraddresshub.entity.Customer;
import com.gastro.customeraddresshub.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer findByNameAndPhone(String name, String phone) {
        return customerRepository.findByNameAndPhone(name, phone);
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }
}
