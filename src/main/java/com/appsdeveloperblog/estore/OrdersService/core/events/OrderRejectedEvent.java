package com.appsdeveloperblog.estore.OrdersService.core.events;

import com.appsdeveloperblog.estore.OrdersService.core.model.OrderStatus;
import lombok.Value;

@Value
public class OrderRejectedEvent {
    String orderId;
    String reason;
    OrderStatus orderStatus = OrderStatus.REJECTED;
}
