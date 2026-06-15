package com.ht.shoppingcart.service;

import com.ht.common.enums.ActionType;
import com.ht.shoppingcart.exception.DuplicateItemException;
import com.ht.shoppingcart.messaging.CartEventProducer;
import com.ht.shoppingcart.component.CartMapper;
import com.ht.shoppingcart.dto.CartItemDto;
import com.ht.shoppingcart.exception.CartNotFoundException;
import com.ht.shoppingcart.exception.ItemNotFoundException;
import com.ht.shoppingcart.model.Cart;
import com.ht.shoppingcart.model.CartItem;
import com.ht.shoppingcart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final CartEventProducer cartEventProducer;

    public Cart getCart(String customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));
    }

    public Cart addItem(String customerId, CartItemDto dto) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> new Cart(customerId));

        boolean alreadyExists = cart.getItems().stream()
                .anyMatch(existingItem -> existingItem.getOfferId().equals(dto.getOfferId()));

        if (alreadyExists) {
            throw new DuplicateItemException(customerId, dto.getOfferId());
        }

        CartItem item = cartMapper.toCartItem(dto, ActionType.ADD);
        cart.getItems().add(item);
        cart.setUpdatedAt(Instant.now());

        Cart saved = cartRepository.save(cart);

        cartEventProducer.publish(customerId, item.getOfferId(), item.getAction(),
                item.getPrices(), saved.getUpdatedAt());

        return saved;
    }

    public Cart modifyItem(String customerId, String offerId, CartItemDto dto) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));

        CartItem updatedItem = cartMapper.toCartItem(dto, ActionType.MODIFY);

        boolean found = false;
        for (int i = 0; i < cart.getItems().size(); i++) {
            if (cart.getItems().get(i).getOfferId().equals(offerId)) {
                cart.getItems().set(i, updatedItem);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new ItemNotFoundException(customerId, offerId);
        }

        cart.setUpdatedAt(Instant.now());
        Cart saved = cartRepository.save(cart);

        cartEventProducer.publish(customerId, updatedItem.getOfferId(), updatedItem.getAction(),
                updatedItem.getPrices(), saved.getUpdatedAt());

        return saved;
    }

    public Cart removeItem(String customerId, String offerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(customerId));

        Iterator<CartItem> iterator = cart.getItems().iterator();
        CartItem removed = null;
        while (iterator.hasNext()) {
            CartItem candidate = iterator.next();
            if (candidate.getOfferId().equals(offerId)) {
                removed = candidate;
                iterator.remove();
                break;
            }
        }

        if (removed == null) {
            throw new ItemNotFoundException(customerId, offerId);
        }

        cart.setUpdatedAt(Instant.now());
        Cart saved = cartRepository.save(cart);

        cartEventProducer.publish(customerId, offerId, ActionType.DELETE,
                removed.getPrices(), saved.getUpdatedAt());

        return saved;
    }

    public void evictCart(String customerId) {
        if (!cartRepository.existsByCustomerId(customerId)) {
            throw new CartNotFoundException(customerId);
        }
        cartRepository.deleteByCustomerId(customerId);
    }
}