package com.ht.common.enums;

import com.ht.common.model.Price;

/**
 * Discriminator for the {@link Price} polymorphic hierarchy.
 */
public enum PriceType {
    ONE_TIME,
    RECURRING
}
