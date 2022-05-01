package com.appsdeveloperblog.estore.OrdersService.core.events;

import com.appsdeveloperblog.estore.OrdersService.core.model.OrderStatus;
import lombok.Value;

@Value
public class OrderApprovedEvent {
    String orderId;
    OrderStatus orderStatus = OrderStatus.APPROVED;
}
