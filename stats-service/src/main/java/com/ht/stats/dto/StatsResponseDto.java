package com.ht.stats.dto;

import com.ht.common.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponseDto {
    private String offerId;
    private ActionType action;
    private Instant from;
    private Instant to;
    private long count;
}