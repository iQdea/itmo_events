package com.itmo.microservices.shop.order.api

import com.itmo.microservices.shop.order.api.model.OrderStatus
import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.util.*

const val ORDER_CREATED = "ORDER_CREATED"
const val ORDER_BOOKED = "ORDER_BOOKED"
const val ORDER_PAID = "ORDER_PAID"
const val ORDER_DISCARD = "ORDER_DISCARD"
const val ORDER_SHIPPING = "ORDER_SHIPPING"
const val ORDER_COMPLETED = "ORDER_COMPLETED"
const val PRODUCT_PUT = "PRODUCT_PUT"

@DomainEvent( name = ORDER_CREATED)
data class OrderCreatedEvent(
    val orderId: UUID,
    val timestamp: Long,
    val status: OrderStatus
) : Event<OrderAggregate> (
    name = ORDER_CREATED
)

@DomainEvent( name = ORDER_BOOKED)
data class OrderBookedEvent(
    val orderId: UUID,
    val bookingId: UUID,
    val status: OrderStatus,
    val failedItems: Set<UUID>
) : Event<OrderAggregate> (
    name = ORDER_BOOKED
)

@DomainEvent( name = ORDER_PAID)
data class OrderPaidEvent(
    val orderId: UUID,
    val status: OrderStatus
) : Event<OrderAggregate> (
    name = ORDER_PAID
)

@DomainEvent( name = ORDER_DISCARD)
data class OrderDiscardEvent(
    val orderId: UUID,
    val status: OrderStatus
) : Event<OrderAggregate> (
    name = ORDER_DISCARD
)

@DomainEvent( name = ORDER_SHIPPING)
data class OrderShippingEvent(
    val orderId: UUID,
    val status: OrderStatus
) : Event<OrderAggregate> (
    name = ORDER_SHIPPING
)

@DomainEvent( name = ORDER_COMPLETED)
data class OrderCompletedEvent(
    val orderId: UUID,
    val status: OrderStatus
) : Event<OrderAggregate> (
    name = ORDER_COMPLETED
)

@DomainEvent( name = PRODUCT_PUT)
data class ProductPutEvent(
    val productId: UUID,
    val orderId: UUID,
    val amount: Int,
    val title: String,
    val price: Int
) : Event<OrderAggregate> (
    name = PRODUCT_PUT
)