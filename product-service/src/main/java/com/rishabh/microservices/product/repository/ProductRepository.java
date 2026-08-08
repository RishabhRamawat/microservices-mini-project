package com.rishabh.microservices.product.repository;

import com.rishabh.microservices.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
