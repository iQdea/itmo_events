package com.itmo.microservices.shop.payment.logic

import com.itmo.microservices.shop.payment.api.PaymentAggregate
import com.itmo.microservices.shop.payment.api.PaymentCreatedEvent
import com.itmo.microservices.shop.payment.api.PaymentFailedEvent
import com.itmo.microservices.shop.payment.api.PaymentSucceededEvent
import com.itmo.microservices.shop.payment.api.model.FinancialOperationType
import com.itmo.microservices.shop.payment.api.model.PaymentStatus
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.util.*

class PaymentState : AggregateState<UUID, PaymentAggregate> {
    private lateinit var paymentId: UUID
    var status: PaymentStatus = PaymentStatus.CREATED
    var operationType: FinancialOperationType = FinancialOperationType.WITHDRAW
    var transactionId: UUID = UUID.randomUUID()
    var user: String = ""
    override fun getId() = paymentId

    fun createNewPayment(
        id: UUID = UUID.randomUUID(),
        user: String,
        transactionId: UUID,
        amount: Int
    ): PaymentCreatedEvent {
        if (amount <= 0)
            throw IllegalStateException("Amount must be greater than zero")
        return PaymentCreatedEvent(
            id,
            transactionId,
            user,
            timestamp = System.currentTimeMillis(),
            operationType = FinancialOperationType.WITHDRAW,
            status = this.status,
            amount
        )
    }

    fun succeeded(paymentId: UUID): PaymentSucceededEvent {
        return PaymentSucceededEvent(paymentId)
    }

    fun failed(paymentId: UUID): PaymentFailedEvent {
        return PaymentFailedEvent(paymentId)
    }

    @StateTransitionFunc
    fun createNewPayment(event: PaymentCreatedEvent) {
        this.operationType = event.operationType
        this.user = event.user
        this.paymentId = event.paymentId
        this.transactionId = event.transactionId
    }

    @StateTransitionFunc
    fun succeeded(event: PaymentSucceededEvent) {
        this.status = PaymentStatus.SUCCESS
    }

    @StateTransitionFunc
    fun failed(event: PaymentFailedEvent) {
        this.status = PaymentStatus.FAILED
    }
}