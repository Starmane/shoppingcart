package com.ht.shoppingcart.repository;

import com.ht.shoppingcart.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByCustomerId(String customerId);

    void deleteByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);
}