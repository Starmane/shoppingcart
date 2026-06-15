package com.ht.common.model;

import com.ht.common.enums.PriceType;
import com.ht.common.enums.RecurrenceUnit;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RecurringPrice extends Price {

    private Integer numberOfRecurrences;

    private RecurrenceUnit recurrenceUnit;

    public RecurringPrice(BigDecimal value, Integer numberOfRecurrences, RecurrenceUnit recurrenceUnit) {
        super(value);
        this.numberOfRecurrences = numberOfRecurrences;
        this.recurrenceUnit = recurrenceUnit;
    }

    @Override
    public PriceType getType() {
        return PriceType.RECURRING;
    }
}