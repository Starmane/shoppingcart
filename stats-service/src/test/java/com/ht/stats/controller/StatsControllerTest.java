package com.ht.stats.controller;

import com.ht.common.enums.ActionType;
import com.ht.stats.exception.InvalidStatsRangeException;
import com.ht.stats.service.StatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@DisplayName("StatsController")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    private static final String OFFER_ID = "offer-1";
    private static final String FROM = "2026-01-01T00:00:00Z";
    private static final String TO = "2026-12-31T23:59:59Z";

    // ---------- GET /stats - working path ----------

    @Test
    @DisplayName("GET /stats - returns 200 with the count for a valid offerId/action/range")
    void getStats_validRequest_returnsOkWithCount() throws Exception {
        // given
        when(statsService.countByOfferAndAction(
                eq(OFFER_ID), eq(ActionType.ADD), eq(Instant.parse(FROM)), eq(Instant.parse(TO))))
                .thenReturn(3L);

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("action", "ADD")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.offerId").value(OFFER_ID))
                .andExpect(jsonPath("$.action").value("ADD"))
                .andExpect(jsonPath("$.from").value(FROM))
                .andExpect(jsonPath("$.to").value(TO))
                .andExpect(jsonPath("$.count").value(3));

        verify(statsService).countByOfferAndAction(OFFER_ID, ActionType.ADD, Instant.parse(FROM), Instant.parse(TO));
    }

    @Test
    @DisplayName("GET /stats - returns 200 with count 0 when no events match")
    void getStats_noMatchingEvents_returnsOkWithZeroCount() throws Exception {
        // given
        when(statsService.countByOfferAndAction(any(), any(), any(), any())).thenReturn(0L);

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", "offer-nonexistent")
                        .param("action", "DELETE")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("GET /stats - supports MODIFY action")
    void getStats_modifyAction_returnsOkWithCorrectAction() throws Exception {
        // given
        when(statsService.countByOfferAndAction(eq(OFFER_ID), eq(ActionType.MODIFY), any(), any()))
                .thenReturn(7L);

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("action", "MODIFY")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("MODIFY"))
                .andExpect(jsonPath("$.count").value(7));
    }

    // ---------- GET /stats - missing parameters ----------

    @Test
    @DisplayName("GET /stats - returns 400 when offerId is missing")
    void getStats_missingOfferId_returnsBadRequest() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("action", "ADD")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /stats - returns 400 when action is missing")
    void getStats_missingAction_returnsBadRequest() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /stats - returns 400 when from is missing")
    void getStats_missingFrom_returnsBadRequest() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("action", "ADD")
                        .param("to", TO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /stats - returns 400 when to is missing")
    void getStats_missingTo_returnsBadRequest() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("action", "ADD")
                        .param("from", FROM))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /stats - invalid parameter values ----------

    @Test
    @DisplayName("GET /stats - returns 400 when action is not a valid enum value")
    void getStats_invalidActionValue_returnsBadRequest() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("action", "BUY")
                        .param("from", FROM)
                        .param("to", TO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /stats - returns 400 when from is not a valid ISO-8601 date-time")
    void getStats_invalidFromFormat_returnsBadRequest() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("action", "ADD")
                        .param("from", "2026-01-01") // date only, not date-time
                        .param("to", TO))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /stats - returns 400 when to is not a valid ISO-8601 date-time")
    void getStats_invalidToFormat_returnsBadRequest() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("action", "ADD")
                        .param("from", FROM)
                        .param("to", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /stats - business rule violations delegated to service ----------

    @Test
    @DisplayName("GET /stats - returns 400 when 'from' is after 'to'")
    void getStats_fromAfterTo_returnsBadRequest() throws Exception {
        // given
        when(statsService.countByOfferAndAction(eq(OFFER_ID), eq(ActionType.ADD), any(), any()))
                .thenThrow(new InvalidStatsRangeException("'from' must not be after 'to'"));

        // when
        // then
        mockMvc.perform(get("/stats")
                        .param("offerId", OFFER_ID)
                        .param("action", "ADD")
                        .param("from", TO)   // intentionally swapped
                        .param("to", FROM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' must not be after 'to'"));
    }
}