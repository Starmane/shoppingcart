package com.ht.stats.service;

import com.ht.common.enums.ActionType;
import com.ht.stats.exception.InvalidStatsRangeException;
import com.ht.stats.repository.CartEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService")
class StatsServiceTest {

    @Mock
    private CartEventRepository cartEventRepository;

    private StatsService statsService;

    private static final String OFFER_ID = "offer-1";
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-12-31T23:59:59Z");

    @BeforeEach
    void setUp() {
        statsService = new StatsService(cartEventRepository);
    }

    // ---------- happy path ----------

    @Test
    @DisplayName("countByOfferAndAction - returns the count from the repository for a valid range")
    void countByOfferAndAction_validRange_returnsRepositoryCount() {
        // given
        when(cartEventRepository.countByOfferIdAndActionAndTimestampBetween(OFFER_ID, ActionType.ADD, FROM, TO))
                .thenReturn(5L);

        // when
        long result = statsService.countByOfferAndAction(OFFER_ID, ActionType.ADD, FROM, TO);

        // then
        assertThat(result).isEqualTo(5L);
        verify(cartEventRepository).countByOfferIdAndActionAndTimestampBetween(OFFER_ID, ActionType.ADD, FROM, TO);
    }

    @Test
    @DisplayName("countByOfferAndAction - returns 0 when the repository finds no matching events")
    void countByOfferAndAction_noMatchingEvents_returnsZero() {
        // given
        when(cartEventRepository.countByOfferIdAndActionAndTimestampBetween(any(), any(), any(), any()))
                .thenReturn(0L);

        // when
        long result = statsService.countByOfferAndAction(OFFER_ID, ActionType.DELETE, FROM, TO);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("countByOfferAndAction - passes through the action parameter to the repository")
    void countByOfferAndAction_modifyAction_queriesWithModifyAction() {
        // given
        when(cartEventRepository.countByOfferIdAndActionAndTimestampBetween(OFFER_ID, ActionType.MODIFY, FROM, TO))
                .thenReturn(2L);

        // when
        long result = statsService.countByOfferAndAction(OFFER_ID, ActionType.MODIFY, FROM, TO);

        // then
        assertThat(result).isEqualTo(2L);
        verify(cartEventRepository).countByOfferIdAndActionAndTimestampBetween(eq(OFFER_ID), eq(ActionType.MODIFY), eq(FROM), eq(TO));
    }

    // ---------- range validation ----------

    @Test
    @DisplayName("countByOfferAndAction - throws InvalidStatsRangeException when 'from' is after 'to'")
    void countByOfferAndAction_fromAfterTo_throwsInvalidStatsRangeException() {
        // given
        Instant from = TO;
        Instant to = FROM; // swapped - from is after to

        // when / then
        assertThatThrownBy(() -> statsService.countByOfferAndAction(OFFER_ID, ActionType.ADD, from, to))
                .isInstanceOf(InvalidStatsRangeException.class)
                .hasMessage("'from' must not be after 'to'");

        verify(cartEventRepository, never())
                .countByOfferIdAndActionAndTimestampBetween(any(), any(), any(), any());
    }

    @Test
    @DisplayName("countByOfferAndAction - allows 'from' equal to 'to'")
    void countByOfferAndAction_fromEqualsTo_doesNotThrow() {
        // given
        Instant sameInstant = FROM;
        when(cartEventRepository.countByOfferIdAndActionAndTimestampBetween(OFFER_ID, ActionType.ADD, sameInstant, sameInstant))
                .thenReturn(1L);

        // when
        long result = statsService.countByOfferAndAction(OFFER_ID, ActionType.ADD, sameInstant, sameInstant);

        // then
        assertThat(result).isEqualTo(1L);
        verify(cartEventRepository).countByOfferIdAndActionAndTimestampBetween(OFFER_ID, ActionType.ADD, sameInstant, sameInstant);
    }

    @Test
    @DisplayName("countByOfferAndAction - allows 'from' before 'to'")
    void countByOfferAndAction_fromBeforeTo_doesNotThrow() {
        // given
        when(cartEventRepository.countByOfferIdAndActionAndTimestampBetween(OFFER_ID, ActionType.ADD, FROM, TO))
                .thenReturn(0L);

        // when / then
        assertThat(statsService.countByOfferAndAction(OFFER_ID, ActionType.ADD, FROM, TO)).isZero();
    }
}