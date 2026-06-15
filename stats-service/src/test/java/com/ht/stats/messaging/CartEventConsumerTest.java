package com.ht.stats.messaging;

import com.ht.common.enums.ActionType;
import com.ht.common.enums.PriceType;
import com.ht.stats.model.CartEvent;
import com.ht.stats.messaging.CartEventMessage.PriceSnapshot;
import com.ht.stats.repository.CartEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartEventConsumer")
class CartEventConsumerTest {

    @Mock
    private CartEventRepository cartEventRepository;

    private CartEventConsumer cartEventConsumer;

    @BeforeEach
    void setUp() {
        cartEventConsumer = new CartEventConsumer(cartEventRepository);
    }

    private CartEventMessage sampleMessage(ActionType action) {
        return new CartEventMessage(
                "customer-1",
                "offer-1",
                action,
                List.of(new PriceSnapshot(PriceType.ONE_TIME, new BigDecimal("19.99"), null, null)),
                Instant.parse("2026-06-14T10:00:00Z")
        );
    }

    // ---------- basic mapping ----------

    @Test
    @DisplayName("onCartEvent - maps message fields to CartEvent and saves it")
    void onCartEvent_validMessage_mapsAndSavesCartEvent() {
        // given
        CartEventMessage message = sampleMessage(ActionType.ADD);

        // when
        cartEventConsumer.onCartEvent(message);

        // then
        ArgumentCaptor<CartEvent> captor = ArgumentCaptor.forClass(CartEvent.class);
        verify(cartEventRepository).save(captor.capture());

        CartEvent saved = captor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo("customer-1");
        assertThat(saved.getOfferId()).isEqualTo("offer-1");
        assertThat(saved.getAction()).isEqualTo(ActionType.ADD);
        assertThat(saved.getTimestamp()).isEqualTo(Instant.parse("2026-06-14T10:00:00Z"));
        assertThat(saved.getPrices()).hasSize(1);
        assertThat(saved.getPrices().get(0).getType()).isEqualTo(PriceType.ONE_TIME);
        assertThat(saved.getPrices().get(0).getValue()).isEqualByComparingTo("19.99");
    }

    @Test
    @DisplayName("onCartEvent - preserves MODIFY action")
    void onCartEvent_modifyAction_savesWithModifyAction() {
        // given
        CartEventMessage message = sampleMessage(ActionType.MODIFY);

        // when
        cartEventConsumer.onCartEvent(message);

        // then
        ArgumentCaptor<CartEvent> captor = ArgumentCaptor.forClass(CartEvent.class);
        verify(cartEventRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(ActionType.MODIFY);
    }

    @Test
    @DisplayName("onCartEvent - preserves DELETE action")
    void onCartEvent_deleteAction_savesWithDeleteAction() {
        // given
        CartEventMessage message = sampleMessage(ActionType.DELETE);

        // when
        cartEventConsumer.onCartEvent(message);

        // then
        ArgumentCaptor<CartEvent> captor = ArgumentCaptor.forClass(CartEvent.class);
        verify(cartEventRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(ActionType.DELETE);
    }

    // ---------- multiple prices ----------

    @Test
    @DisplayName("onCartEvent - preserves multiple price snapshots in order")
    void onCartEvent_multiplePrices_preservesAllPriceSnapshots() {
        // given
        CartEventMessage message = new CartEventMessage(
                "customer-1",
                "offer-router",
                ActionType.ADD,
                List.of(
                        new PriceSnapshot(PriceType.ONE_TIME, new BigDecimal("25.00"), null, null),
                        new PriceSnapshot(PriceType.RECURRING, new BigDecimal("4.50"), 24,
                                com.ht.common.enums.RecurrenceUnit.MONTH)
                ),
                Instant.now()
        );

        // when
        cartEventConsumer.onCartEvent(message);

        // then
        ArgumentCaptor<CartEvent> captor = ArgumentCaptor.forClass(CartEvent.class);
        verify(cartEventRepository).save(captor.capture());

        List<PriceSnapshot> savedPrices = captor.getValue().getPrices();
        assertThat(savedPrices).hasSize(2);
        assertThat(savedPrices.get(0).getType()).isEqualTo(PriceType.ONE_TIME);
        assertThat(savedPrices.get(1).getType()).isEqualTo(PriceType.RECURRING);
        assertThat(savedPrices.get(1).getNumberOfRecurrences()).isEqualTo(24);
    }

    // ---------- recurring price snapshot ----------

    @Test
    @DisplayName("onCartEvent - preserves recurring price fields")
    void onCartEvent_recurringPrice_preservesRecurrenceFields() {
        // given
        CartEventMessage message = new CartEventMessage(
                "customer-1",
                "offer-sub",
                ActionType.ADD,
                List.of(new PriceSnapshot(PriceType.RECURRING, new BigDecimal("9.99"), 12,
                        com.ht.common.enums.RecurrenceUnit.MONTH)),
                Instant.now()
        );

        // when
        cartEventConsumer.onCartEvent(message);

        // then
        ArgumentCaptor<CartEvent> captor = ArgumentCaptor.forClass(CartEvent.class);
        verify(cartEventRepository).save(captor.capture());

        PriceSnapshot saved = captor.getValue().getPrices().get(0);
        assertThat(saved.getType()).isEqualTo(PriceType.RECURRING);
        assertThat(saved.getValue()).isEqualByComparingTo("9.99");
        assertThat(saved.getNumberOfRecurrences()).isEqualTo(12);
        assertThat(saved.getRecurrenceUnit()).isEqualTo(com.ht.common.enums.RecurrenceUnit.MONTH);
    }

    // ---------- repository interaction count ----------

    @Test
    @DisplayName("onCartEvent - calls save exactly once per message")
    void onCartEvent_callsSaveExactlyOnce() {
        // given
        CartEventMessage message = sampleMessage(ActionType.ADD);

        // when
        cartEventConsumer.onCartEvent(message);

        // then
        verify(cartEventRepository, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.any(CartEvent.class));
    }
}
