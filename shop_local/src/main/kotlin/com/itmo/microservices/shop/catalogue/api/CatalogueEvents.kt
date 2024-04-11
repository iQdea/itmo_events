package com.itmo.microservices.shop.catalogue.api

import com.itmo.microservices.shop.order.api.model.BookingStatus
import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.util.*

const val CATALOGUE_CREATED = "CATALOGUE_CREATED"
const val PRODUCT_ADDED = "PRODUCT_ADDED"
const val PRODUCT_BOOKED = "PRODUCT_BOOKED"
const val PRODUCT_REMOVED = "PRODUCT_REMOVED"

@DomainEvent(name = CATALOGUE_CREATED)
data class CatalogueCreatedEvent(
    val catalogueId: UUID,
    val catalogueName: String
) : Event<CatalogueAggregate>(
    name = CATALOGUE_CREATED
)

@DomainEvent(name = PRODUCT_ADDED)
data class ProductAddedEvent(
    val productId: UUID,
    val catalogueId: UUID,
    val title: String,
    val description: String,
    val price: Int,
    val amount: Int
) : Event<CatalogueAggregate>(
    name = PRODUCT_ADDED
)

@DomainEvent(name = PRODUCT_BOOKED)
data class ProductBookedEvent(
    val bookingId: UUID,
    val productId: UUID,
    val orderId: UUID,
    val status: BookingStatus,
    val amount: Int,
    val timestamp: Long
    ) : Event<CatalogueAggregate>(
    name = PRODUCT_BOOKED
)