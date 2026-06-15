package com.ht.shoppingcart.component;

import com.ht.shoppingcart.dto.CartItemDto;
import com.ht.shoppingcart.dto.CartResponseDto;
import com.ht.shoppingcart.dto.PriceDto;
import com.ht.shoppingcart.model.Cart;
import com.ht.shoppingcart.model.CartItem;
import com.ht.common.model.Price;
import com.ht.common.model.RecurringPrice;
import com.ht.common.model.OneTimePrice;
import com.ht.common.enums.PriceType;
import com.ht.common.enums.RecurrenceUnit;
import com.ht.common.enums.ActionType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartMapper")
class CartMapperTest {

    private final CartMapper cartMapper = new CartMapper();

    @Test
    @DisplayName("Should map one-time PriceDto to OneTimePrice")
    void toPrice_oneTimePriceDto_mapsToOneTimePrice() {
        // GIVEN
        PriceDto dto = new PriceDto(
                PriceType.ONE_TIME,
                new BigDecimal("19.99"),
                null,
                null
        );

        // WHEN
        Price result = cartMapper.toPrice(dto);

        // THEN
        assertThat(result).isInstanceOf(OneTimePrice.class);
        assertThat(result.getValue()).isEqualByComparingTo("19.99");
        assertThat(result.getType()).isEqualTo(PriceType.ONE_TIME);
    }

    @Test
    @DisplayName("Should map recurring PriceDto to RecurringPrice")
    void toPrice_recurringPriceDto_mapsToRecurringPrice() {
        // GIVEN
        PriceDto dto = new PriceDto(
                PriceType.RECURRING,
                new BigDecimal("9.99"),
                12,
                RecurrenceUnit.MONTH
        );

        // WHEN
        Price result = cartMapper.toPrice(dto);

        // THEN
        assertThat(result).isInstanceOf(RecurringPrice.class);
        RecurringPrice recurring = (RecurringPrice) result;
        assertThat(recurring.getValue()).isEqualByComparingTo("9.99");
        assertThat(recurring.getNumberOfRecurrences()).isEqualTo(12);
        assertThat(recurring.getRecurrenceUnit()).isEqualTo(RecurrenceUnit.MONTH);
        assertThat(recurring.getType()).isEqualTo(PriceType.RECURRING);
    }

    @Test
    @DisplayName("Should map OneTimePrice to PriceDto without recurrence fields")
    void toPriceDto_oneTimePrice_mapsToFlatDtoWithoutRecurrenceFields() {
        // GIVEN
        Price price = new OneTimePrice(new BigDecimal("499.99"));

        // WHEN
        PriceDto dto = cartMapper.toPriceDto(price);

        // THEN
        assertThat(dto.getType()).isEqualTo(PriceType.ONE_TIME);
        assertThat(dto.getValue()).isEqualByComparingTo("499.99");
        assertThat(dto.getNumberOfRecurrences()).isNull();
        assertThat(dto.getRecurrenceUnit()).isNull();
    }

    @Test
    @DisplayName("Should map RecurringPrice to PriceDto with recurrence fields")
    void toPriceDto_recurringPrice_mapsToFlatDtoWithRecurrenceFields() {
        // GIVEN
        Price price = new RecurringPrice(
                new BigDecimal("4.50"),
                24,
                RecurrenceUnit.MONTH
        );

        // WHEN
        PriceDto dto = cartMapper.toPriceDto(price);

        // THEN
        assertThat(dto.getType()).isEqualTo(PriceType.RECURRING);
        assertThat(dto.getValue()).isEqualByComparingTo("4.50");
        assertThat(dto.getNumberOfRecurrences()).isEqualTo(24);
        assertThat(dto.getRecurrenceUnit()).isEqualTo(RecurrenceUnit.MONTH);
    }

    @Test
    @DisplayName("Should set action from method parameter when mapping CartItem")
    void toCartItem_setsActionFromParameter_notFromDto() {
        // GIVEN
        CartItemDto dto = new CartItemDto(
                "offer-1",
                List.of(new PriceDto(
                        PriceType.ONE_TIME,
                        new BigDecimal("9.99"),
                        null,
                        null))
        );

        // WHEN
        CartItem item = cartMapper.toCartItem(dto, ActionType.ADD);

        // THEN
        assertThat(item.getOfferId()).isEqualTo("offer-1");
        assertThat(item.getAction()).isEqualTo(ActionType.ADD);
        assertThat(item.getPrices()).hasSize(1);
    }

    @Test
    @DisplayName("Should map all prices when CartItemDto contains multiple prices")
    void toCartItem_withMultiplePrices_mapsAllPrices() {
        // GIVEN
        CartItemDto dto = new CartItemDto(
                "offer-router",
                List.of(
                        new PriceDto(
                                PriceType.ONE_TIME,
                                new BigDecimal("25.00"),
                                null,
                                null
                        ),
                        new PriceDto(
                                PriceType.RECURRING,
                                new BigDecimal("4.50"),
                                24,
                                RecurrenceUnit.MONTH
                        )
                )
        );

        // WHEN
        CartItem item = cartMapper.toCartItem(dto, ActionType.MODIFY);

        // THEN
        assertThat(item.getAction()).isEqualTo(ActionType.MODIFY);
        assertThat(item.getPrices()).hasSize(2);
        assertThat(item.getPrices().get(0)).isInstanceOf(OneTimePrice.class);
        assertThat(item.getPrices().get(1)).isInstanceOf(RecurringPrice.class);
    }

    @Test
    @DisplayName("Should map CartItem to CartItemDto and omit action")
    void toItemDto_mapsOfferIdAndPrices_omitsAction() {
        // GIVEN
        CartItem item = new CartItem(
                "offer-1",
                ActionType.ADD,
                List.of(new OneTimePrice(new BigDecimal("19.99")))
        );

        // WHEN
        CartItemDto dto = cartMapper.toItemDto(item);

        // THEN
        assertThat(dto.getOfferId()).isEqualTo("offer-1");
        assertThat(dto.getPrices()).hasSize(1);
        assertThat(dto.getPrices().get(0).getType()).isEqualTo(PriceType.ONE_TIME);
    }

    @Test
    @DisplayName("Should map all cart fields to CartResponseDto")
    void toResponseDto_mapsAllCartFields() {
        // GIVEN
        Cart cart = new Cart("customer-1");
        cart.getItems().add(
                new CartItem(
                        "offer-1",
                        ActionType.ADD,
                        List.of(new OneTimePrice(new BigDecimal("19.99")))
                )
        );
        cart.getItems().add(
                new CartItem(
                        "offer-2",
                        ActionType.ADD,
                        List.of(new RecurringPrice(
                                new BigDecimal("9.99"),
                                12,
                                RecurrenceUnit.MONTH))
                )
        );
        cart.setUpdatedAt(Instant.parse("2026-06-14T10:00:00Z"));

        // WHEN
        CartResponseDto dto = cartMapper.toResponseDto(cart);

        // THEN
        assertThat(dto.getCustomerId()).isEqualTo("customer-1");
        assertThat(dto.getItems()).hasSize(2);
        assertThat(dto.getItems().get(0).getOfferId()).isEqualTo("offer-1");
        assertThat(dto.getItems().get(1).getOfferId()).isEqualTo("offer-2");
        assertThat(dto.getUpdatedAt()).isEqualTo(
                Instant.parse("2026-06-14T10:00:00Z")
        );
    }

    @Test
    @DisplayName("Should map empty cart to response with empty item list")
    void toResponseDto_emptyCart_mapsToEmptyItemsList() {
        // GIVEN
        Cart cart = new Cart("customer-1");

        // WHEN
        CartResponseDto dto = cartMapper.toResponseDto(cart);

        // THEN
        assertThat(dto.getCustomerId()).isEqualTo("customer-1");
        assertThat(dto.getItems()).isEmpty();
    }
}
