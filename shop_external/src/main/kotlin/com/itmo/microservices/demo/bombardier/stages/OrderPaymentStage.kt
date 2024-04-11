package com.itmo.microservices.demo.bombardier.stages

import com.itmo.microservices.commonlib.annotations.InjectEventLogger
import com.itmo.microservices.commonlib.logging.EventLogger
import com.itmo.microservices.demo.bombardier.external.ExternalServiceApi
import com.itmo.microservices.demo.bombardier.external.FinancialOperationType
import com.itmo.microservices.demo.bombardier.external.PaymentStatus
import com.itmo.microservices.demo.bombardier.flow.UserManagement
import com.itmo.microservices.demo.bombardier.logging.OrderCommonNotableEvents
import com.itmo.microservices.demo.bombardier.logging.OrderPaymentNotableEvents.*
import com.itmo.microservices.demo.bombardier.utils.ConditionAwaiter
import com.itmo.microservices.demo.common.logging.EventLoggerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OrderPaymentStage : TestStage {
    @InjectEventLogger
    lateinit var eventLog: EventLogger

    lateinit var eventLogger: EventLoggerWrapper

    override suspend fun run(
        userManagement: UserManagement,
        externalServiceApi: ExternalServiceApi
    ): TestStage.TestContinuationType {
        eventLogger = EventLoggerWrapper(eventLog, testCtx().serviceName)

        val order = externalServiceApi.getOrder(testCtx().userId!!, testCtx().orderId!!)

        val paymentDetails = testCtx().paymentDetails
        paymentDetails.attempt++

        eventLogger.info(I_PAYMENT_STARTED, order, paymentDetails.attempt)

        paymentDetails.startedAt = System.currentTimeMillis()

        val paymentSubmissionDto = externalServiceApi.payOrder(
            testCtx().userId!!,
            testCtx().orderId!!
        ) // todo sukhoa add payment details to test ctx
        withContext(Dispatchers.IO) {
            Thread.sleep(5000)
        }

        ConditionAwaiter.awaitAtMost(1, TimeUnit.SECONDS)
            .condition {
                val history = externalServiceApi.getOrder(testCtx().userId!!, testCtx().orderId!!).paymentHistory
                history.any { it.transactionId == paymentSubmissionDto.transactionId }
            }
            .onFailure {
                eventLogger.error(E_TIMEOUT_EXCEEDED, order.id)
                if (it != null) {
                    throw it
                }
                throw TestStage.TestStageFailedException("Exception instead of silently fail")
            }.startWaiting()

        val paymentLogRecord = externalServiceApi.getOrder(testCtx().userId!!, testCtx().orderId!!).paymentHistory
            .find { it.transactionId == paymentSubmissionDto.transactionId }!!

        when (val status = paymentLogRecord.status) {
            PaymentStatus.SUCCESS -> {
                ConditionAwaiter.awaitAtMost(10, TimeUnit.SECONDS)
                    .condition {
                        val userChargedRecord =
                            externalServiceApi.userFinancialHistory(testCtx().userId!!, testCtx().orderId!!)
                                .find { it.paymentTransactionId == paymentSubmissionDto.transactionId }

                        userChargedRecord?.type == FinancialOperationType.WITHDRAW
                    }
                    .onFailure {
                        eventLogger.error(E_WITHDRAW_NOT_FOUND, order.id, testCtx().userId)
                        if (it != null) {
                            throw it
                        }
                        throw TestStage.TestStageFailedException("Exception instead of silently fail")
                    }.startWaiting()

                paymentDetails.finishedAt = System.currentTimeMillis()
                eventLogger.info(I_PAYMENT_SUCCESS, order.id, paymentDetails.attempt)

                return TestStage.TestContinuationType.CONTINUE
            }
            PaymentStatus.FAILED -> { // todo sukhoa check order status hasn't changed and user ne charged
                if (paymentDetails.attempt < 10) {
                    eventLogger.info(I_PAYMENT_RETRY, order.id, paymentDetails.attempt)
                    return TestStage.TestContinuationType.RETRY
                } else {
                    eventLogger.error(E_LAST_ATTEMPT_FAIL, order.id, paymentDetails.attempt)
                    paymentDetails.failedAt = System.currentTimeMillis()
                    return TestStage.TestContinuationType.FAIL
                }
            } // todo sukhoa not enough money
            else -> {
                eventLogger.error(
                    OrderCommonNotableEvents.E_ILLEGAL_ORDER_TRANSITION,
                    order.id, order.status, status
                )
                return TestStage.TestContinuationType.FAIL
            }
        }
    }
}