package com.ht.shoppingcart.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    @Indexed(unique = true)
    private String customerId;

    private List<CartItem> items = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    public Cart(String customerId) {
        this.customerId = customerId;
        this.id = customerId;
        this.items = new ArrayList<>();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}