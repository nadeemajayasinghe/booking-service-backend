package com.carRental.booking.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleEventConsumer {

    private final VehicleCacheService vehicleCacheService;

    @KafkaListener(
            topics  = "${app.kafka.topic.vehicle-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onVehicleCreated(
            @Payload VehicleEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[Kafka] CREATED vehicle={} partition={} offset={}",
                event.getVehicleId(), partition, offset);
        vehicleCacheService.upsert(event);
    }

    @KafkaListener(
            topics  = "${app.kafka.topic.vehicle-updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onVehicleUpdated(@Payload VehicleEvent event) {
        log.info("[Kafka] UPDATED vehicle={}", event.getVehicleId());
        vehicleCacheService.upsert(event);
    }

    @KafkaListener(
            topics  = "${app.kafka.topic.vehicle-deleted}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onVehicleDeleted(@Payload VehicleEvent event) {
        log.info("[Kafka] DELETED vehicle={}", event.getVehicleId());
        vehicleCacheService.remove(event.getVehicleId());
    }
}
