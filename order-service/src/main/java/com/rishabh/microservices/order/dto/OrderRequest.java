package com.rishabh.microservices.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequest {
    // Holds the list of line items submitted when placing a new order
    private List<OrderLineItemsDto> orderLineItemsDtoList;
}
