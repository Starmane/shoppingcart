package com.ht.shoppingcart.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.shoppingcart.dto.CartItemDto;
import com.ht.shoppingcart.dto.CartResponseDto;
import com.ht.shoppingcart.dto.PriceDto;
import com.ht.common.enums.ActionType;
import com.ht.common.enums.PriceType;
import com.ht.common.enums.RecurrenceUnit;
import com.ht.shoppingcart.exception.CartNotFoundException;
import com.ht.shoppingcart.exception.DuplicateItemException;
import com.ht.shoppingcart.exception.ItemNotFoundException;
import com.ht.shoppingcart.model.Cart;
import com.ht.shoppingcart.model.CartItem;
import com.ht.common.model.OneTimePrice;
import com.ht.common.model.Price;
import com.ht.shoppingcart.component.CartMapper;
import com.ht.shoppingcart.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@DisplayName("CartController")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private CartMapper cartMapper;

    private static final String CUSTOMER_ID = "customer-1";
    private static final String OFFER_ID = "offer-1";

    private CartItemDto validItemDto() {
        return new CartItemDto(OFFER_ID,
                List.of(new PriceDto(PriceType.ONE_TIME, new BigDecimal("19.99"), null, null)));
    }

    private Cart sampleCart() {
        Cart cart = new Cart(CUSTOMER_ID);
        cart.getItems().add(new CartItem(OFFER_ID, ActionType.ADD,
                List.<Price>of(new OneTimePrice(new BigDecimal("19.99")))));
        return cart;
    }

    private CartResponseDto sampleResponseDto() {
        return new CartResponseDto(CUSTOMER_ID,
                List.of(new CartItemDto(OFFER_ID,
                        List.of(new PriceDto(PriceType.ONE_TIME, new BigDecimal("19.99"), null, null)))),
                Instant.now(), Instant.now());
    }

    private CartResponseDto emptyResponseDto() {
        return new CartResponseDto(CUSTOMER_ID, List.of(), Instant.now(), Instant.now());
    }

    // ---------- GET /carts/{customerId} ----------

    @Test
    @DisplayName("GET /carts/{customerId} - returns 200 with cart content when cart exists")
    void getCart_existingCart_returnsOkWithCartContent() throws Exception {
        // given
        Cart cart = sampleCart();
        CartResponseDto responseDto = sampleResponseDto();

        when(cartService.getCart(CUSTOMER_ID)).thenReturn(cart);
        when(cartMapper.toResponseDto(cart)).thenReturn(responseDto);

        // when
        // then
        mockMvc.perform(get("/carts/{customerId}", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.items[0].offerId").value(OFFER_ID));
    }

    @Test
    @DisplayName("GET /carts/{customerId} - returns 404 when no cart exists for customer")
    void getCart_noCartExists_returnsNotFound() throws Exception {
        // given
        when(cartService.getCart(CUSTOMER_ID)).thenThrow(new CartNotFoundException(CUSTOMER_ID));

        // when
        // then
        mockMvc.perform(get("/carts/{customerId}", CUSTOMER_ID))
                .andExpect(status().isNotFound());
    }

    // ---------- POST /carts/{customerId}/items ----------

    @Test
    @DisplayName("POST /carts/{customerId}/items - returns 201 and the updated cart when request is valid")
    void addItem_validRequest_returnsCreated() throws Exception {
        // given
        CartItemDto requestDto = validItemDto();
        Cart cart = sampleCart();
        CartResponseDto responseDto = sampleResponseDto();

        when(cartService.addItem(eq(CUSTOMER_ID), any(CartItemDto.class))).thenReturn(cart);
        when(cartMapper.toResponseDto(cart)).thenReturn(responseDto);

        // when
        // then
        mockMvc.perform(post("/carts/{customerId}/items", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.items[0].offerId").value(OFFER_ID));

        verify(cartService).addItem(eq(CUSTOMER_ID), any(CartItemDto.class));
    }

    @Test
    @DisplayName("POST /carts/{customerId}/items - returns 409 when the offer already exists in the cart")
    void addItem_duplicateOffer_returnsConflict() throws Exception {
        // given
        when(cartService.addItem(eq(CUSTOMER_ID), any(CartItemDto.class)))
                .thenThrow(new DuplicateItemException(CUSTOMER_ID, OFFER_ID));

        // when
        // then
        mockMvc.perform(post("/carts/{customerId}/items", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validItemDto())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /carts/{customerId}/items - returns 400 when offerId is missing")
    void addItem_missingOfferId_returnsBadRequest() throws Exception {
        // given
        CartItemDto invalidDto = new CartItemDto(null,
                List.of(new PriceDto(PriceType.ONE_TIME, new BigDecimal("19.99"), null, null)));

        // when
        // then
        mockMvc.perform(post("/carts/{customerId}/items", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /carts/{customerId}/items - returns 400 when prices list is empty")
    void addItem_emptyPricesList_returnsBadRequest() throws Exception {
        // given
        CartItemDto invalidDto = new CartItemDto(OFFER_ID, List.of());

        // when
        // then
        mockMvc.perform(post("/carts/{customerId}/items", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /carts/{customerId}/items - returns 400 when RECURRING price is missing recurrence fields")
    void addItem_recurringPriceMissingRecurrenceFields_returnsBadRequest() throws Exception {
        // given
        CartItemDto invalidDto = new CartItemDto(OFFER_ID,
                List.of(new PriceDto(PriceType.RECURRING, new BigDecimal("9.99"), null, null)));

        // when
        // then
        mockMvc.perform(post("/carts/{customerId}/items", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /carts/{customerId}/items - returns 400 when ONE_TIME price includes recurrence fields")
    void addItem_oneTimePriceWithRecurrenceFields_returnsBadRequest() throws Exception {
        // given
        CartItemDto invalidDto = new CartItemDto(OFFER_ID,
                List.of(new PriceDto(PriceType.ONE_TIME, new BigDecimal("9.99"), 12, RecurrenceUnit.MONTH)));

        // when
        // then
        mockMvc.perform(post("/carts/{customerId}/items", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /carts/{customerId}/items - returns 400 when request body is malformed JSON")
    void addItem_malformedJson_returnsBadRequest() throws Exception {
        // given
        String malformedJson = "{ \"offerId\": \"offer-1\", \"prices\": [ { \"type\": \"NOT_A_TYPE\", \"value\": 1 } ] }";

        // when
        // then
        mockMvc.perform(post("/carts/{customerId}/items", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /carts/{customerId}/items - returns 400 when price value is negative")
    void addItem_negativePriceValue_returnsBadRequest() throws Exception {
        // given
        CartItemDto invalidDto = new CartItemDto(OFFER_ID,
                List.of(new PriceDto(PriceType.ONE_TIME, new BigDecimal("-5.00"), null, null)));

        // when
        // then
        mockMvc.perform(post("/carts/{customerId}/items", CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // ---------- PUT /carts/{customerId}/items/{offerId} ----------

    @Test
    @DisplayName("PUT /carts/{customerId}/items/{offerId} - returns 200 and the updated cart when request is valid")
    void modifyItem_validRequest_returnsOk() throws Exception {
        // given
        CartItemDto requestDto = validItemDto();
        Cart cart = sampleCart();
        CartResponseDto responseDto = sampleResponseDto();

        when(cartService.modifyItem(eq(CUSTOMER_ID), eq(OFFER_ID), any(CartItemDto.class))).thenReturn(cart);
        when(cartMapper.toResponseDto(cart)).thenReturn(responseDto);

        // when
        // then
        mockMvc.perform(put("/carts/{customerId}/items/{offerId}", CUSTOMER_ID, OFFER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID));

        verify(cartService).modifyItem(eq(CUSTOMER_ID), eq(OFFER_ID), any(CartItemDto.class));
    }

    @Test
    @DisplayName("PUT /carts/{customerId}/items/{offerId} - returns 404 when the cart does not exist")
    void modifyItem_cartNotFound_returnsNotFound() throws Exception {
        // given
        when(cartService.modifyItem(eq(CUSTOMER_ID), eq(OFFER_ID), any(CartItemDto.class)))
                .thenThrow(new CartNotFoundException(CUSTOMER_ID));

        // when
        // then
        mockMvc.perform(put("/carts/{customerId}/items/{offerId}", CUSTOMER_ID, OFFER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validItemDto())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /carts/{customerId}/items/{offerId} - returns 404 when the offer is not in the cart")
    void modifyItem_offerNotInCart_returnsNotFound() throws Exception {
        // given
        when(cartService.modifyItem(eq(CUSTOMER_ID), eq(OFFER_ID), any(CartItemDto.class)))
                .thenThrow(new ItemNotFoundException(CUSTOMER_ID, OFFER_ID));

        // when
        // then
        mockMvc.perform(put("/carts/{customerId}/items/{offerId}", CUSTOMER_ID, OFFER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validItemDto())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /carts/{customerId}/items/{offerId} - returns 400 when request body fails validation")
    void modifyItem_invalidBody_returnsBadRequest() throws Exception {
        // given
        CartItemDto invalidDto = new CartItemDto(OFFER_ID, List.of());

        // when
        // then
        mockMvc.perform(put("/carts/{customerId}/items/{offerId}", CUSTOMER_ID, OFFER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    // ---------- DELETE /carts/{customerId}/items/{offerId} ----------

    @Test
    @DisplayName("DELETE /carts/{customerId}/items/{offerId} - returns 200 with updated cart when item exists")
    void removeItem_existingItem_returnsOkWithUpdatedCart() throws Exception {
        // given
        Cart cart = new Cart(CUSTOMER_ID); // empty after removal
        CartResponseDto responseDto = emptyResponseDto();

        when(cartService.removeItem(CUSTOMER_ID, OFFER_ID)).thenReturn(cart);
        when(cartMapper.toResponseDto(cart)).thenReturn(responseDto);

        // when
        // then
        mockMvc.perform(delete("/carts/{customerId}/items/{offerId}", CUSTOMER_ID, OFFER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("DELETE /carts/{customerId}/items/{offerId} - returns 404 when the cart does not exist")
    void removeItem_cartNotFound_returnsNotFound() throws Exception {
        // given
        when(cartService.removeItem(CUSTOMER_ID, OFFER_ID))
                .thenThrow(new CartNotFoundException(CUSTOMER_ID));

        // when
        // then
        mockMvc.perform(delete("/carts/{customerId}/items/{offerId}", CUSTOMER_ID, OFFER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /carts/{customerId}/items/{offerId} - returns 404 when the item is not in the cart")
    void removeItem_itemNotFound_returnsNotFound() throws Exception {
        // given
        when(cartService.removeItem(CUSTOMER_ID, OFFER_ID))
                .thenThrow(new ItemNotFoundException(CUSTOMER_ID, OFFER_ID));

        // when
        // then
        mockMvc.perform(delete("/carts/{customerId}/items/{offerId}", CUSTOMER_ID, OFFER_ID))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE /carts/{customerId} ----------

    @Test
    @DisplayName("DELETE /carts/{customerId} - returns 204 when the cart is evicted successfully")
    void evictCart_existingCart_returnsNoContent() throws Exception {
        // given
        // (no stubbing needed - evictCart succeeds by default on the mock)

        // when
        // then
        mockMvc.perform(delete("/carts/{customerId}", CUSTOMER_ID))
                .andExpect(status().isNoContent());

        verify(cartService).evictCart(CUSTOMER_ID);
    }

    @Test
    @DisplayName("DELETE /carts/{customerId} - returns 404 when no cart exists for the customer")
    void evictCart_cartNotFound_returnsNotFound() throws Exception {
        // given
        doThrow(new CartNotFoundException(CUSTOMER_ID))
                .when(cartService).evictCart(CUSTOMER_ID);

        // when
        // then
        mockMvc.perform(delete("/carts/{customerId}", CUSTOMER_ID))
                .andExpect(status().isNotFound());
    }
}