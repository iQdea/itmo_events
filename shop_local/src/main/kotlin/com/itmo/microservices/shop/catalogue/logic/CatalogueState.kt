package com.itmo.microservices.shop.catalogue.logic

import com.itmo.microservices.shop.catalogue.api.CatalogueAggregate
import com.itmo.microservices.shop.catalogue.api.CatalogueCreatedEvent
import com.itmo.microservices.shop.catalogue.api.ProductAddedEvent
import com.itmo.microservices.shop.catalogue.api.ProductBookedEvent
import com.itmo.microservices.shop.catalogue.api.model.CatalogueItemModel
import com.itmo.microservices.shop.order.api.model.BookingStatus
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.util.*

class CatalogueState : AggregateState<UUID, CatalogueAggregate> {
    private lateinit var catalogueId: UUID
    var catalogueProducts: MutableMap<UUID, CatalogueItemModel> = mutableMapOf()

    override fun getId() = catalogueId

    fun createNewCatalogue(id: UUID = UUID.randomUUID(), name: String): CatalogueCreatedEvent {
        return CatalogueCreatedEvent(id, name)
    }

    fun createNewProduct(
        title: String,
        description: String,
        price: Int,
        amount: Int
    ): ProductAddedEvent {
        if (amount < 0)
            throw IllegalStateException("Amount must be greater than zero or equal")
        if (price <= 0)
            throw IllegalStateException("Price must be greater than zero")
        return ProductAddedEvent(UUID.randomUUID(), catalogueId = this.getId(), title, description, price, amount)
    }

    fun bookProduct(
        bookingId: UUID,
        productId: UUID,
        orderId: UUID,
        status: BookingStatus,
        amount: Int,
        timestamp: Long
    ): ProductBookedEvent {
        if (amount <= 0)
            throw IllegalStateException("Amount must be greater than zero")
        val product = (catalogueProducts[productId]
            ?: throw IllegalArgumentException("No such product: $productId"))
        if (amount > product.amount) {
            throw IllegalStateException("You can't get more than ${product.amount}")
        }
        return ProductBookedEvent(bookingId, productId, orderId, status, amount, timestamp)
    }

    @StateTransitionFunc
    fun createNewCatalogue(event: CatalogueCreatedEvent) {
        catalogueId = event.catalogueId
    }

    @StateTransitionFunc
    fun createNewProduct(event: ProductAddedEvent) {
        catalogueProducts[event.productId] = CatalogueItemModel(
            event.productId,
            event.catalogueId,
            event.title,
            event.description,
            event.price,
            event.amount
        )
    }

    @StateTransitionFunc
    fun bookProduct(event: ProductBookedEvent) {
        catalogueProducts[event.productId]!!.putFrom(event.amount)
    }
}