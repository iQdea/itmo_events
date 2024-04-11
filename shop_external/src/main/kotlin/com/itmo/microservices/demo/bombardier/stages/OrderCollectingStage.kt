package com.itmo.microservices.demo.bombardier.stages

import com.itmo.microservices.commonlib.annotations.InjectEventLogger
import com.itmo.microservices.commonlib.logging.EventLogger
import com.itmo.microservices.demo.bombardier.external.ExternalServiceApi
import com.itmo.microservices.demo.bombardier.flow.UserManagement
import com.itmo.microservices.demo.bombardier.logging.OrderCollectingNotableEvents.*
import com.itmo.microservices.demo.common.logging.EventLoggerWrapper
import org.springframework.stereotype.Component
import java.lang.Integer.min
import java.util.*
import kotlin.random.Random

@Component
class OrderCollectingStage : TestStage {
    @InjectEventLogger
    private lateinit var eventLog: EventLogger

    lateinit var eventLogger: EventLoggerWrapper

    override suspend fun run(userManagement: UserManagement, externalServiceApi: ExternalServiceApi): TestStage.TestContinuationType {
        eventLogger = EventLoggerWrapper(eventLog, testCtx().serviceName)

        eventLogger.info(I_ADDING_ITEMS, testCtx().orderId)

        val itemIds = mutableMapOf<UUID, Int>()
        val items = externalServiceApi.getAvailableItems(testCtx().userId!!, testCtx().catalogueId!!)
        if (items.size < 3) {
            throw IllegalStateException("Products found: ${items.size}, required: at least 3")
        }
        repeat(Random.nextInt(1, min(20, items.size))) {
            val itemToAdd = items.random()
            val amount = Random.nextInt(1, min(20, itemToAdd.amount))
            itemIds[itemToAdd.id] = amount

            externalServiceApi.putItemToOrder(testCtx().userId!!, testCtx().orderId!!, itemToAdd.id, amount)
        }

        val finalOrder = externalServiceApi.getOrder(testCtx().userId!!, testCtx().orderId!!)
        val orderMap = finalOrder.itemsMap.mapKeys { it.key.id }
        itemIds.forEach { (id, count) ->
            if (!orderMap.containsKey(id)) {
                eventLogger.error(E_ADD_ITEMS_FAIL, id, count, 0)
                return TestStage.TestContinuationType.FAIL
            }
            if (orderMap[id] != count) {
                eventLogger.error(E_ADD_ITEMS_FAIL, id, count, orderMap[id])
                return TestStage.TestContinuationType.FAIL
            }
        }

        val finalNumOfItems = finalOrder.itemsMap.size
        if (finalNumOfItems != itemIds.size) {
            eventLogger.error(E_ITEMS_MISMATCH, finalNumOfItems, itemIds.size)
            return TestStage.TestContinuationType.FAIL

        }

        eventLogger.info(I_ORDER_COLLECTING_SUCCESS, itemIds.size, testCtx().orderId)
        return TestStage.TestContinuationType.CONTINUE
    }

}