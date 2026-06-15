package com.ht.shoppingcart.service;

import com.ht.common.model.RecurringPrice;
import com.ht.shoppingcart.dto.CartItemDto;
import com.ht.shoppingcart.dto.PriceDto;
import com.ht.common.enums.ActionType;
import com.ht.common.enums.PriceType;
import com.ht.common.enums.RecurrenceUnit;
import com.ht.shoppingcart.exception.CartNotFoundException;
import com.ht.shoppingcart.exception.DuplicateItemException;
import com.ht.shoppingcart.messaging.CartEventProducer;
import com.ht.shoppingcart.model.Cart;
import com.ht.shoppingcart.model.CartItem;
import com.ht.common.model.OneTimePrice;
import com.ht.shoppingcart.repository.CartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.ht.shoppingcart.component.CartMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService")
class CartServiceTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String OFFER_ID = "offer-1";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private CartEventProducer cartEventProducer;

    @InjectMocks
    private CartService cartService;

    private CartItemDto sampleItemDto() {
        return new CartItemDto(
                OFFER_ID,
                List.of(
                        new PriceDto(
                                PriceType.ONE_TIME,
                                new BigDecimal("19.99"),
                                null,
                                null
                        )
                )
        );
    }

    private CartItem sampleCartItem() {
        return new CartItem(
                OFFER_ID,
                ActionType.ADD,
                List.of(
                        new OneTimePrice(
                                new BigDecimal("19.99")
                        )
                )
        );
    }

    @Test
    @DisplayName("Should return existing cart when customer exists")
    void getCart_existingCustomer_returnsExistingCart() {
        // GIVEN
        Cart existing = new Cart(CUSTOMER_ID);

        when(cartRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(existing));

        // WHEN
        Cart result = cartService.getCart(CUSTOMER_ID);

        // THEN
        assertThat(result).isSameAs(existing);

        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CartNotFoundException when customer cart does not exist")
    void getCart_noExistingCart_throwsCartNotFoundException() {
        // GIVEN
        when(cartRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> cartService.getCart(CUSTOMER_ID))
                .isInstanceOf(CartNotFoundException.class);

        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create cart and add item for new customer")
    void addItem_newCustomer_createsCartAndAddsItemWithActionAdd() {
        // GIVEN
        CartItemDto dto = sampleItemDto();
        CartItem mappedItem = sampleCartItem();

        when(cartRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.empty());

        when(cartMapper.toCartItem(dto, ActionType.ADD))
                .thenReturn(mappedItem);

        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Cart result = cartService.addItem(CUSTOMER_ID, dto);

        // THEN
        assertThat(result.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.getItems()).containsExactly(mappedItem);

        verify(cartMapper).toCartItem(dto, ActionType.ADD);

        verify(cartEventProducer).publish(
                eq(CUSTOMER_ID),
                eq(OFFER_ID),
                eq(ActionType.ADD),
                eq(mappedItem.getPrices()),
                any()
        );
    }

    @Test
    @DisplayName("addItem - throws DuplicateItemException when offerId already exists in the cart")
    void addItem_offerAlreadyInCart_throwsDuplicateItemException() {
        // given
        Cart existing = new Cart(CUSTOMER_ID);
        existing.getItems().add(sampleCartItem()); // offer-1 already present

        CartItemDto duplicateDto = new CartItemDto(OFFER_ID,
                List.of(new PriceDto(PriceType.ONE_TIME, new BigDecimal("9.99"), null, null)));

        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existing));

        // when / then
        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, duplicateDto))
                .isInstanceOf(DuplicateItemException.class);

        verify(cartRepository, never()).save(any());
        verify(cartEventProducer, never()).publish(any(), any(), any(), any(), any());
        verify(cartMapper, never()).toCartItem(any(), any());
    }

    @Test
    @DisplayName("addItem - allows a different offerId even if another offer already exists in the cart")
    void addItem_differentOfferId_addsSuccessfully() {
        // given
        Cart existing = new Cart(CUSTOMER_ID);
        existing.getItems().add(sampleCartItem()); // offer-1 already present

        CartItemDto newItemDto = new CartItemDto("offer-2",
                List.of(new PriceDto(PriceType.ONE_TIME, new BigDecimal("5.00"), null, null)));
        CartItem newItem = new CartItem("offer-2", ActionType.ADD,
                List.of(new OneTimePrice(new BigDecimal("5.00"))));

        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(existing));
        when(cartMapper.toCartItem(newItemDto, ActionType.ADD)).thenReturn(newItem);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Cart result = cartService.addItem(CUSTOMER_ID, newItemDto);

        // then
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Should replace existing item when modifying cart item")
    void modifyItem_existingItem_replacesItemWithActionModify() {
        // GIVEN
        Cart existing = new Cart(CUSTOMER_ID);
        existing.getItems().add(sampleCartItem());

        CartItemDto updateDto = new CartItemDto(
                OFFER_ID,
                List.of(
                        new PriceDto(
                                PriceType.RECURRING,
                                new BigDecimal("14.99"),
                                12,
                                RecurrenceUnit.MONTH
                        )
                )
        );

        CartItem updatedItem = new CartItem(
                OFFER_ID,
                ActionType.MODIFY,
                List.of(
                        new RecurringPrice(
                                new BigDecimal("14.99"),
                                12,
                                RecurrenceUnit.MONTH
                        )
                )
        );

        when(cartRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(existing));

        when(cartMapper.toCartItem(updateDto, ActionType.MODIFY))
                .thenReturn(updatedItem);

        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Cart result = cartService.modifyItem(
                CUSTOMER_ID,
                OFFER_ID,
                updateDto
        );

        // THEN
        assertThat(result.getItems())
                .hasSize(1)
                .first()
                .isSameAs(updatedItem);

        verify(cartEventProducer).publish(
                eq(CUSTOMER_ID),
                eq(OFFER_ID),
                eq(ActionType.MODIFY),
                anyList(),
                any()
        );
    }

    @Test
    @DisplayName("Should remove item and publish delete event")
    void removeItem_existingItem_removesItAndPublishesDeleteAction() {
        // GIVEN
        Cart existing = new Cart(CUSTOMER_ID);
        CartItem item = sampleCartItem();

        existing.getItems().add(item);

        when(cartRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(existing));

        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Cart result = cartService.removeItem(
                CUSTOMER_ID,
                OFFER_ID
        );

        // THEN
        assertThat(result.getItems()).isEmpty();

        verify(cartEventProducer).publish(
                eq(CUSTOMER_ID),
                eq(OFFER_ID),
                eq(ActionType.DELETE),
                eq(item.getPrices()),
                any()
        );
    }

    @Test
    @DisplayName("Should delete cart when cart exists")
    void evictCart_existingCart_deletesIt() {
        // GIVEN
        when(cartRepository.existsByCustomerId(CUSTOMER_ID))
                .thenReturn(true);

        // WHEN
        cartService.evictCart(CUSTOMER_ID);

        // THEN
        verify(cartRepository).deleteByCustomerId(CUSTOMER_ID);
    }

    @Test
    @DisplayName("Should throw CartNotFoundException when deleting non-existing cart")
    void evictCart_cartDoesNotExist_throwsCartNotFoundExceptionWithoutDeleting() {
        // GIVEN
        when(cartRepository.existsByCustomerId(CUSTOMER_ID))
                .thenReturn(false);

        // WHEN / THEN
        assertThatThrownBy(() -> cartService.evictCart(CUSTOMER_ID))
                .isInstanceOf(CartNotFoundException.class);

        verify(cartRepository, never()).deleteByCustomerId(any());
    }
}