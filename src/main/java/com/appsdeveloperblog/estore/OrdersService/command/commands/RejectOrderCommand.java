package com.appsdeveloperblog.estore.OrdersService.command.commands;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class RejectOrderCommand {

    @TargetAggregateIdentifier
    String orderId;
    String reason;
}
