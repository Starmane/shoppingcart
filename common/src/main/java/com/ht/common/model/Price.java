package com.ht.common.model;

import com.ht.common.enums.PriceType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = OneTimePrice.class,
                name = "ONE_TIME"),
        @JsonSubTypes.Type(
                value = RecurringPrice.class,
                name = "RECURRING")
})
public abstract class Price {

    protected BigDecimal value;

    protected Price(BigDecimal value) {
        this.value = value;
    }

    public abstract PriceType getType();
}