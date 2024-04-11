package com.itmo.microservices.shop.catalogue.projections

import com.itmo.microservices.shop.catalogue.api.CatalogueAggregate
import com.itmo.microservices.shop.catalogue.api.ProductAddedEvent
import com.itmo.microservices.shop.catalogue.api.ProductBookedEvent
import com.itmo.microservices.shop.order.projections.BookingLogRecord
import com.itmo.microservices.shop.order.projections.BookingLogRecordsCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct
import javax.persistence.Id

@Component
class ProductsInCataloguesEventsSubscribers(
    private val bookingCacheRepository: BookingLogRecordsCacheRepository,
    private val productsCacheRepository: ProductsInCataloguesCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager,
) {
    val logger: Logger = LoggerFactory.getLogger(ProductsInCataloguesEventsSubscribers::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(CatalogueAggregate::class, "products-in-catalogues::cache") {
            `when`(ProductAddedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    productsCacheRepository.save(
                        ProductInCatalogue(
                            event.productId,
                            event.catalogueId,
                            event.title,
                            event.description,
                            event.price,
                            event.amount
                        )
                    )
                }
                logger.info("Update products cache, create product ${event.productId} in catalogue ${event.catalogueId}")
            }
            `when`(ProductBookedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val product = productsCacheRepository.findAll().first { it.productId == event.productId }
                    product.amount = product.amount.minus(event.amount)
                    productsCacheRepository.save(product)
                    bookingCacheRepository.save(
                        BookingLogRecord(
                            bookingId = event.bookingId,
                            orderId = event.orderId,
                            itemId = event.productId,
                            status = event.status,
                            amount = event.amount,
                            timestamp = event.timestamp
                        )
                    )
                }
                logger.info("Update products cache, take ${event.amount} products ${event.productId}. Create booking ${event.bookingId} log")
            }
        }
    }
}

@Document("products-in-catalogues-cache")
data class ProductInCatalogue(
    @Id
    val productId: UUID = UUID.randomUUID(),
    val catalogueId: UUID,
    val title: String,
    val description: String,
    val price: Int,
    var amount: Int
)

@Repository
interface ProductsInCataloguesCacheRepository : MongoRepository<ProductInCatalogue, UUID>