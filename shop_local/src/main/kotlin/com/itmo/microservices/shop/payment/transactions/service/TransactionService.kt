package com.itmo.microservices.shop.payment.transactions.service

import com.itmo.microservices.shop.payment.transactions.api.TransactionCreatedEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionFailedEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionSucceededEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionsAggregate
import com.itmo.microservices.shop.payment.transactions.logic.TransactionsState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import ru.quipy.core.EventSourcingService
import java.util.*

@Service
class TransactionService(private val transactionsEsService: EventSourcingService<UUID, TransactionsAggregate, TransactionsState>) {
    fun createTransaction(amount: Int): TransactionCreatedEvent {
        return transactionsEsService.create { transaction ->
            transaction.initiateTransaction(amount = amount)
        }
    }

    fun successTransaction(transactionId: UUID): TransactionSucceededEvent {
        return transactionsEsService.update(transactionId) {transaction ->
            transaction.succeeded(transactionId)
        }
    }

    fun failTransaction(transactionId: UUID): TransactionFailedEvent {
        return transactionsEsService.update(transactionId) {transaction ->
            transaction.failed(transactionId)
        }
    }
}