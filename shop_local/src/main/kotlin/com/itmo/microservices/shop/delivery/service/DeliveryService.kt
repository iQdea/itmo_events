package com.itmo.microservices.shop.delivery.service

import com.itmo.microservices.shop.delivery.api.*
import com.itmo.microservices.shop.delivery.api.model.DeliveryInfoRecord
import com.itmo.microservices.shop.delivery.api.model.DeliveryModel
import com.itmo.microservices.shop.delivery.api.model.DeliveryStatusDto
import com.itmo.microservices.shop.delivery.api.model.DeliverySubmissionOutcome
import com.itmo.microservices.shop.delivery.logic.DeliveryState
import com.itmo.microservices.shop.order.api.OrderAggregate
import com.itmo.microservices.shop.order.logic.OrderState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import ru.quipy.core.EventSourcingService
import java.time.Instant
import java.util.UUID

@Service
class DeliveryService(
    private val deliveryEsService: EventSourcingService<UUID, DeliveryAggregate, DeliveryState>,
    private val ordersEsService: EventSourcingService<UUID, OrderAggregate, OrderState>
) {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    fun createDelivery(
        id: UUID = UUID.randomUUID(),
        orderId: UUID,
        time: Instant
    ): DeliveryCreatedEvent {
        return deliveryEsService.create { delivery ->
            delivery.createNewDelivery(id, orderId, time)
        }
    }

    fun successDelivery(deliveryId: UUID): DeliverySucceededEvent {
        return deliveryEsService.update(deliveryId) { delivery ->
            delivery.succeeded(deliveryId)
        }
    }

    fun failDelivery(deliveryId: UUID): DeliveryFailedEvent {
        return deliveryEsService.update(deliveryId) { delivery ->
            delivery.failed(deliveryId)
        }
    }

    fun expireDelivery(deliveryId: UUID): DeliveryExpiredEvent {
        return deliveryEsService.update(deliveryId) { delivery ->
            delivery.expired(deliveryId)
        }
    }

    fun processDelivery(
        orderId: UUID,
        time: Instant
    ): DeliveryInfoRecord {
        var delivery = DeliveryCreatedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            Instant.now(),
            DeliverySubmissionOutcome.CREATED
        )
        val result = kotlin.runCatching {
            delivery = createDelivery(orderId = orderId, time = time)
            DeliveryInfoRecord(
                delivery.outcome,
                delivery.preparedTime,
                delivery.attempts,
                delivery.submittedTime,
                delivery.orderId,
                delivery.submissionStartedTime
            )
        }.onSuccess {
            ordersEsService.update(orderId) { order ->
                order.shippingOrder(orderId)
            }
        }.onFailure {
            failDelivery(delivery.deliveryId)
        }
        return result.getOrThrow()
    }

    fun confirmOrderDelivery(deliveryId: UUID): DeliveryStatusDto {
        val deliveryState = deliveryEsService.getState(deliveryId)
        if (deliveryState !== null) {
            return if (deliveryState.submissionStartedTime < Instant.now()) {
                expireDelivery(deliveryId)
                ordersEsService.update(deliveryState.orderId) { order ->
                    order.paidOrder(deliveryState.orderId)
                }
                DeliveryStatusDto(deliveryId, outcome = DeliverySubmissionOutcome.EXPIRED)
            } else {
                successDelivery(deliveryId)
                ordersEsService.update(deliveryState.orderId) { order ->
                    order.completedOrder(deliveryState.orderId)
                }
                DeliveryStatusDto(deliveryId, outcome = DeliverySubmissionOutcome.SUCCESS)
            }
        } else {
            return DeliveryStatusDto(deliveryId, outcome = DeliverySubmissionOutcome.FAILURE)
        }
    }

    fun generateSlots(amount: Int): List<Instant> {
        return generateSequence(Instant.now()) { it.plusSeconds(20) }.take(amount).toList()
    }

    fun getHistory(orderId: UUID): List<DeliveryInfoRecord> {
        val logs: MutableSet<DeliveryInfoRecord> = mutableSetOf()
        val deliveries = mongoTemplate.find(
            Query.query(Criteria.where("orderId").`is`(orderId)),
            DeliveryModel::class.java,
            "deliveries-cache"
        )
        for (delivery in deliveries) {
            val log = DeliveryInfoRecord(
                outcome = delivery.outcome,
                preparedTime = delivery.preparedTime,
                attempts = delivery.attempts,
                submittedTime = delivery.submittedTime,
                orderId = delivery.orderId,
                submissionStartedTime = delivery.submissionStartedTime
            )
            logs.add(log)
        }
        return logs.toList()
    }
}