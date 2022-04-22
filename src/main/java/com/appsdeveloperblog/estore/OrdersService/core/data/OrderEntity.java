package com.appsdeveloperblog.estore.OrdersService.core.data;

import com.appsdeveloperblog.estore.OrdersService.command.OrderStatus;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "orders")
@Data
public class OrderEntity implements Serializable {

    private static final long serialVersionUID = 592735209767125489L;

    @Id
    @Column(unique = true)
    public String orderId;
    private String productId;
    private String userId;
    private Integer quantity;
    private String addressId;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
}