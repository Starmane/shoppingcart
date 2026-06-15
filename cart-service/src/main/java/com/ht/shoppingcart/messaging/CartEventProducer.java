package com.ht.shoppingcart.messaging;

import com.ht.common.enums.ActionType;
import com.ht.common.enums.PriceType;
import com.ht.shoppingcart.model.CartEventMessage;
import com.ht.common.model.Price;
import com.ht.common.model.RecurringPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventProducer {

    private final KafkaTemplate<String, CartEventMessage> kafkaTemplate;

    @Value("${app.kafka.topic.cart-events}")
    private String topic;

    public void publish(String customerId, String offerId, ActionType action,
                        List<Price> prices, Instant timestamp) {

        List<CartEventMessage.PriceSnapshot> snapshots = prices.stream()
                .map(this::toSnapshot)
                .toList();

        CartEventMessage message = new CartEventMessage(customerId, offerId, action, snapshots, timestamp);

        // Key by offerId so all events for the same offer preserve order
        // across partitions/consumers on the stats-service side.
        kafkaTemplate.send(topic, offerId, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish cart event for offerId={}: {}", offerId, ex.getMessage(), ex);
                    } else {
                        log.debug("Published cart event for offerId={} action={}", offerId, action);
                    }
                });
    }

    private CartEventMessage.PriceSnapshot toSnapshot(Price price) {
        if (price instanceof RecurringPrice recurring) {
            return new CartEventMessage.PriceSnapshot(PriceType.RECURRING, recurring.getValue(),
                    recurring.getNumberOfRecurrences(), recurring.getRecurrenceUnit());
        }
        return new CartEventMessage.PriceSnapshot(PriceType.ONE_TIME, price.getValue(), null, null);
    }
}