package com.ht.common.enums;

/**
 * Unit of time for a recurring price's recurrence count.
 * E.g. RecurrenceCount=12, Unit=MONTH -> "12 months"
 *      RecurrenceCount=7,  Unit=DAY   -> "7 days"
 */
public enum RecurrenceUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}