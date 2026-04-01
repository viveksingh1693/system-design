package com.viv.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viv.order.entity.Order;
import com.viv.order.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Operations backed by the PostgreSQL order datasource")
public class OrderController {

    private final OrderService service;


    @PostMapping
    @Operation(
        summary = "Create an order",
        description = "Validates the user from the MySQL datasource and saves the order in the PostgreSQL datasource."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = Order.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request or user not found")
    })
    public Order create(
        @Parameter(description = "Existing user identifier", example = "1", required = true)
        @RequestParam Long userId,
        @Parameter(description = "Order amount", example = "249.99", required = true)
        @RequestParam Double amount
    ) {
        return service.createOrder(userId, amount);
    }

}
