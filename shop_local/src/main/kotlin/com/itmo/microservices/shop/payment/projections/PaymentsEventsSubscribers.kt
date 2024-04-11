package com.itmo.microservices.shop.payment.projections


import com.itmo.microservices.shop.payment.api.PaymentAggregate
import com.itmo.microservices.shop.payment.api.PaymentCreatedEvent
import com.itmo.microservices.shop.payment.api.PaymentFailedEvent
import com.itmo.microservices.shop.payment.api.PaymentSucceededEvent
import com.itmo.microservices.shop.payment.api.model.FinancialOperationType
import com.itmo.microservices.shop.payment.api.model.PaymentStatus
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
class PaymentsEventsSubscribers(
    private val paymentsCacheRepository: PaymentsCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager
) {

    private val logger: Logger = LoggerFactory.getLogger(PaymentsEventsSubscribers::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(PaymentAggregate::class, "payments::cache") {
            `when`(PaymentCreatedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    paymentsCacheRepository.save(
                        Payment(
                            event.paymentId,
                            event.transactionId,
                            event.user,
                            event.timestamp,
                            event.status,
                            event.operationType,
                            event.amount
                        )
                    )
                }
                logger.info("Update payments cache, create new with id ${event.paymentId}")
            }
            `when`(PaymentSucceededEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val payment = paymentsCacheRepository.findAll().first { it.paymentId == event.paymentId }
                    payment.status = PaymentStatus.SUCCESS
                    paymentsCacheRepository.save(payment)
                }
                logger.info("Update payments cache, payment with id ${event.paymentId} succeeded")
            }
            `when`(PaymentFailedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    val payment = paymentsCacheRepository.findAll().first { it.paymentId == event.paymentId }
                    payment.status = PaymentStatus.FAILED
                    paymentsCacheRepository.save(payment)
                }
                logger.info("Update payments cache, payment with id ${event.paymentId} failed")
            }
        }
    }
}

@Document("payments-cache")
data class Payment(
    @Id
    val paymentId: UUID,
    val transactionId: UUID,
    val user: String,
    val timestamp: Long,
    var status: PaymentStatus,
    var operationType: FinancialOperationType,
    val amount: Int
)

@Repository
interface PaymentsCacheRepository : MongoRepository<Payment, UUID>