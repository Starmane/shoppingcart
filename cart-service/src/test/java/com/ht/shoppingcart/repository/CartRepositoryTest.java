package com.ht.shoppingcart.repository;

import com.ht.common.enums.ActionType;
import com.ht.shoppingcart.model.Cart;
import com.ht.shoppingcart.model.CartItem;
import com.ht.common.model.OneTimePrice;
import com.ht.common.model.Price;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
@DisplayName("CartRepository")
class CartRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @Autowired
    private CartRepository cartRepository;

    private static final String CUSTOMER_ID = "customer-1";

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        cartRepository.deleteAll();
    }

    private Cart sampleCart(String customerId) {
        Cart cart = new Cart(customerId);
        cart.getItems().add(new CartItem("offer-1", ActionType.ADD,
                List.<Price>of(new OneTimePrice(new BigDecimal("19.99")))));
        return cart;
    }

    // ---------- findByCustomerId ----------

    @Test
    @DisplayName("findByCustomerId - returns the cart when one exists for the customer")
    void findByCustomerId_existingCustomer_returnsCart() {
        // given
        cartRepository.save(sampleCart(CUSTOMER_ID));

        // when
        Optional<Cart> result = cartRepository.findByCustomerId(CUSTOMER_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.get().getItems()).hasSize(1);
        assertThat(result.get().getItems().get(0).getOfferId()).isEqualTo("offer-1");
    }

    @Test
    @DisplayName("findByCustomerId - returns empty when no cart exists for the customer")
    void findByCustomerId_noCustomerExists_returnsEmpty() {
        // given
        // no carts saved

        // when
        Optional<Cart> result = cartRepository.findByCustomerId("nonexistent-customer");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByCustomerId - does not return a cart belonging to a different customer")
    void findByCustomerId_differentCustomerExists_returnsEmpty() {
        // given
        cartRepository.save(sampleCart("customer-other"));

        // when
        Optional<Cart> result = cartRepository.findByCustomerId(CUSTOMER_ID);

        // then
        assertThat(result).isEmpty();
    }

    // ---------- existsByCustomerId ----------

    @Test
    @DisplayName("existsByCustomerId - returns true when a cart exists for the customer")
    void existsByCustomerId_existingCustomer_returnsTrue() {
        // given
        cartRepository.save(sampleCart(CUSTOMER_ID));

        // when
        boolean result = cartRepository.existsByCustomerId(CUSTOMER_ID);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByCustomerId - returns false when no cart exists for the customer")
    void existsByCustomerId_noCustomerExists_returnsFalse() {
        // given
        // no carts saved

        // when
        boolean result = cartRepository.existsByCustomerId("nonexistent-customer");

        // then
        assertThat(result).isFalse();
    }

    // ---------- deleteByCustomerId ----------

    @Test
    @DisplayName("deleteByCustomerId - removes the cart for the given customer")
    void deleteByCustomerId_existingCustomer_removesCart() {
        // given
        cartRepository.save(sampleCart(CUSTOMER_ID));
        assertThat(cartRepository.existsByCustomerId(CUSTOMER_ID)).isTrue();

        // when
        cartRepository.deleteByCustomerId(CUSTOMER_ID);

        // then
        assertThat(cartRepository.existsByCustomerId(CUSTOMER_ID)).isFalse();
        assertThat(cartRepository.findByCustomerId(CUSTOMER_ID)).isEmpty();
    }

    @Test
    @DisplayName("deleteByCustomerId - does not affect other customers' carts")
    void deleteByCustomerId_existingCustomer_doesNotAffectOtherCarts() {
        // given
        cartRepository.save(sampleCart(CUSTOMER_ID));
        cartRepository.save(sampleCart("customer-other"));

        // when
        cartRepository.deleteByCustomerId(CUSTOMER_ID);

        // then
        assertThat(cartRepository.existsByCustomerId(CUSTOMER_ID)).isFalse();
        assertThat(cartRepository.existsByCustomerId("customer-other")).isTrue();
    }

    @Test
    @DisplayName("deleteByCustomerId - does nothing when no cart exists for the customer")
    void deleteByCustomerId_noCustomerExists_doesNothing() {
        // given
        // no carts saved

        // when / then - should not throw
        cartRepository.deleteByCustomerId("nonexistent-customer");

        assertThat(cartRepository.count()).isZero();
    }

    // ---------- persistence round-trip / polymorphic Price ----------

    @Test
    @DisplayName("save and retrieve - persists and reconstructs nested CartItem and Price correctly")
    void saveAndRetrieve_preservesNestedItemAndPriceData() {
        // given
        Cart cart = sampleCart(CUSTOMER_ID);

        // when
        cartRepository.save(cart);
        Optional<Cart> result = cartRepository.findByCustomerId(CUSTOMER_ID);

        // then
        assertThat(result).isPresent();
        CartItem item = result.get().getItems().get(0);
        assertThat(item.getOfferId()).isEqualTo("offer-1");
        assertThat(item.getAction()).isEqualTo(ActionType.ADD);
        assertThat(item.getPrices()).hasSize(1);
        assertThat(item.getPrices().get(0)).isInstanceOf(OneTimePrice.class);
        assertThat(item.getPrices().get(0).getValue()).isEqualByComparingTo("19.99");
    }
}