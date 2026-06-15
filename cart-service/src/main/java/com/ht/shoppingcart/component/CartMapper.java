package com.ht.shoppingcart.component;

import com.ht.common.enums.ActionType;
import com.ht.common.enums.PriceType;
import com.ht.common.model.OneTimePrice;
import com.ht.common.model.Price;
import com.ht.common.model.RecurringPrice;
import com.ht.shoppingcart.dto.CartItemDto;
import com.ht.shoppingcart.dto.CartResponseDto;
import com.ht.shoppingcart.dto.PriceDto;
import com.ht.shoppingcart.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartMapper {

    public CartResponseDto toResponseDto(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(this::toItemDto)
                .toList();
        return new CartResponseDto(cart.getCustomerId(), itemDtos,
                cart.getCreatedAt(), cart.getUpdatedAt());
    }

    public CartItemDto toItemDto(CartItem item) {
        List<PriceDto> priceDtos = item.getPrices().stream()
                .map(this::toPriceDto)
                .toList();
        return new CartItemDto(item.getOfferId(), priceDtos);
    }

    public CartItem toCartItem(CartItemDto dto, ActionType action) {
        List<Price> prices = dto.getPrices().stream()
                .map(this::toPrice)
                .toList();
        return new CartItem(dto.getOfferId(), action, prices);
    }

    public PriceDto toPriceDto(Price price) {
        if (price instanceof RecurringPrice recurring) {
            return new PriceDto(PriceType.RECURRING, recurring.getValue(),
                    recurring.getNumberOfRecurrences(), recurring.getRecurrenceUnit());
        }
        return new PriceDto(PriceType.ONE_TIME, price.getValue(), null, null);
    }

    public Price toPrice(PriceDto dto) {
        if (dto.getType() == PriceType.RECURRING) {
            return new RecurringPrice(dto.getValue(),
                    dto.getNumberOfRecurrences(), dto.getRecurrenceUnit());
        }
        return new OneTimePrice(dto.getValue());
    }
}