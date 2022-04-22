package com.appsdeveloperblog.estore.OrdersService.core.events;

import com.appsdeveloperblog.estore.OrdersService.command.OrderStatus;
import lombok.Data;

@Data
public class OrderCreatedEvent {

    private String orderId;
    private String userId;
    private String productId;
    private Integer quantity;
    private String addressId;
    private OrderStatus orderStatus;
}
