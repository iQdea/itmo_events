package com.itmo.microservices.shop.user.config

import com.itmo.microservices.shop.user.api.UserAggregate
import com.itmo.microservices.shop.user.api.UserCreatedEvent
import com.itmo.microservices.shop.user.logic.UserState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.AggregateRegistry
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.*
import javax.annotation.PostConstruct

@Configuration
class UserBoundedContextConfig {
    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Autowired
    private lateinit var aggregateRegistry: AggregateRegistry

    @PostConstruct
    fun init() {
        aggregateRegistry.register(UserAggregate::class, UserState::class) {
            registerStateTransition(UserCreatedEvent::class, UserState::registerUser)
        }
    }
    @Bean
    fun userEsService(): EventSourcingService<UUID, UserAggregate, UserState> = eventSourcingServiceFactory.create()
}