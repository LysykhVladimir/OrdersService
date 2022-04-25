package com.appsdeveloperblog.estore.OrdersService.core.events;

import com.appsdeveloperblog.estore.OrdersService.command.OrderStatus;
import lombok.Value;

@Value
public class OrderApprovedEvent {
    String orderId;
    OrderStatus orderStatus = OrderStatus.APPROVED;
}
