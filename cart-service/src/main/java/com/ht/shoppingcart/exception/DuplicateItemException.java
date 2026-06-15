package com.ht.shoppingcart.exception;

public class DuplicateItemException extends RuntimeException {

    public DuplicateItemException(String customerId, String offerId) {
        super("Item with offerId '" + offerId + "' already exists in cart for customer: " + customerId
                + ". Use modify instead.");
    }
}