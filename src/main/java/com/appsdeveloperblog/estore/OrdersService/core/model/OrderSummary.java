package com.appsdeveloperblog.estore.OrdersService.core.model;

import lombok.Value;

@Value
public class OrderSummary {

    String orderId;
    OrderStatus orderStatus;
    String reason;
}
