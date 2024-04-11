package com.itmo.microservices.shop.payment.service

import com.itmo.microservices.shop.order.api.OrderAggregate
import com.itmo.microservices.shop.order.api.model.BookingLogModel
import com.itmo.microservices.shop.order.api.model.BookingStatus
import com.itmo.microservices.shop.order.logic.OrderState
import com.itmo.microservices.shop.payment.api.PaymentAggregate
import com.itmo.microservices.shop.payment.api.PaymentCreatedEvent
import com.itmo.microservices.shop.payment.api.PaymentFailedEvent
import com.itmo.microservices.shop.payment.api.PaymentSucceededEvent
import com.itmo.microservices.shop.payment.api.model.*
import com.itmo.microservices.shop.payment.logic.PaymentState
import com.itmo.microservices.shop.payment.transactions.service.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import ru.quipy.core.EventSourcingService
import java.lang.IllegalStateException
import java.util.*


@Service
class PaymentService(
    private val transactionService: TransactionService,
    private val paymentsEsService: EventSourcingService<UUID, PaymentAggregate, PaymentState>,
    private val ordersEsService: EventSourcingService<UUID, OrderAggregate, OrderState>,
) {
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    fun createPayment(amount: Int, user: String): PaymentCreatedEvent {
        val transaction = transactionService.createTransaction(amount)
        return paymentsEsService.create { payment ->
            payment.createNewPayment(user = user, transactionId = transaction.transactionId, amount = amount)
        }
    }

    fun successPayment(paymentId: UUID): PaymentSucceededEvent {
        paymentsEsService.getState(paymentId)?.let { transactionService.successTransaction(it.transactionId) }
        return paymentsEsService.update(paymentId) { payment ->
            payment.succeeded(paymentId)
        }
    }

    fun failPayment(paymentId: UUID): PaymentFailedEvent {
        paymentsEsService.getState(paymentId)?.let { transactionService.failTransaction(it.transactionId) }
        return paymentsEsService.update(paymentId) { payment ->
            payment.failed(paymentId)
        }
    }

    fun processPayment(orderId: UUID, user: UserDetails): PaymentSubmissionDto {
        val amount = getOrderItemsAmount(orderId)
        if (amount == 0) {
            throw IllegalStateException("No order with id $orderId")
        }
        var payment = PaymentCreatedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "",
            System.currentTimeMillis(),
            FinancialOperationType.WITHDRAW,
            PaymentStatus.CREATED,
            1
        )
        val result = kotlin.runCatching {
            payment = createPayment(amount, user.username)
            PaymentSubmissionDto(payment.timestamp, payment.transactionId)
        }.onSuccess {
            successPayment(payment.paymentId)
            ordersEsService.update(orderId) { order ->
                order.paidOrder(orderId)
            }
        }.onFailure {
            failPayment(payment.paymentId)
            ordersEsService.update(orderId) { order ->
                order.discardOrder(orderId)
            }
        }
        return result.getOrThrow()
    }

    fun getOrderItemsAmount(orderId: UUID): Int {
        var amount = 0
        val order = ordersEsService.getState(orderId)
        if (order != null) {
            val orderItems = order.orderProducts.toList().map { it.second }
            for (item in orderItems) {
                val criteria: MutableSet<Criteria> = mutableSetOf()
                val query = Query()
                criteria.add(Criteria.where("orderId").`is`(orderId))
                criteria.add(Criteria.where("itemId").`is`(item.productId))
                val cond = criteria.toList()
                query.addCriteria(Criteria().andOperator(cond))
                val data = mongoTemplate.find(
                    query,
                    BookingLogModel::class.java,
                    "bookings-logs-cache"
                )
                if (data.size != 0) {
                    val itemBooking = data.first()
                    if (itemBooking.status !== BookingStatus.FAILED) {
                        amount += item.amount * item.price
                    }
                }
            }
        }
        return amount
    }

    fun getFinLog(orderId: UUID, user: UserDetails): List<UserAccountFinancialLogRecordDto> {
        val logs: MutableSet<UserAccountFinancialLogRecordDto> = mutableSetOf()
        val payments = mongoTemplate.find(
            Query.query(Criteria.where("user").`is`(user.username)),
            PaymentModel::class.java,
            "payments-cache"
        )
        for (payment in payments) {
            val log = UserAccountFinancialLogRecordDto(
                type = FinancialOperationType.WITHDRAW,
                amount = payment.amount,
                orderId = orderId,
                paymentTransactionId = payment.transactionId,
                timestamp = payment.timestamp
            )
            logs.add(log)
        }
        return logs.toList()
    }
}