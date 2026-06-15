package com.ht.shoppingcart.controller;

import com.ht.shoppingcart.dto.CartItemDto;
import com.ht.shoppingcart.dto.CartResponseDto;
import com.ht.shoppingcart.model.Cart;
import com.ht.shoppingcart.component.CartMapper;
import com.ht.shoppingcart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;
    private final CartMapper cartMapper;

    public CartController(CartService cartService, CartMapper cartMapper) {
        this.cartService = cartService;
        this.cartMapper = cartMapper;
    }

    @Operation(summary = "Get the current content of a customer's cart")
    @GetMapping("/{customerId}")
    public ResponseEntity<CartResponseDto> getCart(@PathVariable String customerId) {
        Cart cart = cartService.getCart(customerId);
        return ResponseEntity.ok(cartMapper.toResponseDto(cart));
    }

    @Operation(summary = "Add an item to a customer's cart — action is always ADD")
    @PostMapping("/{customerId}/items")
    public ResponseEntity<CartResponseDto> addItem(@PathVariable String customerId,
                                                   @Valid @RequestBody CartItemDto itemDto) {
        Cart cart = cartService.addItem(customerId, itemDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartMapper.toResponseDto(cart));
    }

    @Operation(summary = "Modify an existing item in a customer's cart — action is always MODIFY")
    @PutMapping("/{customerId}/items/{offerId}")
    public ResponseEntity<CartResponseDto> modifyItem(@PathVariable String customerId,
                                                      @PathVariable String offerId,
                                                      @Valid @RequestBody CartItemDto itemDto) {
        Cart cart = cartService.modifyItem(customerId, offerId, itemDto);
        return ResponseEntity.ok(cartMapper.toResponseDto(cart));
    }

    @Operation(summary = "Remove an item from a customer's cart — action is always DELETE")
    @DeleteMapping("/{customerId}/items/{offerId}")
    public ResponseEntity<CartResponseDto> removeItem(@PathVariable String customerId,
                                                      @PathVariable String offerId) {
        Cart cart = cartService.removeItem(customerId, offerId);
        return ResponseEntity.ok(cartMapper.toResponseDto(cart));
    }

    @Operation(summary = "Evict (delete) a customer's entire cart")
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> evictCart(@PathVariable String customerId) {
        cartService.evictCart(customerId);
        return ResponseEntity.noContent().build();
    }
}