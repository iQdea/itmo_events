package com.itmo.microservices.shop.payment.api

import com.itmo.microservices.shop.payment.api.model.FinancialOperationType
import com.itmo.microservices.shop.payment.api.model.PaymentStatus
import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.util.*

const val PAYMENT_CREATED = "PAYMENT_CREATED"
const val PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED"
const val PAYMENT_FAILED = "PAYMENT_FAILED"

@DomainEvent( name = PAYMENT_CREATED)
data class PaymentCreatedEvent(
    val paymentId: UUID,
    val transactionId: UUID,
    val user: String,
    val timestamp: Long,
    val operationType: FinancialOperationType,
    val status: PaymentStatus,
    val amount: Int
) : Event<PaymentAggregate> (
    name = PAYMENT_CREATED
)

@DomainEvent( name = PAYMENT_FAILED)
data class PaymentFailedEvent(
    val paymentId: UUID
) : Event<PaymentAggregate> (
    name = PAYMENT_FAILED
)

@DomainEvent( name = PAYMENT_SUCCEEDED)
data class PaymentSucceededEvent(
    val paymentId: UUID
) : Event<PaymentAggregate> (
    name = PAYMENT_SUCCEEDED
)