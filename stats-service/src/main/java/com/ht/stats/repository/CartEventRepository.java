package com.ht.stats.repository;

import com.ht.common.enums.ActionType;
import com.ht.stats.model.CartEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;

public interface CartEventRepository extends MongoRepository<CartEvent, String> {

    long countByOfferIdAndActionAndTimestampBetween(
            String offerId, ActionType action, Instant from, Instant to);
}