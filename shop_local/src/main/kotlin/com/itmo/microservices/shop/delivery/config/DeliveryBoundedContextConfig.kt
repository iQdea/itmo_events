package com.itmo.microservices.shop.delivery.config

import com.itmo.microservices.shop.delivery.api.*
import com.itmo.microservices.shop.delivery.logic.DeliveryState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.AggregateRegistry
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.*
import javax.annotation.PostConstruct

@Configuration
class DeliveryBoundedContextConfig {
    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Autowired
    private lateinit var aggregateRegistry: AggregateRegistry

    @PostConstruct
    fun init() {
        aggregateRegistry.register(DeliveryAggregate::class, DeliveryState::class) {
            registerStateTransition(DeliveryCreatedEvent::class, DeliveryState::createNewDelivery)
            registerStateTransition(DeliverySucceededEvent::class, DeliveryState::succeeded)
            registerStateTransition(DeliveryFailedEvent::class, DeliveryState::failed)
            registerStateTransition(DeliveryExpiredEvent::class, DeliveryState::expired)
        }
    }
    @Bean
    fun deliveryEsService(): EventSourcingService<UUID, DeliveryAggregate, DeliveryState> = eventSourcingServiceFactory.create()
}