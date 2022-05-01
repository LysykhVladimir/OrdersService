package com.appsdeveloperblog.estore.OrdersService.command.rest;

import com.appsdeveloperblog.estore.OrdersService.core.model.OrderStatus;
import lombok.Data;

@Data
public class CreateOrderRestModel {

    private String userId;
    private String productId;
    private Integer quantity;
    private String addressId;
    private OrderStatus orderStatus;
}
