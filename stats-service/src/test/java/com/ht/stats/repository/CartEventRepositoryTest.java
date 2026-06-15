package com.ht.stats.repository;

import com.ht.common.enums.ActionType;
import com.ht.stats.model.CartEvent;
import com.ht.stats.messaging.CartEventMessage.PriceSnapshot;
import com.ht.common.enums.PriceType;
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
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
@DisplayName("CartEventRepository")
class CartEventRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @Autowired
    private CartEventRepository cartEventRepository;

    private static final String OFFER_ID = "offer-1";
    private static final Instant JAN_1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant JUN_1 = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant DEC_31 = Instant.parse("2026-12-31T23:59:59Z");

    @BeforeEach
    void setUp() {
        cartEventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        cartEventRepository.deleteAll();
    }

    private CartEvent event(String customerId, String offerId, ActionType action, Instant timestamp) {
        return new CartEvent(customerId, offerId, action,
                List.of(new PriceSnapshot(PriceType.ONE_TIME, new BigDecimal("19.99"), null, null)),
                timestamp);
    }

    // ---------- basic matching ----------

    @Test
    @DisplayName("countByOfferIdAndActionAndTimestampBetween - counts events matching offerId, action, and within range")
    void countByOfferIdAndActionAndTimestampBetween_matchingEvents_returnsCorrectCount() {
        // given
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.ADD, JUN_1));
        cartEventRepository.save(event("customer-2", OFFER_ID, ActionType.ADD, JUN_1.plusSeconds(60)));

        // when
        long count = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.ADD, JAN_1, DEC_31);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByOfferIdAndActionAndTimestampBetween - returns 0 when no events exist")
    void countByOfferIdAndActionAndTimestampBetween_noEvents_returnsZero() {
        // given
        // no events saved

        // when
        long count = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.ADD, JAN_1, DEC_31);

        // then
        assertThat(count).isZero();
    }

    // ---------- offerId filtering ----------

    @Test
    @DisplayName("countByOfferIdAndActionAndTimestampBetween - does not count events for a different offerId")
    void countByOfferIdAndActionAndTimestampBetween_differentOfferId_excluded() {
        // given
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.ADD, JUN_1));
        cartEventRepository.save(event("customer-1", "offer-other", ActionType.ADD, JUN_1));

        // when
        long count = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.ADD, JAN_1, DEC_31);

        // then
        assertThat(count).isEqualTo(1);
    }

    // ---------- action filtering ----------

    @Test
    @DisplayName("countByOfferIdAndActionAndTimestampBetween - does not count events with a different action")
    void countByOfferIdAndActionAndTimestampBetween_differentAction_excluded() {
        // given
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.ADD, JUN_1));
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.DELETE, JUN_1));
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.MODIFY, JUN_1));

        // when
        long addCount = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.ADD, JAN_1, DEC_31);
        long deleteCount = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.DELETE, JAN_1, DEC_31);
        long modifyCount = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.MODIFY, JAN_1, DEC_31);

        // then
        assertThat(addCount).isEqualTo(1);
        assertThat(deleteCount).isEqualTo(1);
        assertThat(modifyCount).isEqualTo(1);
    }

    // ---------- timestamp range filtering ----------

    @Test
    @DisplayName("countByOfferIdAndActionAndTimestampBetween - excludes events before the 'from' timestamp")
    void countByOfferIdAndActionAndTimestampBetween_eventBeforeRange_excluded() {
        // given
        Instant beforeRange = JAN_1.minusSeconds(1);
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.ADD, beforeRange));

        // when
        long count = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.ADD, JAN_1, DEC_31);

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countByOfferIdAndActionAndTimestampBetween - excludes events after the 'to' timestamp")
    void countByOfferIdAndActionAndTimestampBetween_eventAfterRange_excluded() {
        // given
        Instant afterRange = DEC_31.plusSeconds(1);
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.ADD, afterRange));

        // when
        long count = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.ADD, JAN_1, DEC_31);

        // then
        assertThat(count).isZero();
    }

    // ---------- combined filter ----------

    @Test
    @DisplayName("countByOfferIdAndActionAndTimestampBetween - all three filters must match simultaneously")
    void countByOfferIdAndActionAndTimestampBetween_allFiltersAppliedTogether() {
        // given
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.ADD, JUN_1));            // matches all
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.DELETE, JUN_1));         // wrong action
        cartEventRepository.save(event("customer-1", "offer-other", ActionType.ADD, JUN_1));       // wrong offer
        cartEventRepository.save(event("customer-1", OFFER_ID, ActionType.ADD, DEC_31.plusSeconds(1))); // out of range

        // when
        long count = cartEventRepository.countByOfferIdAndActionAndTimestampBetween(
                OFFER_ID, ActionType.ADD, JAN_1, DEC_31);

        // then
        assertThat(count).isEqualTo(1);
    }
}