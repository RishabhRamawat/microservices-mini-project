package com.rishabh.microservices.order.service;

import com.rishabh.microservices.order.dto.OrderLineItemsDto;
import com.rishabh.microservices.order.dto.OrderRequest;
import com.rishabh.microservices.order.dto.OrderResponse;
import com.rishabh.microservices.order.entity.Order;
import com.rishabh.microservices.order.entity.OrderLineItems;
import com.rishabh.microservices.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderResponse placeOrder(OrderRequest orderRequest) {
        // The service owns order number generation — clients must not supply it
        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .build();

        // Map each incoming DTO to an entity, then link both sides of the bidirectional relationship
        for (OrderLineItemsDto dto : orderRequest.getOrderLineItemsDtoList()) {
            OrderLineItems lineItem = new OrderLineItems();
            lineItem.setSkuCode(dto.getSkuCode());
            lineItem.setPrice(dto.getPrice());
            lineItem.setQuantity(dto.getQuantity());

            // Required so Hibernate writes the correct order_id FK value on the owning side
            lineItem.setOrder(order);

            // Keeps the in-memory collection consistent with the DB state after cascaded save
            order.getOrderLineItems().add(lineItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Convert saved line-item entities back into DTOs for the response
        List<OrderLineItemsDto> responseDtoList = new ArrayList<>();
        for (OrderLineItems lineItem : savedOrder.getOrderLineItems()) {
            OrderLineItemsDto dto = OrderLineItemsDto.builder()
                    .skuCode(lineItem.getSkuCode())
                    .price(lineItem.getPrice())
                    .quantity(lineItem.getQuantity())
                    .build();
            responseDtoList.add(dto);
        }

        return OrderResponse.builder()
                .id(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .orderLineItemsDtoList(responseDtoList)
                .build();
    }
}
