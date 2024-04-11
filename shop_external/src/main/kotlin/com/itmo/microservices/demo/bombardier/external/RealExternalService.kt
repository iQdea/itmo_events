package com.itmo.microservices.demo.bombardier.external

import com.itmo.microservices.demo.bombardier.BombardierProperties
import com.itmo.microservices.demo.bombardier.ServiceDescriptor
import com.itmo.microservices.demo.bombardier.external.communicator.ExternalServiceToken
import com.itmo.microservices.demo.bombardier.external.communicator.InvalidExternalServiceResponseException
import com.itmo.microservices.demo.bombardier.external.communicator.UserAwareExternalServiceApiCommunicator
import com.itmo.microservices.demo.bombardier.external.storage.UserStorage
import com.itmo.microservices.demo.bombardier.flow.UserManagement
import com.itmo.microservices.demo.common.logging.LoggerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.time.Duration
import java.util.*
import java.util.concurrent.ForkJoinPool

class UserNotAuthenticatedException(username: String) : Exception(username)

class RealExternalService(override val descriptor: ServiceDescriptor, private val userStorage: UserStorage, props: BombardierProperties) : ExternalServiceApi {
    private val executorService = ForkJoinPool()
    private val communicator = UserAwareExternalServiceApiCommunicator(descriptor, executorService, props)
    val log = LoggerWrapper(
        LoggerFactory.getLogger(UserManagement::class.java),
        descriptor.name
    )
    suspend fun getUserSession(id: UUID): ExternalServiceToken {
        val username = getUser(id).name

        return communicator.getUserSession(username) ?: throw UserNotAuthenticatedException(username)
    }

    override suspend fun getUser(id: UUID): User {
        return userStorage.get(id)
    }

    override suspend fun createUser(name: String): User {
        val user = communicator.executeWithDeserialize<User>(
            "createUser",
            "/users",
        ) {
            jsonPost(
                "name" to name,
                "password" to "pwd_$name"
            )
        }
        withContext(Dispatchers.IO) {
            Thread.sleep(5000)
        }
        userStorage.create(user)

        communicator.authenticate(name, "pwd_$name")

        return user
    }

    override suspend fun userFinancialHistory(userId: UUID, orderId: UUID?): List<UserAccountFinancialLogRecord> {
        val session = getUserSession(userId)
        val url = if (orderId != null) "/finlog?orderId=$orderId" else "/finlog"

        return communicator.executeWithAuthAndDeserialize("userFinancialHistory",url, session)
    }

    override suspend fun createOrder(userId: UUID): Order {
        val session = getUserSession(userId)

        return communicator.executeWithAuthAndDeserialize("createOrder", "/orders", session) {
            post()
        }
    }

    override suspend fun getOrder(userId: UUID, orderId: UUID): Order {
        val session = getUserSession(userId)

        return communicator.executeWithAuthAndDeserialize("getOrder","/orders/$orderId", session)
    }

    override suspend fun createCatalogue(userId: UUID): ExternalServiceApi.Catalogue {
        val session = getUserSession(userId)
        return communicator.executeWithAuthAndDeserialize("createCatalogue", "/_internal/catalog", session) {
            post()
        }
    }

    override suspend fun createItemInCatalogue(userId: UUID, catalogueId: UUID) {
        val session = getUserSession(userId)
        communicator.executeWithAuth("createItemInCatalogue", "/_internal/catalogItem?isTest=true&catalogueId=$catalogueId", session) {
            post()
        }
    }

    override suspend fun confirmDelivery(deliveryId: UUID, userId: UUID) {
        val session = getUserSession(userId)
        communicator.executeWithAuth("confirmDelivery", "/orders/delivery/$deliveryId/confirm", session){
            post()
        }
    }
    override suspend fun getItems(catalogueId: UUID, userId: UUID, available: Boolean): List<CatalogItem> {
        val session = getUserSession(userId)

        return communicator.executeWithAuthAndDeserialize("getItems","/items?available=$available&size=150&catalogueId=$catalogueId", session)
    }

    override suspend fun putItemToOrder(userId: UUID, orderId: UUID, itemId: UUID, amount: Int): Boolean {
        val session = getUserSession(userId)

        val okCode = HttpStatus.OK.value()
        val badCode = HttpStatus.BAD_REQUEST.value()

        val code = try {
            communicator.executeWithAuth("putItemToOrder", "/orders/$orderId/items/$itemId?amount=$amount", session) {
                put()
            }
            withContext(Dispatchers.IO) {
                Thread.sleep(5000)
            }
        }
        catch (e: InvalidExternalServiceResponseException) {
            if (e.code != badCode) {
                throw e
            }
            badCode
        }

        return code == okCode
    }

    override suspend fun bookOrder(userId: UUID, orderId: UUID): BookingDto {
        val session = getUserSession(userId)

        return communicator.executeWithAuthAndDeserialize("bookOrder", "/orders/$orderId/bookings", session) {
            post()
        }
    }

    override suspend fun getDeliverySlots(userId: UUID, number: Int): List<Duration> {
        val session = getUserSession(userId)

        return communicator.executeWithAuthAndDeserialize("getDeliverySlots", "/delivery/slots?number=$number", session)
    }

    override suspend fun setDeliveryTime(userId: UUID, orderId: UUID, slot: Duration): DeliveryInfoRecord {
        val session = getUserSession(userId)
        val address = UUID.randomUUID().toString()
        return communicator.executeWithAuthAndDeserialize("setDeliveryTime", "/orders/$orderId/delivery?slot=${slot.seconds}&address=$address", session) {
            post()
        }
    }

    override suspend fun payOrder(userId: UUID, orderId: UUID): PaymentSubmissionDto {
        val session = getUserSession(userId)

        return communicator.executeWithAuthAndDeserialize("payOrder", "/orders/$orderId/payment", session) {
            post()
        }
    }

    override suspend fun simulateDelivery(userId: UUID, orderId: UUID) {
    }

    override suspend fun abandonedCardHistory(orderId: UUID): List<AbandonedCardLogRecord> {
        TODO("Not yet implemented")
    }

    override suspend fun getBookingHistory(userId: UUID, bookingId: UUID): List<BookingLogRecord> {
        val session = getUserSession(userId)

        return communicator.executeWithAuthAndDeserialize("getBookingHistory","/_internal/bookingHistory/$bookingId", session)
    }

    override suspend fun deliveryLog(userId: UUID, orderId: UUID): List<DeliveryInfoRecord> {
        val session = getUserSession(userId)

        return communicator.executeWithAuthAndDeserialize("deliveryLog","/_internal/delivery/$orderId", session)
    }
}