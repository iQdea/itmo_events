package com.itmo.microservices.demo.bombardier.stages

import com.itmo.microservices.commonlib.annotations.InjectEventLogger
import com.itmo.microservices.commonlib.logging.EventLogger
import com.itmo.microservices.demo.bombardier.external.ExternalServiceApi
import com.itmo.microservices.demo.bombardier.flow.UserManagement
import com.itmo.microservices.demo.bombardier.logging.OrderSettingsDeliveryNotableEvents.*
import com.itmo.microservices.demo.common.logging.EventLoggerWrapper
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.random.Random

@Component
class OrderSettingDeliverySlotsStage : TestStage {
    @InjectEventLogger
    lateinit var eventLog: EventLogger

    lateinit var eventLogger: EventLoggerWrapper

    override suspend fun run(
        userManagement: UserManagement,
        externalServiceApi: ExternalServiceApi
    ): TestStage.TestContinuationType {
        eventLogger = EventLoggerWrapper(eventLog, testCtx().serviceName)

        if (!testCtx().finalizationNeeded()) {
            eventLogger.info(I_SKIP_SETTING_SLOT, testCtx().orderId)
            return TestStage.TestContinuationType.CONTINUE
        }

        eventLogger.info(I_CHOOSE_SLOT, testCtx().orderId)

        val availableSlots = externalServiceApi.getDeliverySlots(
            testCtx().userId!!,
            10
        ) // TODO: might be a better idea to provide different number here

        var deliverySlot = Duration.ZERO
        repeat(Random.nextInt(1, 4)) {
            deliverySlot = availableSlots.random()
            if (deliverySlot > Duration.ofSeconds(30)) {
                deliverySlot = Duration.ofSeconds(Random.nextLong(1, 30))
            }
            val delivery = externalServiceApi.setDeliveryTime(testCtx().userId!!, testCtx().orderId!!, deliverySlot)
            testCtx().deliveryId = delivery.deliveryId
            Thread.sleep(5000)

            val resultSlot = externalServiceApi.getOrder(testCtx().userId!!, testCtx().orderId!!).deliveryDuration
            Thread.sleep(5000)
            if (resultSlot != deliverySlot) {
                eventLogger.error(E_CHOOSE_SLOT_FAIL, deliverySlot, resultSlot)
                return TestStage.TestContinuationType.FAIL
            }
        }

        eventLogger.info(I_CHOOSE_SLOT_SUCCESS, deliverySlot.seconds, testCtx().orderId)
        return TestStage.TestContinuationType.CONTINUE
    }
}