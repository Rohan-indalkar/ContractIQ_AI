package com.contractiq.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class OutboxPublisher {
    private final OutboxEventRepository events;
    private final KafkaTemplate<String, ProcessingEvent> kafka;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository events, KafkaTemplate<String, ProcessingEvent> kafka, ObjectMapper objectMapper) {
        this.events = events;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEventEntity event : events.findPendingForUpdate(PageRequest.of(0, 25))) {
            try {
                ProcessingEvent payload = objectMapper.treeToValue(event.payloadJson, ProcessingEvent.class);
                kafka.send(event.eventType, event.aggregateId, payload).get();
                event.publishedAt = LocalDateTime.now();
                events.save(event);
            } catch (Exception exception) {
                org.slf4j.LoggerFactory.getLogger(OutboxPublisher.class)
                        .warn("Outbox publish deferred eventId={} type={}", event.id, event.eventType, exception);
                break;
            }
        }
    }
}
