package com.itmo.microservices.shop.order.service

import com.itmo.microservices.shop.catalogue.api.CatalogueAggregate
import com.itmo.microservices.shop.catalogue.api.ProductBookedEvent
import com.itmo.microservices.shop.catalogue.api.model.CatalogueItemModel
import com.itmo.microservices.shop.catalogue.logic.CatalogueState
import com.itmo.microservices.shop.common.exception.NotFoundException
import com.itmo.microservices.shop.order.api.OrderAggregate
import com.itmo.microservices.shop.order.api.OrderBookedEvent
import com.itmo.microservices.shop.order.api.OrderCreatedEvent
import com.itmo.microservices.shop.order.api.ProductPutEvent
import com.itmo.microservices.shop.order.api.model.*
import com.itmo.microservices.shop.order.logic.OrderState
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import ru.quipy.core.EventSourcingService

@Service
class OrderService(
    private val ordersEsService: EventSourcingService<UUID, OrderAggregate, OrderState>,
    private val catalogueEsService: EventSourcingService<UUID, CatalogueAggregate, CatalogueState>
) {
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    fun createOrder(): OrderCreatedEvent {
        return ordersEsService.create { order ->
            order.createNewOrder()
        }
    }

    fun createOrderRequest(): OrderResponseDto {
        val order = createOrder()
        val itemsMap: MutableMap<UUID, OrderItemResponseDto> = mutableMapOf()
        return OrderResponseDto(order.orderId, order.timestamp, order.status, itemsMap)
    }

    fun putItem(orderId: UUID, itemId: UUID, amount: Int) {
        addProductToOrder(orderId, itemId, amount)
    }
    fun addProductToOrder(orderId: UUID, itemId: UUID, amount: Int): ProductPutEvent {
        val catalogueProduct = mongoTemplate.find(
            Query.query(Criteria.where("productId").`is`(itemId)),
            CatalogueItemModel::class.java,
            "products-in-catalogues-cache"
        ).first()
        return ordersEsService.update(orderId) { order ->
            order.putProductToOrder(orderId, itemId, amount, catalogueProduct.title, catalogueProduct.price)
        }
    }

    fun getOrderRequest(orderId: UUID): OrderResponseDto {
        val order = getOrder(orderId)
        val itemsMap: MutableMap<UUID, OrderItemResponseDto> = mutableMapOf()
        if (order.orderProducts.isNotEmpty()) {
            for (orderItem in order.orderProducts) {
                val itemData = orderItem.value
                itemsMap[orderId] = OrderItemResponseDto(itemData.productId, itemData.title, itemData.amount, itemData.price)
            }
        }
        return OrderResponseDto(orderId, order.timestamp, order.status, itemsMap)
    }
    fun getOrder(orderId: UUID): OrderModel {
        val order = mongoTemplate.find(
            Query.query(Criteria.where("orderId").`is`(orderId)),
            OrderModel::class.java,
            "orders-cache"
        )
        if (order.size == 0) {
            throw NotFoundException("Order with id $orderId not found")
        }
        return order.first()
    }

    fun processBooking(orderId: UUID): BookingDto {
        val booking = bookOrder(orderId)
        return BookingDto(booking.bookingId, booking.failedItems)
    }
    fun bookOrder(orderId: UUID): OrderBookedEvent {
        val bookingId = UUID.randomUUID()
        val timestamp = System.currentTimeMillis()
        val failedItems = bookItems(bookingId, orderId, timestamp)
        return ordersEsService.update(orderId) {
            it.bookOrder(bookingId, orderId, failedItems)
        }
    }

    fun bookItems(bookingId: UUID, orderId: UUID, timestamp: Long): MutableSet<UUID> {
        val failedItems: MutableSet<UUID> = mutableSetOf()
        val order = ordersEsService.getState(orderId)
        if (order != null) {
            val orderItems = order.orderProducts.toList().map { it.second }
            for (item in orderItems) {
                val book = bookItem(bookingId, item.productId, orderId, item.amount, timestamp)
                if (book.status == BookingStatus.FAILED) {
                    failedItems.add(item.productId)
                }
            }
        }
        return failedItems
    }

    fun bookItem(
        bookingId: UUID,
        productId: UUID,
        orderId: UUID,
        productAmount: Int,
        timestamp: Long
    ): ProductBookedEvent {
        var status: BookingStatus = BookingStatus.FAILED
        val catalogueProduct = mongoTemplate.find(
            Query.query(Criteria.where("productId").`is`(productId)),
            CatalogueItemModel::class.java,
            "products-in-catalogues-cache"
        )
        if (catalogueProduct.size == 1) {
            status = BookingStatus.SUCCESS
        }
        return catalogueEsService.update(catalogueProduct.first().catalogueId) { catalogue ->
            catalogue.bookProduct(bookingId, productId, orderId, status, productAmount, timestamp)
        }
    }

    fun getBookingHistory(bookingId: UUID): List<BookingLogRecord> {
        val bookings = mongoTemplate.find(
            Query.query(Criteria.where("bookingId").`is`(bookingId)),
            BookingLogModel::class.java,
            "bookings-logs-cache"
        )
        return bookings.map { BookingLogRecord(it.bookingId, it.itemId, it.status, it.amount, it.timestamp) }
    }
}