package com.viv.order.service.impl;

import org.springframework.stereotype.Service;

import com.viv.order.entity.Order;
import com.viv.order.repository.OrderRepository;
import com.viv.order.service.OrderService;
import com.viv.user.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;


    @Transactional(transactionManager = "orderTransactionManager")
    public Order createOrder(Long userId, Double amount) {

        userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUserId(userId);
        order.setAmount(amount);

        return orderRepo.save(order);
    }
}