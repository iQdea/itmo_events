package com.itmo.microservices.shop.catalogue.controller

import com.itmo.microservices.shop.catalogue.api.model.*
import com.itmo.microservices.shop.catalogue.service.CatalogueService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping()
class CatalogueController(private val catalogueService: CatalogueService) {

    @PostMapping("/_internal/catalog")
    @Operation(
        summary = "Создание каталога",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun createCatalogue(@RequestBody request: CreateCatalogueRequest): CatalogueResponseDto = catalogueService.createCatalogueRequest(request)

    @PostMapping("/_internal/catalogItem")
    @Operation(
        summary = "Создание товара в каталоге",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun createItem(@RequestBody request: CreateProductInCatalogueRequest): CatalogueItemDto =
        catalogueService.addItem(request)

    @GetMapping("/items")
    @Operation(
        summary = "Получение товаров каталога",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getItems(
        @RequestParam available: Boolean,
        @RequestParam size: Int,
        @RequestParam catalogueId: UUID
    ): List<ListCatalogueItemDto> =
        catalogueService.getProducts(available, size, catalogueId)
}