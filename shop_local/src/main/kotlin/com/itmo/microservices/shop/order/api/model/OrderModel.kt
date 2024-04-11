package com.itmo.microservices.shop.order.api.model
import java.util.*

data class OrderResponseDto(
    val id: UUID,
    val timeCreated: Long,
    val status: OrderStatus,
    val itemsMap: MutableMap<UUID, OrderItemResponseDto> = mutableMapOf()
)

data class OrderModel(
    val orderId: UUID,
    val timestamp: Long,
    val status: OrderStatus,
    val orderProducts: MutableMap<UUID, OrderItemModel> = mutableMapOf()
)
data class OrderItemResponseDto(
    val id: UUID,
    val title: String,
    val amount: Int,
    val price: Int
)

enum class OrderStatus {
    COLLECTING,
    DISCARD,
    BOOKED,
    PAID,
    SHIPPING,
    REFUND,
    COMPLETED
}

enum class BookingStatus {
    FAILED,
    SUCCESS
}
data class OrderItemModel(
    val productId: UUID,
    val orderId: UUID,
    val title: String,
    val amount: Int,
    val price: Int
)

data class BookingLogModel(
    var bookingId: UUID,
    var orderId: UUID,
    var itemId: UUID,
    var status: BookingStatus,
    var amount: Int,
    val timestamp: Long
)

data class BookingLogRecord(
    var bookingId: UUID,
    var itemId: UUID,
    var status: BookingStatus,
    var amount: Int,
    val timestamp: Long
)

data class BookingDto(
    val id: UUID,
    val failedItems: Set<UUID>
)