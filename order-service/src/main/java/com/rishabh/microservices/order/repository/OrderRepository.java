package com.rishabh.microservices.order.repository;

import com.rishabh.microservices.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository interface for managing Order entity persistence operations
public interface OrderRepository extends JpaRepository<Order, Long> {
}
