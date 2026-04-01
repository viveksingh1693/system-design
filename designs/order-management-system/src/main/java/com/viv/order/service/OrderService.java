package com.viv.order.service;

import com.viv.order.entity.Order;

public interface OrderService {

    Order createOrder(Long userId, Double amount);

}
