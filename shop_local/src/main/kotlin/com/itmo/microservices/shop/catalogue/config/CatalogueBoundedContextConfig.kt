package com.itmo.microservices.shop.catalogue.config

import com.itmo.microservices.shop.catalogue.api.CatalogueAggregate
import com.itmo.microservices.shop.catalogue.api.CatalogueCreatedEvent
import com.itmo.microservices.shop.catalogue.api.ProductAddedEvent
import com.itmo.microservices.shop.catalogue.api.ProductBookedEvent
import com.itmo.microservices.shop.catalogue.logic.CatalogueState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.core.AggregateRegistry
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.*
import javax.annotation.PostConstruct

@Configuration
class CatalogueBoundedContextConfig {
    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Autowired
    private lateinit var aggregateRegistry: AggregateRegistry

    @PostConstruct
    fun init() {
        aggregateRegistry.register(CatalogueAggregate::class, CatalogueState::class) {
            registerStateTransition(CatalogueCreatedEvent::class, CatalogueState::createNewCatalogue)
            registerStateTransition(ProductAddedEvent::class, CatalogueState::createNewProduct)
            registerStateTransition(ProductBookedEvent::class, CatalogueState::bookProduct)
        }
    }
    @Bean
    fun catalogueEsService(): EventSourcingService<UUID, CatalogueAggregate, CatalogueState> = eventSourcingServiceFactory.create()
}