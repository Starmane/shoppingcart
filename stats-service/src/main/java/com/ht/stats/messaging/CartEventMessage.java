package com.ht.stats.messaging;

import com.ht.common.enums.ActionType;
import com.ht.common.enums.PriceType;
import com.ht.common.enums.RecurrenceUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartEventMessage {

    private String customerId;
    private String offerId;
    private ActionType action;
    private List<PriceSnapshot> prices;
    private Instant timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceSnapshot {
        private PriceType type;
        private BigDecimal value;
        private Integer numberOfRecurrences;
        private RecurrenceUnit recurrenceUnit;
    }
}