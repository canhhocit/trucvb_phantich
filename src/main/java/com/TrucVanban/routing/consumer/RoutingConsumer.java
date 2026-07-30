package com.TrucVanban.routing.consumer;

import com.TrucVanban.routing.dto.request.RoutingRequest;
import com.TrucVanban.routing.service.RoutingService;
import com.TrucVanban.shared.config.RabbitMQConfig;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoutingConsumer {

    RoutingService routingService;

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_EXCHANGE_QUEUE)
    public void dispatchToAgencyB(RoutingRequest request) {
        log.info("[routing] Nhận routing message: transactionCode={}", request.getTransactionCode());
        routingService.dispatch(request);
    }
}
