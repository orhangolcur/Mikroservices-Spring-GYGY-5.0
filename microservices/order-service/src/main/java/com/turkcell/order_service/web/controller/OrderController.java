package com.turkcell.order_service.web.controller;

import com.turkcell.order_service.dto.CreateOrderRequest;
import com.turkcell.order_service.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello Order-Service";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
