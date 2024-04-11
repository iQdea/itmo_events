package com.itmo.microservices.shop.payment.transactions.logic

import com.itmo.microservices.shop.payment.transactions.api.*
import com.itmo.microservices.shop.payment.transactions.api.model.TransactionStatus
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.util.UUID

class TransactionsState : AggregateState<UUID, TransactionsAggregate> {
    private lateinit var transactionId: UUID
    private var transactionStatus = TransactionStatus.CREATED
    private var amount: Int = 0

    override fun getId() = transactionId

    fun initiateTransaction(
        id: UUID = UUID.randomUUID(),
        amount: Int
    ): TransactionCreatedEvent {
        val time = System.currentTimeMillis()
        if (amount <= 0)
            throw IllegalStateException("Amount must be greater than zero")
        return TransactionCreatedEvent(id, time, amount, status = this.transactionStatus)
    }

    fun succeeded(transactionId: UUID): TransactionSucceededEvent {
        return TransactionSucceededEvent(transactionId)
    }

    fun failed(transactionId: UUID): TransactionFailedEvent {
        return TransactionFailedEvent(transactionId)
    }

    @StateTransitionFunc
    fun initiateTransaction(event: TransactionCreatedEvent) {
        this.transactionId = event.transactionId
        this.amount = event.amount
    }

    @StateTransitionFunc
    fun succeeded(event: TransactionSucceededEvent) {
        this.transactionStatus = TransactionStatus.SUCCEEDED
    }

    @StateTransitionFunc
    fun failed(event: TransactionFailedEvent) {
        this.transactionStatus = TransactionStatus.FAILED
    }
}