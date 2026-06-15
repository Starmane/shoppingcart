package com.ht.common.model;

import com.ht.common.enums.PriceType;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OneTimePrice extends Price {

    public OneTimePrice(BigDecimal value) {
        super(value);
    }

    @Override
    public PriceType getType() {
        return PriceType.ONE_TIME;
    }
}