package com.ht.shoppingcart.dto;

import com.ht.common.enums.PriceType;
import com.ht.common.enums.RecurrenceUnit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceDto {

    @NotNull(message = "Price type is mandatory (ONE_TIME or RECURRING)")
    private PriceType type;

    @NotNull(message = "Price value is mandatory")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price value must be greater than 0")
    private BigDecimal value;

    @Positive(message = "numberOfRecurrences must be a positive number")
    private Integer numberOfRecurrences;

    private RecurrenceUnit recurrenceUnit;

    /**
     * Cross-field validation: RECURRING prices must have both
     * numberOfRecurrences and recurrenceUnit; ONE_TIME prices must not.
     * Called manually from CartItemDto's @AssertTrue to avoid
     * needing a separate custom validator class.
     */
    @JsonIgnore
    public boolean isValidForType() {
        if (type == null) return false;
        if (type == PriceType.RECURRING) {
            return numberOfRecurrences != null && recurrenceUnit != null;
        }
        // ONE_TIME - recurrence fields must be absent
        return numberOfRecurrences == null && recurrenceUnit == null;
    }
}