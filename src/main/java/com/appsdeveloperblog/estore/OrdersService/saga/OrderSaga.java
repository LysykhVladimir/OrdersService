package com.appsdeveloperblog.estore.OrdersService.saga;

import com.appsdeveloperblog.estore.OrdersService.command.commands.ApproveOrderCommand;
import com.appsdeveloperblog.estore.OrdersService.command.commands.RejectOrderCommand;
import com.appsdeveloperblog.estore.OrdersService.core.events.OrderApprovedEvent;
import com.appsdeveloperblog.estore.OrdersService.core.events.OrderCreatedEvent;
import com.appsdeveloperblog.estore.OrdersService.core.events.OrderRejectedEvent;
import com.appsdeveloperblog.estore.OrdersService.core.model.OrderSummary;
import com.appsdeveloperblog.estore.OrdersService.query.FindOrderQuery;
import com.appsdeveloperblog.estore.core.commands.CancelProductReservationCommand;
import com.appsdeveloperblog.estore.core.commands.ProcessPaymentCommand;
import com.appsdeveloperblog.estore.core.commands.ReserveProductCommand;
import com.appsdeveloperblog.estore.core.events.PaymentProcessedEvent;
import com.appsdeveloperblog.estore.core.events.ProductReservationCancelledEvent;
import com.appsdeveloperblog.estore.core.events.ProductReservedEvent;
import com.appsdeveloperblog.estore.core.model.User;
import com.appsdeveloperblog.estore.core.query.FetchUserPaymentDetailsQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Saga
@Slf4j
public class OrderSaga {

    public static final String PAYMENT_PROCESSING_DEADLINE = "payment-processing-deadline";
    private String scheduleId;

    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;
    @Autowired
    private transient QueryUpdateEmitter queryUpdateEmitter;
    @Autowired
    private transient DeadlineManager deadlineManager;

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCreatedEvent orderCreatedEvent) {

        ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
                .orderId(orderCreatedEvent.getOrderId())
                .productId(orderCreatedEvent.getProductId())
                .quantity(orderCreatedEvent.getQuantity())
                .userId(orderCreatedEvent.getUserId())
                .build();

        log.info("OrderCreatedEvent handled for orderId:{} and productId:{}",
                reserveProductCommand.getOrderId(), reserveProductCommand.getProductId());

        commandGateway.send(reserveProductCommand, (commandMessage, commandResultMessage) -> {
            if (commandResultMessage.isExceptional()) {
                //Start a compensating transaction
                RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(orderCreatedEvent.getOrderId(),
                        commandResultMessage.exceptionResult().getMessage());
                commandGateway.send(rejectOrderCommand);
            }
        });
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservedEvent productReservedEvent) {
        //Process user payment
        log.info("ProductReservedEvent is called for productId:{} and orderId:{}",
                productReservedEvent.getProductId(), productReservedEvent.getOrderId());

        FetchUserPaymentDetailsQuery fetchUserPaymentDetailsQuery =
                new FetchUserPaymentDetailsQuery(productReservedEvent.getUserId());

        User userPaymentDetails = null;
        try {
            userPaymentDetails = queryGateway.query(fetchUserPaymentDetailsQuery, ResponseTypes.instanceOf(User.class)).join();
        } catch (Exception ex) {
            log.error(ex.getMessage());
            //Start compensating transaction
            cancelProductReservation(productReservedEvent, ex.getMessage());
            return;
        }

        if (userPaymentDetails == null) {
            //Start compensating transaction
            cancelProductReservation(productReservedEvent, "Could not fetch user payment details");
            return;
        }
        log.info("Successfully fetched user payment details for user {}", userPaymentDetails.getFirstName());

        deadlineManager.schedule(Duration.of(10, ChronoUnit.SECONDS),
                PAYMENT_PROCESSING_DEADLINE, productReservedEvent);

        ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
                .orderId(productReservedEvent.getOrderId())
                .paymentDetails(userPaymentDetails.getPaymentDetails())
                .paymentId(UUID.randomUUID().toString())
                .build();

        String result = null;
        try {
            result = commandGateway.sendAndWait(processPaymentCommand, 10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            cancelProductReservation(productReservedEvent, ex.getMessage());
            return;
        }

        if (result == null) {
            log.info("The ProcessPaymentCommand resulted is NULL. Initiating a compensation transaction");
            cancelProductReservation(productReservedEvent, "Could not user payment with provided payment details");
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(PaymentProcessedEvent paymentProcessedEvent) {
        cancelDeadline();

        //Send an ApprovedOrderCommand
        ApproveOrderCommand approveOrderCommand = new ApproveOrderCommand(paymentProcessedEvent.getOrderId());
        commandGateway.send(approveOrderCommand);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderApprovedEvent orderApprovedEvent) {
        log.info("Order is approved. Order saga is complete fo orderId= {}", orderApprovedEvent.getOrderId());
        //SagaLifecycle.end();
        queryUpdateEmitter.emit(FindOrderQuery.class, query -> true,
                new OrderSummary(orderApprovedEvent.getOrderId(), orderApprovedEvent.getOrderStatus(), ""));
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservationCancelledEvent productReservationCancelledEvent) {
        //Create and send reject order command
        RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(productReservationCancelledEvent.getOrderId(),
                productReservationCancelledEvent.getReason());
        commandGateway.send(rejectOrderCommand);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderRejectedEvent orderRejectedEvent) {
        log.info("Successfully rejected order with id = {}", orderRejectedEvent.getOrderId());
        queryUpdateEmitter.emit(FindOrderQuery.class, query -> true,
                new OrderSummary(orderRejectedEvent.getOrderId(),
                        orderRejectedEvent.getOrderStatus(),
                        orderRejectedEvent.getReason()));
    }

    @DeadlineHandler(deadlineName = PAYMENT_PROCESSING_DEADLINE)
    public void handlePaymentDeadline(ProductReservedEvent productReservedEvent) {
        log.info("Payment processing deadline took place. Sending a compensating command to cancel the product reservation");
        cancelProductReservation(productReservedEvent, "Payment timeout");

    }


    private void cancelProductReservation(ProductReservedEvent productReservedEvent, String reason) {
        cancelDeadline();

        CancelProductReservationCommand publishProductReservationCommand = CancelProductReservationCommand.builder()
                .orderId(productReservedEvent.getOrderId())
                .productId(productReservedEvent.getProductId())
                .quantity(productReservedEvent.getQuantity())
                .userId(productReservedEvent.getUserId())
                .reason(reason)
                .build();

        commandGateway.send(publishProductReservationCommand);
    }

    private void cancelDeadline() {
        if (scheduleId != null) {
            deadlineManager.cancelSchedule(PAYMENT_PROCESSING_DEADLINE, scheduleId);
            scheduleId = null;
        }

    }
}
