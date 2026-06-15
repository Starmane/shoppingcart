package com.ht.stats.model;

import com.ht.common.enums.ActionType;
import com.ht.stats.messaging.CartEventMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Immutable record of a single cart action (ADD/MODIFY/DELETE) performed
 * on an offer - the source of truth for statistics queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cart_events")
@CompoundIndex(name = "offer_action_time_idx", def = "{'offerId': 1, 'action': 1, 'timestamp': 1}")
public class CartEvent {

    @Id
    private String id;

    private String customerId;
    private String offerId;
    private ActionType action;
    private List<CartEventMessage.PriceSnapshot> prices;
    private Instant timestamp;

    public CartEvent(String customerId, String offerId, ActionType action,
                     List<CartEventMessage.PriceSnapshot> prices, Instant timestamp) {
        this.customerId = customerId;
        this.offerId = offerId;
        this.action = action;
        this.prices = prices;
        this.timestamp = timestamp;
    }
}