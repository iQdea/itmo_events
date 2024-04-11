package com.itmo.microservices.demo.bombardier.flow

import com.itmo.microservices.demo.bombardier.ServiceDescriptor
import com.itmo.microservices.demo.bombardier.exception.BadRequestException
import com.itmo.microservices.demo.bombardier.external.knownServices.KnownServices
import com.itmo.microservices.demo.bombardier.external.knownServices.ServiceWithApiAndAdditional
import com.itmo.microservices.demo.bombardier.stages.*
import com.itmo.microservices.demo.bombardier.stages.TestStage.TestContinuationType.CONTINUE
import com.itmo.microservices.demo.bombardier.stages.TestStage.TestContinuationType.STOP
import com.itmo.microservices.demo.common.logging.LoggerWrapper
import com.itmo.microservices.demo.common.metrics.Metrics
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

@Service
class TestController(
    private val knownServices: KnownServices,
    choosingUserAccountStage: ChoosingUserAccountStage,
    orderCreationStage: OrderCreationStage,
    orderCollectingStage: OrderCollectingStage,
    orderFinalizingStage: OrderFinalizingStage,
    orderSettingDeliverySlotsStage: OrderSettingDeliverySlotsStage,
    orderChangeItemsAfterFinalizationStage: OrderChangeItemsAfterFinalizationStage,
    orderPaymentStage: OrderPaymentStage,
    orderDeliveryStage: OrderDeliveryStage
) {
    companion object {
        val log = LoggerFactory.getLogger(TestController::class.java)
        val metrics = Metrics()
    }

    val runningTests = ConcurrentHashMap<String, TestingFlow>()

    val executor: ExecutorService = Executors.newFixedThreadPool(4)

    private val coroutineScope = CoroutineScope(executor.asCoroutineDispatcher())

    private val testStages = listOf(
        choosingUserAccountStage.asErrorFree().asMetricRecordable(),
        orderCreationStage.asErrorFree().asMetricRecordable(),
        orderCollectingStage.asErrorFree().asMetricRecordable(),
//        OrderAbandonedStage(serviceApi).asErrorFree(),
        orderFinalizingStage.asErrorFree().asMetricRecordable(),
        orderSettingDeliverySlotsStage.asErrorFree().asMetricRecordable(),
        orderChangeItemsAfterFinalizationStage.asErrorFree().asMetricRecordable(),
        orderFinalizingStage.asErrorFree().asMetricRecordable(),
        orderSettingDeliverySlotsStage.asErrorFree().asMetricRecordable(),
        orderPaymentStage.asRetryable().asErrorFree().asMetricRecordable(),
        orderDeliveryStage.asErrorFree().asMetricRecordable()
    )

    fun startTestingForService(params: TestParameters) {
        val logger = LoggerWrapper(log, params.serviceName)

        val testingFlowCoroutine = SupervisorJob()

        val v = runningTests.putIfAbsent(params.serviceName, TestingFlow(params, testingFlowCoroutine))
        if (v != null) {
            throw BadRequestException("There is no such feature launch several flows for the service in parallel :(")
        }

        val descriptor = knownServices.descriptorFromName(params.serviceName)
        val stuff = knownServices.getStuff(params.serviceName)

        runBlocking {
            stuff.userManagement.createUsersPool(params.numberOfUsers)
        }

        repeat(params.parallelProcessesNumber) {
            logger.info("Launch coroutine for $descriptor")
            launchNewTestFlow(descriptor, stuff)
        }
    }

    fun getTestingFlowForService(serviceName: String): TestingFlow {
        return runningTests[serviceName] ?: throw IllegalArgumentException("There is no running test for $serviceName")
    }

    suspend fun stopTestByServiceName(serviceName: String) {
        runningTests[serviceName]?.testFlowCoroutine?.cancelAndJoin()
            ?: throw BadRequestException("There is no running tests with serviceName = $serviceName")
        runningTests.remove(serviceName)
    }

    suspend fun stopAllTests() {
        runningTests.values.forEach {
            it.testFlowCoroutine.cancelAndJoin()
        }
        runningTests.clear()
    }

    class TestingFlow(
        val testParams: TestParameters,
        val testFlowCoroutine: CompletableJob,
        val testsStarted: AtomicInteger = AtomicInteger(1),
        val testsFinished: AtomicInteger = AtomicInteger(0)
    )

    private fun launchNewTestFlow(descriptor: ServiceDescriptor, stuff: ServiceWithApiAndAdditional) {
        val logger = LoggerWrapper(log, descriptor.name)

        val serviceName = descriptor.name
        val metrics = metrics.withTags(metrics.serviceLabel, serviceName)

        val testingFlow = runningTests[serviceName] ?: return

        if (testingFlow.testParams.numberOfTests != null && testingFlow.testsFinished.get() >= testingFlow.testParams.numberOfTests) {
            logger.info("Wrapping up test flow. Number of tests exceeded")
            runningTests.remove(serviceName)
            return
        }

        val testNum = testingFlow.testsStarted.getAndIncrement() // data race :(
        if (testingFlow.testParams.numberOfTests != null && testNum > testingFlow.testParams.numberOfTests) {
            logger.info("All tests Started. No new tests")
            return
        }

        logger.info("Starting $testNum test for service $serviceName, parent job is ${testingFlow.testFlowCoroutine}")

        coroutineScope.launch(testingFlow.testFlowCoroutine + TestContext(serviceName = serviceName)) {
            val testStartTime = System.currentTimeMillis()
            testStages.forEach { stage ->
                val stageResult = stage.run(stuff.userManagement, stuff.api)
                when {

                    stage.isFinal() && !stageResult.iSFailState() || stageResult == STOP -> {
                        metrics.testOkDurationRecord(System.currentTimeMillis() - testStartTime)
                        return@launch
                    }
                    stageResult.iSFailState() -> {
                        metrics.testFailDurationRecord(System.currentTimeMillis() - testStartTime)
                        return@launch
                    }
                    stageResult == CONTINUE -> Unit
                }
            }
        }.invokeOnCompletion { th ->
            if (th != null) {
                logger.error("Unexpected fail in test", th)
            }
            logger.info("Test ${testingFlow.testsFinished.incrementAndGet()} finished")
            launchNewTestFlow(descriptor, stuff)
        }
    }
}

object TestCtxKey : CoroutineContext.Key<TestContext>

data class TestContext(
    val testId: UUID = UUID.randomUUID(),
    val serviceName: String,
    var userId: UUID? = null,
    var orderId: UUID? = null,
    var catalogueId: UUID? = null,
    var deliveryId: UUID? = null,
    var paymentDetails: PaymentDetails = PaymentDetails(),
    var stagesComplete: MutableList<String> = mutableListOf(),
    var wasChangedAfterFinalization: Boolean = false,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<TestContext>
        get() = TestCtxKey

    fun finalizationNeeded() = OrderChangeItemsAfterFinalizationStage::class.java.simpleName !in stagesComplete
            || wasChangedAfterFinalization
}

data class PaymentDetails(
    var startedAt: Long? = null,
    var failedAt: Long? = null,
    var finishedAt: Long? = null,
    var attempt: Int = 0,
    var amount: Int? = null,
)

data class TestParameters(
    val serviceName: String,
    val numberOfUsers: Int,
    val parallelProcessesNumber: Int,
    val numberOfTests: Int? = null
)