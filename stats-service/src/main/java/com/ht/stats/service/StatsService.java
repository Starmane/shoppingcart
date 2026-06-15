package com.ht.stats.service;

import com.ht.common.enums.ActionType;
import com.ht.stats.exception.InvalidStatsRangeException;
import com.ht.stats.repository.CartEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final CartEventRepository cartEventRepository;

    public long countByOfferAndAction(String offerId, ActionType action, Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new InvalidStatsRangeException("'from' must not be after 'to'");
        }
        return cartEventRepository.countByOfferIdAndActionAndTimestampBetween(offerId, action, from, to);
    }
}