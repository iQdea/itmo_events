package com.itmo.microservices.shop.payment.transactions.projections

import com.itmo.microservices.shop.payment.transactions.api.TransactionCreatedEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionFailedEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionSucceededEvent
import com.itmo.microservices.shop.payment.transactions.api.TransactionsAggregate
import com.itmo.microservices.shop.payment.transactions.api.model.TransactionStatus
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
class TransactionsEventsSubscribers(
    private val transactionCacheRepository: TransactionsCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager
) {

    private val logger: Logger = LoggerFactory.getLogger(TransactionsEventsSubscribers::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(TransactionsAggregate::class, "transactions::cache") {
            `when`(TransactionCreatedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    transactionCacheRepository.save(Transaction(event.transactionId, event.timestamp, event.amount, event.status))
                }
                logger.info("Update transactions cache, create new with id ${event.transactionId}")
            }
            `when`(TransactionSucceededEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val transaction = transactionCacheRepository.findAll().first { it.transactionId == event.transactionId }
                    transaction.status = TransactionStatus.SUCCEEDED
                    transactionCacheRepository.save(transaction)
                }
                logger.info("Update transactions cache, transaction with id ${event.transactionId} succeeded")
            }
            `when`(TransactionFailedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val transaction = transactionCacheRepository.findAll().first { it.transactionId == event.transactionId }
                    transaction.status = TransactionStatus.FAILED
                    transactionCacheRepository.save(transaction)
                }
                logger.info("Update transactions cache, transaction with id ${event.transactionId} failed")
            }
        }
    }
}

@Document("transactions-cache")
data class Transaction(
    @Id
    var transactionId: UUID,
    val timestamp: Long,
    val amount: Int,
    var status: TransactionStatus
)

@Repository
interface TransactionsCacheRepository : MongoRepository<Transaction, UUID>