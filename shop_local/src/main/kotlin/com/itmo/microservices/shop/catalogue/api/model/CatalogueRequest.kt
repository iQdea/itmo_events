package com.itmo.microservices.shop.catalogue.api.model

import java.util.UUID

data class CreateCatalogueRequest(
    val name: String
)

data class CreateProductInCatalogueRequest(
    val catalogueId: UUID,
    val title: String,
    val description: String,
    val price: Int,
    val amount: Int
)