package com.itmo.microservices.shop.delivery.api

import com.itmo.microservices.shop.delivery.api.model.DeliverySubmissionOutcome

import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.time.Instant
import java.util.*

const val DELIVERY_CREATED = "DELIVERY_CREATED"
const val DELIVERY_SUCCEEDED = "DELIVERY_SUCCEEDED"
const val DELIVERY_FAILED = "DELIVERY_FAILED"
const val DELIVERY_EXPIRED = "DELIVERY_EXPIRED"

@DomainEvent( name = DELIVERY_CREATED)
data class DeliveryCreatedEvent(
    val deliveryId: UUID,
    val orderId: UUID,
    val attempts: Int,
    val preparedTime: Long,
    val submittedTime: Long,
    val submissionStartedTime: Instant,
    val outcome: DeliverySubmissionOutcome
) : Event<DeliveryAggregate> (
    name = DELIVERY_CREATED
)

@DomainEvent( name = DELIVERY_FAILED)
data class DeliveryFailedEvent(
    val deliveryId: UUID
) : Event<DeliveryAggregate> (
    name = DELIVERY_FAILED
)

@DomainEvent( name = DELIVERY_SUCCEEDED)
data class DeliverySucceededEvent(
    val deliveryId: UUID
) : Event<DeliveryAggregate> (
    name = DELIVERY_SUCCEEDED
)

@DomainEvent( name = DELIVERY_EXPIRED)
data class DeliveryExpiredEvent(
    val deliveryId: UUID
) : Event<DeliveryAggregate> (
    name = DELIVERY_EXPIRED
)