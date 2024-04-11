package com.itmo.microservices.shop.delivery.logic

import com.itmo.microservices.shop.delivery.api.*
import com.itmo.microservices.shop.delivery.api.model.DeliverySubmissionOutcome
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.time.Instant
import java.util.*

class DeliveryState : AggregateState<UUID, DeliveryAggregate> {
    private lateinit var deliveryId: UUID
    var orderId: UUID = UUID.randomUUID()
    var outcome: DeliverySubmissionOutcome = DeliverySubmissionOutcome.CREATED
    var submissionStartedTime: Instant = Instant.now()
    override fun getId() = deliveryId

    fun createNewDelivery(
        id: UUID = UUID.randomUUID(),
        orderId: UUID,
        time: Instant
    ): DeliveryCreatedEvent {
        return DeliveryCreatedEvent(
            id,
            orderId,
            1,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            time,
            outcome = DeliverySubmissionOutcome.CREATED
        )
    }

    fun succeeded(deliveryId: UUID): DeliverySucceededEvent {
        return DeliverySucceededEvent(deliveryId)
    }

    fun failed(deliveryId: UUID): DeliveryFailedEvent {
        return DeliveryFailedEvent(deliveryId)
    }

    fun expired(deliveryId: UUID): DeliveryExpiredEvent {
        return DeliveryExpiredEvent(deliveryId)
    }

    @StateTransitionFunc
    fun createNewDelivery(event: DeliveryCreatedEvent) {
        this.outcome = event.outcome
        this.deliveryId = event.deliveryId
        this.orderId = event.orderId
        this.submissionStartedTime = event.submissionStartedTime
    }

    @StateTransitionFunc
    fun succeeded(event: DeliverySucceededEvent) {
        this.outcome = DeliverySubmissionOutcome.SUCCESS
    }

    @StateTransitionFunc
    fun failed(event: DeliveryFailedEvent) {
        this.outcome = DeliverySubmissionOutcome.FAILURE
    }

    @StateTransitionFunc
    fun expired(event: DeliveryExpiredEvent) {
        this.outcome = DeliverySubmissionOutcome.EXPIRED
    }
}