package com.itmo.microservices.shop.order.projections

import com.itmo.microservices.shop.order.api.OrderAggregate
import com.itmo.microservices.shop.order.api.OrderBookedEvent
import com.itmo.microservices.shop.order.api.model.BookingStatus
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
class BookingsEventsSubscribers(
    private val bookingsCacheRepository: BookingsCacheRepository,
    private val orderCacheRepository: OrderCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager
) {

    private val logger: Logger = LoggerFactory.getLogger(BookingsEventsSubscribers::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(OrderAggregate::class, "bookings::cache") {
            `when`(OrderBookedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val order = orderCacheRepository.findAll().first { it.orderId == event.orderId }
                    order.status = event.status
                    if (order.status === OrderStatus.BOOKED) {
                        orderCacheRepository.save(order)
                    }
                    bookingsCacheRepository.save(Booking(event.bookingId, event.failedItems))
                }
                logger.info("Update bookings cache, booking order ${event.orderId}")
            }

        }
    }
}

@Document("bookings-logs-cache")
data class BookingLogRecord(
    @Id
    var bookingId: UUID = UUID.randomUUID(),
    var orderId: UUID,
    var itemId: UUID,
    var status: BookingStatus,
    var amount: Int,
    val timestamp: Long
)

@Document("bookings-cache")
data class Booking(
    @Id
    var bookingId: UUID = UUID.randomUUID(),
    var failedItems: Set<UUID>
)

@Repository
interface BookingLogRecordsCacheRepository : MongoRepository<BookingLogRecord, UUID>

@Repository
interface BookingsCacheRepository : MongoRepository<Booking, UUID>