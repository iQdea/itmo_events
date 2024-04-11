package com.itmo.microservices.demo.bombardier.stages

import com.itmo.microservices.commonlib.annotations.InjectEventLogger
import com.itmo.microservices.commonlib.logging.EventLogger
import com.itmo.microservices.demo.bombardier.external.ExternalServiceApi
import com.itmo.microservices.demo.bombardier.flow.UserManagement
import com.itmo.microservices.demo.bombardier.logging.OrderCreationNotableEvents.I_ORDER_CREATED
import com.itmo.microservices.demo.common.logging.EventLoggerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class OrderCreationStage : TestStage {
    @InjectEventLogger
    lateinit var eventLog: EventLogger

    lateinit var eventLogger: EventLoggerWrapper

    override suspend fun run(
        userManagement: UserManagement,
        externalServiceApi: ExternalServiceApi
    ): TestStage.TestContinuationType {
        eventLogger = EventLoggerWrapper(eventLog, testCtx().serviceName)

        val order = externalServiceApi.createOrder(testCtx().userId!!)
        val catalogue = externalServiceApi.createCatalogue(testCtx().userId!!)
        var counter = 0
        val max = 4
        while (counter < max) {
            externalServiceApi.createItemInCatalogue(testCtx().userId!!, catalogue.id)
            withContext(Dispatchers.IO) {
                Thread.sleep(5000)
            }
            counter += 1
        }
        eventLogger.info(I_ORDER_CREATED, order.id)
        testCtx().orderId = order.id
        testCtx().catalogueId = catalogue.id
        return TestStage.TestContinuationType.CONTINUE
    }
}