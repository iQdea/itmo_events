package com.itmo.microservices.shop.catalogue.projections

import com.itmo.microservices.shop.catalogue.api.CatalogueAggregate
import com.itmo.microservices.shop.catalogue.api.CatalogueCreatedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import ru.quipy.streams.AggregateSubscriptionsManager
import javax.annotation.PostConstruct
import javax.persistence.Id

@Service
class CataloguesEventsSubscriber(
    private val catalogueCacheRepository: CatalogueCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager
) {

    private val logger: Logger = LoggerFactory.getLogger(CataloguesEventsSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(CatalogueAggregate::class, "catalogues::cache") {
            `when`(CatalogueCreatedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    catalogueCacheRepository.save(Catalogue(event.catalogueId, event.catalogueName))
                }
                logger.info("Update catalogues cache, create catalogue ${event.catalogueId}-${event.catalogueName}")
            }
        }
    }
}

@Document("catalogues-cache")
data class Catalogue(
    @Id
    var catalogueId: UUID = UUID.randomUUID(),
    var catalogueName: String
)

@Repository
interface CatalogueCacheRepository : MongoRepository<Catalogue, UUID>