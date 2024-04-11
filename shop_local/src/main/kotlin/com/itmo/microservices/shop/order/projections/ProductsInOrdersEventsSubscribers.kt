package com.itmo.microservices.shop.order.projections


import com.itmo.microservices.shop.order.api.OrderAggregate
import com.itmo.microservices.shop.order.api.ProductPutEvent
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
class ProductsInOrdersEventsSubscribers(
    private val productsCacheRepository: ProductsInOrdersCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager
) {
    val logger: Logger = LoggerFactory.getLogger(ProductsInOrdersEventsSubscribers::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(OrderAggregate::class, "products-in-orders::cache") {
            `when`(ProductPutEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    productsCacheRepository.save(
                        ProductInOrder(
                            event.productId,
                            event.orderId,
                            event.title,
                            event.amount,
                            event.price
                        )
                    )
                }
                logger.info("Update orders products cache, add product ${event.productId} to order ${event.orderId}")
            }
        }
    }
}

@Document("products-in-orders-cache")
data class ProductInOrder(
    @Id
    val productId: UUID,
    val orderId: UUID,
    val title: String,
    val amount: Int,
    val price: Int
)

@Repository
interface ProductsInOrdersCacheRepository : MongoRepository<ProductInOrder, UUID>