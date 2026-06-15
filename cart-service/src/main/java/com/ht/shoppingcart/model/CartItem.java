package com.ht.shoppingcart.model;

import com.ht.common.enums.ActionType;
import com.ht.common.model.Price;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private String offerId;
    private ActionType action;
    private List<Price> prices;
}