package com.itmo.microservices.shop.catalogue.api.model
import java.util.*

data class CatalogueModel(
    val catalogueId: UUID,
    val catalogueName: String
)
data class CatalogueResponseDto(
    val id: UUID,
    val name: String
)
data class CatalogueItemModel(
    val productId: UUID,
    val catalogueId: UUID,
    val title: String,
    val description: String,
    val price: Int,
    var amount: Int
) {
    fun putFrom(amount: Int) {
        this.amount = this.amount.minus(amount)
    }
}

data class ListCatalogueItemDto (
    val id: UUID,
    val title: String,
    val description: String,
    val price: Int,
    val amount: Int)
data class CatalogueItemDto (
    val id: UUID,
    val catalogueId: UUID,
    val title: String,
    val description: String,
    val price: Int,
    val amount: Int)