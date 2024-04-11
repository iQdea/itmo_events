package com.itmo.microservices.demo.bombardier.stages

import com.itmo.microservices.commonlib.annotations.InjectEventLogger
import com.itmo.microservices.commonlib.logging.EventLogger
import com.itmo.microservices.demo.bombardier.external.DeliverySubmissionOutcome
import com.itmo.microservices.demo.bombardier.external.ExternalServiceApi
import com.itmo.microservices.demo.bombardier.external.FinancialOperationType
import com.itmo.microservices.demo.bombardier.external.OrderStatus
import com.itmo.microservices.demo.bombardier.flow.UserManagement
import com.itmo.microservices.demo.bombardier.logging.OrderCommonNotableEvents
import com.itmo.microservices.demo.bombardier.logging.OrderDeliveryNotableEvents.*
import com.itmo.microservices.demo.common.logging.EventLoggerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class OrderDeliveryStage : TestStage {
    @InjectEventLogger
    lateinit var eventLog: EventLogger

    lateinit var eventLogger: EventLoggerWrapper

    override fun isFinal(): Boolean {
        return true
    }

    override suspend fun run(
        userManagement: UserManagement,
        externalServiceApi: ExternalServiceApi
    ): TestStage.TestContinuationType {
        eventLogger = EventLoggerWrapper(eventLog, testCtx().serviceName)

        val orderBeforeDelivery = externalServiceApi.getOrder(testCtx().userId!!, testCtx().orderId!!)

        if (orderBeforeDelivery.deliveryDuration == null) {
            eventLogger.error(E_INCORRECT_ORDER_STATUS, orderBeforeDelivery.id, orderBeforeDelivery.status)
            return TestStage.TestContinuationType.FAIL
        }

        externalServiceApi.confirmDelivery(testCtx().deliveryId!!, testCtx().userId!!)
        withContext(Dispatchers.IO) {
            Thread.sleep(5000)
        }
        val orderAfterDelivery = externalServiceApi.getOrder(testCtx().userId!!, testCtx().orderId!!)
        when (orderAfterDelivery.status) {
            is OrderStatus.OrderDelivered -> {
                val deliveryLogArray = externalServiceApi.deliveryLog(testCtx().userId!!, testCtx().orderId!!)
                deliveryLogArray.forEach { deliveryLog ->
                    if (deliveryLog.outcome != DeliverySubmissionOutcome.SUCCESS) {
                        eventLogger.error(E_DELIVERY_OUTCOME_FAIL, orderAfterDelivery.id)
                        return TestStage.TestContinuationType.FAIL
                    }
                }


                eventLogger.info(I_DELIVERY_SUCCESS, orderAfterDelivery.id)
            }
            is OrderStatus.OrderRefund -> {
                val userFinancialHistory =
                    externalServiceApi.userFinancialHistory(testCtx().userId!!, testCtx().orderId!!)
                if (userFinancialHistory.filter { it.type == FinancialOperationType.WITHDRAW }.sumOf { it.amount } !=
                    userFinancialHistory.filter { it.type == FinancialOperationType.REFUND }.sumOf { it.amount }) {
                    eventLogger.error(E_WITHDRAW_AND_REFUND_DIFFERENT, orderAfterDelivery.id,
                        userFinancialHistory.filter { it.type == FinancialOperationType.WITHDRAW }
                            .sumOf { it.amount },
                        userFinancialHistory.filter { it.type == FinancialOperationType.REFUND }
                            .sumOf { it.amount }
                    )
                }
                eventLogger.info(I_REFUND_CORRECT, orderAfterDelivery.id)
            }
            else -> {
                eventLogger.error(
                    OrderCommonNotableEvents.E_ILLEGAL_ORDER_TRANSITION,
                    orderBeforeDelivery.id,
                    orderBeforeDelivery.status,
                    orderAfterDelivery.status
                )
                return TestStage.TestContinuationType.FAIL
            }
        }
        return TestStage.TestContinuationType.CONTINUE
    }
}