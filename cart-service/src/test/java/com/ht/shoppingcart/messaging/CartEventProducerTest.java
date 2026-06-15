package com.ht.shoppingcart.messaging;

import com.ht.common.enums.ActionType;
import com.ht.common.enums.PriceType;
import com.ht.common.enums.RecurrenceUnit;
import com.ht.common.model.OneTimePrice;
import com.ht.common.model.Price;
import com.ht.common.model.RecurringPrice;
import com.ht.shoppingcart.model.CartEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartEventProducerTest {

    @Mock
    private KafkaTemplate<String, CartEventMessage> kafkaTemplate;

    private CartEventProducer cartEventProducer;

    private static final String TOPIC = "cart-events";

    @BeforeEach
    void setUp() {
        cartEventProducer = new CartEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(cartEventProducer, "topic", TOPIC);
    }

    @SuppressWarnings("unchecked")
    private void stubSuccessfulSend() {
        CompletableFuture<SendResult<String, CartEventMessage>> future = new CompletableFuture<>();
        future.complete(null);

        when(kafkaTemplate.send(any(String.class), any(String.class), any(CartEventMessage.class)))
                .thenReturn(future);
    }

    @Test
    @DisplayName("Should publish ONE_TIME price event correctly")
    void publish_oneTimePrice_sendsMessageKeyedByOfferIdWithCorrectShape() {
        // GIVEN
        stubSuccessfulSend();

        List<Price> prices = List.of(new OneTimePrice(new BigDecimal("19.99")));
        Instant timestamp = Instant.parse("2026-06-14T10:00:00Z");

        // WHEN
        cartEventProducer.publish("customer-1", "offer-1", ActionType.ADD, prices, timestamp);

        // THEN
        ArgumentCaptor<CartEventMessage> messageCaptor = ArgumentCaptor.forClass(CartEventMessage.class);

        verify(kafkaTemplate).send(eq(TOPIC), eq("offer-1"), messageCaptor.capture());

        CartEventMessage message = messageCaptor.getValue();

        assertThat(message.getCustomerId()).isEqualTo("customer-1");
        assertThat(message.getOfferId()).isEqualTo("offer-1");
        assertThat(message.getAction()).isEqualTo(ActionType.ADD);
        assertThat(message.getTimestamp()).isEqualTo(timestamp);

        assertThat(message.getPrices()).hasSize(1);

        CartEventMessage.PriceSnapshot snapshot = message.getPrices().get(0);
        assertThat(snapshot.getType()).isEqualTo(PriceType.ONE_TIME);
        assertThat(snapshot.getValue()).isEqualByComparingTo("19.99");
        assertThat(snapshot.getNumberOfRecurrences()).isNull();
        assertThat(snapshot.getRecurrenceUnit()).isNull();
    }

    @Test
    @DisplayName("Should map RECURRING price with recurrence fields correctly")
    void publish_recurringPrice_sendsMessageWithRecurrenceFieldsPopulated() {
        // GIVEN
        stubSuccessfulSend();

        List<Price> prices = List.of(
                new RecurringPrice(new BigDecimal("9.99"), 12, RecurrenceUnit.MONTH)
        );
        Instant timestamp = Instant.parse("2026-06-14T10:00:00Z");

        // WHEN
        cartEventProducer.publish("customer-1", "offer-sub", ActionType.MODIFY, prices, timestamp);

        // THEN
        ArgumentCaptor<CartEventMessage> messageCaptor = ArgumentCaptor.forClass(CartEventMessage.class);

        verify(kafkaTemplate).send(eq(TOPIC), eq("offer-sub"), messageCaptor.capture());

        CartEventMessage.PriceSnapshot snapshot = messageCaptor.getValue().getPrices().get(0);

        assertThat(snapshot.getType()).isEqualTo(PriceType.RECURRING);
        assertThat(snapshot.getValue()).isEqualByComparingTo("9.99");
        assertThat(snapshot.getNumberOfRecurrences()).isEqualTo(12);
        assertThat(snapshot.getRecurrenceUnit()).isEqualTo(RecurrenceUnit.MONTH);
    }

    @Test
    @DisplayName("Should map multiple prices into corresponding snapshots")
    void publish_multiplePrices_mapsEachPriceToCorrespondingSnapshot() {
        // GIVEN
        stubSuccessfulSend();

        List<Price> prices = List.of(
                new OneTimePrice(new BigDecimal("25.00")),
                new RecurringPrice(new BigDecimal("4.50"), 24, RecurrenceUnit.MONTH)
        );

        Instant timestamp = Instant.now();

        // WHEN
        cartEventProducer.publish("customer-1", "offer-router", ActionType.ADD, prices, timestamp);

        // THEN
        ArgumentCaptor<CartEventMessage> messageCaptor = ArgumentCaptor.forClass(CartEventMessage.class);

        verify(kafkaTemplate).send(eq(TOPIC), eq("offer-router"), messageCaptor.capture());

        assertThat(messageCaptor.getValue().getPrices()).hasSize(2);
        assertThat(messageCaptor.getValue().getPrices().get(0).getType())
                .isEqualTo(PriceType.ONE_TIME);
        assertThat(messageCaptor.getValue().getPrices().get(1).getType())
                .isEqualTo(PriceType.RECURRING);
    }

    @Test
    @DisplayName("Should publish DELETE action without modification")
    void publish_deleteAction_isSentAsIs() {
        // GIVEN
        stubSuccessfulSend();

        List<Price> prices = List.of(new OneTimePrice(new BigDecimal("19.99")));

        // WHEN
        cartEventProducer.publish("customer-1", "offer-1", ActionType.DELETE, prices, Instant.now());

        // THEN
        ArgumentCaptor<CartEventMessage> messageCaptor = ArgumentCaptor.forClass(CartEventMessage.class);

        verify(kafkaTemplate).send(eq(TOPIC), eq("offer-1"), messageCaptor.capture());

        assertThat(messageCaptor.getValue().getAction()).isEqualTo(ActionType.DELETE);
    }

    @Test
    @DisplayName("Should not throw exception when Kafka send fails")
    void publish_kafkaSendFails_doesNotThrow() {
        // GIVEN
        CompletableFuture<SendResult<String, CartEventMessage>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("broker unavailable"));

        when(kafkaTemplate.send(any(String.class), any(String.class), any(CartEventMessage.class)))
                .thenReturn(future);

        List<Price> prices = List.of(new OneTimePrice(new BigDecimal("19.99")));

        // WHEN / THEN
        assertDoesNotThrow(() ->
                cartEventProducer.publish(
                        "customer-1",
                        "offer-1",
                        ActionType.ADD,
                        prices,
                        Instant.now()
                )
        );
    }
}