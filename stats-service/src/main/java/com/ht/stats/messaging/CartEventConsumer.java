package com.ht.stats.messaging;

import com.ht.stats.model.CartEvent;
import com.ht.stats.repository.CartEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventConsumer {

    private final CartEventRepository cartEventRepository;

    @KafkaListener(topics = "${app.kafka.topic.cart-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onCartEvent(CartEventMessage message) {
        log.info("Received cart event: customerId={}, offerId={}, action={}, timestamp={}",
                message.getCustomerId(), message.getOfferId(), message.getAction(), message.getTimestamp());

        CartEvent event = new CartEvent(
                message.getCustomerId(),
                message.getOfferId(),
                message.getAction(),
                message.getPrices(),
                message.getTimestamp()
        );

        cartEventRepository.save(event);
    }
}