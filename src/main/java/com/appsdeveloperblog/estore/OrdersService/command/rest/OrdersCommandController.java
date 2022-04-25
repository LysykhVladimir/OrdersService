package com.appsdeveloperblog.estore.OrdersService.command.rest;

import com.appsdeveloperblog.estore.OrdersService.command.commands.CreateOrderCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrdersCommandController {

    public static final String UserId = "27b95829-4f3f-4ddf-8983-151ba010e35b";
    private final CommandGateway commandGateway;

    public OrdersCommandController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    public String createOrder (@Valid @RequestBody CreateOrderRestModel createOrderRestModel) {

        CreateOrderCommand createOrderCommand = CreateOrderCommand.builder()
                .userId(UserId)
                .productId(createOrderRestModel.getProductId())
                .quantity(createOrderRestModel.getQuantity())
                .addressId(createOrderRestModel.getAddressId())
                .orderStatus(createOrderRestModel.getOrderStatus())
                .orderId(UUID.randomUUID().toString())
                .build();

        String returnValue;
        returnValue = commandGateway.sendAndWait(createOrderCommand);
        return returnValue;
    }
}
