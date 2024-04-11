package com.itmo.microservices.shop.payment.transactions.api

import com.itmo.microservices.shop.payment.transactions.api.model.TransactionStatus
import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.util.*

const val TRANSACTION_CREATED = "TRANSACTION_CREATED"
const val SUCCEEDED = "SUCCEEDED"
const val FAILED = "FAILED"

@DomainEvent(name = TRANSACTION_CREATED)
data class TransactionCreatedEvent(
    val transactionId: UUID,
    val timestamp: Long,
    val amount: Int,
    val status: TransactionStatus
) : Event<TransactionsAggregate>(
    name = TRANSACTION_CREATED,
)

@DomainEvent(name = SUCCEEDED)
data class TransactionSucceededEvent(
    val transactionId: UUID,
) : Event<TransactionsAggregate>(
    name = SUCCEEDED,
)

@DomainEvent(name = FAILED)
data class TransactionFailedEvent(
    val transactionId: UUID,
) : Event<TransactionsAggregate>(
    name = FAILED,
)