package com.itmo.microservices.shop.delivery.projections

import com.itmo.microservices.shop.delivery.api.*
import com.itmo.microservices.shop.delivery.api.model.DeliverySubmissionOutcome
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
import java.time.Instant
import javax.annotation.PostConstruct
import javax.persistence.Id

@Service
class DeliveryEventsSubscribers(
    private val deliveriesCacheRepository: DeliveriesCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager
) {

    private val logger: Logger = LoggerFactory.getLogger(DeliveryEventsSubscribers::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(DeliveryAggregate::class, "deliveries::cache") {
            `when`(DeliveryCreatedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    deliveriesCacheRepository.save(
                        Delivery(
                            event.deliveryId,
                            event.orderId,
                            event.attempts,
                            event.preparedTime,
                            event.submittedTime,
                            event.submissionStartedTime,
                            event.outcome
                        )
                    )
                }
                logger.info("Update deliveries cache, create new with id ${event.deliveryId}")
            }
            `when`(DeliverySucceededEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val delivery = deliveriesCacheRepository.findAll().first { it.deliveryId == event.deliveryId }
                    delivery.outcome = DeliverySubmissionOutcome.SUCCESS
                    deliveriesCacheRepository.save(delivery)
                }
                logger.info("Update deliveries cache, delivery with id ${event.deliveryId} succeeded")
            }
            `when`(DeliveryFailedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val delivery = deliveriesCacheRepository.findAll().first { it.deliveryId == event.deliveryId }
                    delivery.outcome = DeliverySubmissionOutcome.FAILURE
                    deliveriesCacheRepository.save(delivery)
                }
                logger.info("Update deliveries cache, delivery with id ${event.deliveryId} failed")
            }
            `when`(DeliveryExpiredEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val delivery = deliveriesCacheRepository.findAll().first { it.deliveryId == event.deliveryId }
                    delivery.outcome = DeliverySubmissionOutcome.EXPIRED
                    deliveriesCacheRepository.save(delivery)
                }
                logger.info("Update deliveries cache, delivery with id ${event.deliveryId} expired")
            }
        }
    }
}

@Document("deliveries-cache")
data class Delivery(
    @Id
    val deliveryId: UUID,
    val orderId: UUID,
    val attempts: Int,
    val preparedTime: Long,
    val submittedTime: Long,
    val submissionStartedTime: Instant,
    var outcome: DeliverySubmissionOutcome
)

@Repository
interface DeliveriesCacheRepository : MongoRepository<Delivery, UUID>