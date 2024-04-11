package com.itmo.microservices.shop.order.projections

import com.itmo.microservices.shop.order.api.*
import com.itmo.microservices.shop.order.api.model.OrderStatus
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
class OrdersEventsSubscriber(
    private val orderCacheRepository: OrderCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager
) {

    private val logger: Logger = LoggerFactory.getLogger(OrdersEventsSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(OrderAggregate::class, "orders::cache") {
            `when`(OrderCreatedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    orderCacheRepository.save(Order(event.orderId, event.timestamp, event.status))
                }
                logger.info("Update orders cache, create order ${event.orderId}")
            }
            `when`(OrderPaidEvent::class) {event ->
                withContext(Dispatchers.IO) {
                    val order = orderCacheRepository.findAll().first { it.orderId == event.orderId }
                    order.status = event.status
                    if (order.status === OrderStatus.PAID) {
                        orderCacheRepository.save(order)
                    }
                }
            }
            `when`(OrderDiscardEvent::class) {event ->
                withContext(Dispatchers.IO) {
                    val order = orderCacheRepository.findAll().first { it.orderId == event.orderId }
                    order.status = event.status
                    if (order.status === OrderStatus.DISCARD) {
                        orderCacheRepository.save(order)
                    }
                }
            }
            `when`(OrderShippingEvent::class) {event ->
                withContext(Dispatchers.IO) {
                    val order = orderCacheRepository.findAll().first { it.orderId == event.orderId }
                    order.status = event.status
                    if (order.status === OrderStatus.SHIPPING) {
                        orderCacheRepository.save(order)
                    }
                }
            }
            `when`(OrderCompletedEvent::class) {event ->
                withContext(Dispatchers.IO) {
                    val order = orderCacheRepository.findAll().first { it.orderId == event.orderId }
                    order.status = event.status
                    if (order.status === OrderStatus.COMPLETED) {
                        orderCacheRepository.save(order)
                    }
                }
            }
        }
    }
}

@Document("orders-cache")
data class Order(
    @Id
    var orderId: UUID = UUID.randomUUID(),
    val timestamp: Long,
    var status: OrderStatus
)

@Repository
interface OrderCacheRepository : MongoRepository<Order, UUID>