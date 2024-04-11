package com.itmo.microservices.shop.order.config


import com.itmo.microservices.shop.order.api.*
import com.itmo.microservices.shop.order.logic.OrderState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.AggregateRegistry
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.*
import javax.annotation.PostConstruct

@Configuration
class OrderBoundedContextConfig {
    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Autowired
    private lateinit var aggregateRegistry: AggregateRegistry

    @PostConstruct
    fun init() {
        aggregateRegistry.register(OrderAggregate::class, OrderState::class) {
            registerStateTransition(OrderCreatedEvent::class, OrderState::createNewOrder)
            registerStateTransition(ProductPutEvent::class, OrderState::putProductToOrder)
            registerStateTransition(OrderBookedEvent::class, OrderState::bookOrder)
            registerStateTransition(OrderDiscardEvent::class, OrderState::discardOrder)
            registerStateTransition(OrderPaidEvent::class, OrderState::paidOrder)
            registerStateTransition(OrderShippingEvent::class, OrderState::shippingOrder)
            registerStateTransition(OrderCompletedEvent::class, OrderState::completedOrder)
        }
    }
    @Bean
    fun orderEsService(): EventSourcingService<UUID, OrderAggregate, OrderState> = eventSourcingServiceFactory.create()
}