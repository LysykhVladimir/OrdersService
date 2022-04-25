package com.appsdeveloperblog.estore.OrdersService.query;

import com.appsdeveloperblog.estore.OrdersService.core.data.OrderEntity;
import com.appsdeveloperblog.estore.OrdersService.core.data.OrdersRepository;
import com.appsdeveloperblog.estore.OrdersService.core.events.OrderApprovedEvent;
import com.appsdeveloperblog.estore.OrdersService.core.events.OrderCreatedEvent;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsHandler {

    private final OrdersRepository ordersRepository;

    public OrderEventsHandler(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;
    }

    @EventHandler
    public void on(OrderCreatedEvent event) {

        OrderEntity orderEntity = new OrderEntity();
        BeanUtils.copyProperties(event, orderEntity);
        ordersRepository.save(orderEntity);
    }

    @EventHandler
    public void on(OrderApprovedEvent event) {
        OrderEntity orderEntity = ordersRepository.findByOrderId(event.getOrderId());

        if (orderEntity == null) {
            //TODO
            return;
        }

        orderEntity.setOrderStatus(event.getOrderStatus());
        ordersRepository.save(orderEntity);
    }
}