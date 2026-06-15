package com.ht.shoppingcart.exception;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(String customerId) {
        super(String.format("Cart not found for customer '%s'", customerId));
    }
}