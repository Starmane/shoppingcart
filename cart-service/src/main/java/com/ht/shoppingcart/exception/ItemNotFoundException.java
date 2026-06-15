package com.ht.shoppingcart.exception;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(String customerId, String offerId) {
        super(String.format("Item with offerId '%s' not found in cart for customer '%s'", offerId, customerId));
    }

}