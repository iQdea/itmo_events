package com.itmo.microservices.shop.payment.config

import com.itmo.microservices.shop.payment.api.PaymentAggregate
import com.itmo.microservices.shop.payment.api.PaymentCreatedEvent
import com.itmo.microservices.shop.payment.api.PaymentFailedEvent
import com.itmo.microservices.shop.payment.api.PaymentSucceededEvent
import com.itmo.microservices.shop.payment.logic.PaymentState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.AggregateRegistry
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.*
import javax.annotation.PostConstruct

@Configuration
class PaymentBoundedContextConfig {
    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Autowired
    private lateinit var aggregateRegistry: AggregateRegistry

    @PostConstruct
    fun init() {
        aggregateRegistry.register(PaymentAggregate::class, PaymentState::class) {
            registerStateTransition(PaymentCreatedEvent::class, PaymentState::createNewPayment)
            registerStateTransition(PaymentFailedEvent::class, PaymentState::failed)
            registerStateTransition(PaymentSucceededEvent::class, PaymentState::succeeded)
        }
    }
    @Bean
    fun paymentEsService(): EventSourcingService<UUID, PaymentAggregate, PaymentState> = eventSourcingServiceFactory.create()
}