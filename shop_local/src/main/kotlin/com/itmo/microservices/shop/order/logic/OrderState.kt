package com.itmo.microservices.shop.order.logic

import com.itmo.microservices.shop.order.api.*
import com.itmo.microservices.shop.order.api.model.OrderItemModel
import com.itmo.microservices.shop.order.api.model.OrderStatus
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.util.*

class OrderState : AggregateState<UUID, OrderAggregate>  {
    private lateinit var orderId: UUID
    var status: OrderStatus = OrderStatus.COLLECTING
    var orderProducts: MutableMap<UUID, OrderItemModel> = mutableMapOf()
    override fun getId() = orderId

    fun createNewOrder(id: UUID = UUID.randomUUID()): OrderCreatedEvent {
        return OrderCreatedEvent(id, timestamp = System.currentTimeMillis(), status = this.status)
    }

    fun putProductToOrder(
        orderId: UUID,
        productId: UUID,
        amount: Int,
        title: String,
        price: Int,
    ): ProductPutEvent {
        if (amount <= 0)
            throw IllegalStateException("Amount must be greater than zero")
        if (price < 0)
            throw IllegalStateException("Price must be greater or equal to zero")
        return ProductPutEvent(productId, orderId, amount, title, price)
    }

    fun bookOrder(bookingId: UUID, orderId: UUID, failedItems: Set<UUID>): OrderBookedEvent {
        if (failedItems.isNotEmpty()) {
            throw java.lang.IllegalStateException("Some items processing failed in order with id $orderId")
        }
        return OrderBookedEvent(orderId, bookingId, status = OrderStatus.BOOKED, failedItems)
    }

    fun paidOrder(orderId: UUID): OrderPaidEvent {
        return OrderPaidEvent(orderId, status = OrderStatus.PAID)
    }

    fun discardOrder(orderId: UUID): OrderDiscardEvent {
        return OrderDiscardEvent(orderId, status = OrderStatus.DISCARD)
    }

    fun shippingOrder(orderId: UUID): OrderShippingEvent {
        return OrderShippingEvent(orderId, status = OrderStatus.SHIPPING)
    }

    fun completedOrder(orderId: UUID): OrderCompletedEvent {
        return OrderCompletedEvent(orderId, status = OrderStatus.COMPLETED)
    }

    @StateTransitionFunc
    fun createNewOrder(event: OrderCreatedEvent) {
        orderId = event.orderId
    }

    @StateTransitionFunc
    fun putProductToOrder(event: ProductPutEvent) {
        orderProducts[event.productId] = OrderItemModel(event.productId, event.orderId, event.title, event.amount, event.price)
    }

    @StateTransitionFunc
    fun bookOrder(event: OrderBookedEvent) {
        status = event.status
    }

    @StateTransitionFunc
    fun paidOrder(event: OrderPaidEvent) {
        status = event.status
    }

    @StateTransitionFunc
    fun discardOrder(event: OrderDiscardEvent) {
        status = event.status
    }

    @StateTransitionFunc
    fun shippingOrder(event: OrderShippingEvent) {
        status = event.status
    }
    @StateTransitionFunc
    fun completedOrder(event: OrderCompletedEvent) {
        status = event.status
    }
}