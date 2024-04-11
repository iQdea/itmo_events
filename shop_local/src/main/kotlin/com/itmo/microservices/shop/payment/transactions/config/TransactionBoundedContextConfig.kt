package com.itmo.microservices.shop.payment.transactions.config

import com.itmo.microservices.shop.payment.transactions.api.TransactionCreatedEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionFailedEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionSucceededEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionsAggregate
import com.itmo.microservices.shop.payment.transactions.logic.TransactionsState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.AggregateRegistry
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.*
import javax.annotation.PostConstruct

@Configuration
class TransactionBoundedContextConfig {
    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Autowired
    private lateinit var aggregateRegistry: AggregateRegistry

    @PostConstruct
    fun init() {
        aggregateRegistry.register(TransactionsAggregate::class, TransactionsState::class) {
            registerStateTransition(TransactionCreatedEvent::class, TransactionsState::initiateTransaction)
            registerStateTransition(TransactionFailedEvent::class, TransactionsState::failed)
            registerStateTransition(TransactionSucceededEvent::class, TransactionsState::succeeded)
        }
    }
    @Bean
    fun transactionsEsService(): EventSourcingService<UUID, TransactionsAggregate, TransactionsState> = eventSourcingServiceFactory.create()
}