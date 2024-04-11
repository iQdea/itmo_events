package com.itmo.microservices.shop.catalogue.service

import com.itmo.microservices.shop.catalogue.api.CatalogueAggregate
import com.itmo.microservices.shop.catalogue.api.CatalogueCreatedEvent
import com.itmo.microservices.shop.catalogue.api.ProductAddedEvent
import com.itmo.microservices.shop.catalogue.api.model.*
import com.itmo.microservices.shop.catalogue.logic.CatalogueState
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import ru.quipy.core.EventSourcingService

@Service
class CatalogueService(private val catalogueEsService: EventSourcingService<UUID, CatalogueAggregate, CatalogueState>) {
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    fun createCatalogueRequest(request: CreateCatalogueRequest): CatalogueResponseDto {
        val catalogue = createCatalogue(request)
        return CatalogueResponseDto(catalogue.catalogueId, catalogue.catalogueName)
    }

    fun createCatalogue(request: CreateCatalogueRequest): CatalogueCreatedEvent {
        val criteria = Criteria.where("catalogueName").`is`(request.name)
        val exists = mongoTemplate.exists(
            Query().addCriteria(criteria),
            CatalogueModel::class.java,
            "catalogues-cache"
        )
        if (exists) {
            throw IllegalStateException("Cannot create catalogue with name ${request.name}. Name is busy")
        }
        return catalogueEsService.create { catalog ->
            catalog.createNewCatalogue(name = request.name)
        }
    }

    fun addItem(request: CreateProductInCatalogueRequest): CatalogueItemDto {
        val item = createProductInCatalogue(request)
        return CatalogueItemDto(item.productId, item.catalogueId, item.title, item.description, item.price, item.amount)
    }

    fun createProductInCatalogue(request: CreateProductInCatalogueRequest): ProductAddedEvent {
        return catalogueEsService.update(request.catalogueId) { catalog ->
            catalog.createNewProduct(request.title, request.description, request.price, request.amount)
        }
    }

    fun getProducts(available: Boolean, size: Int, catalogueId: UUID): List<ListCatalogueItemDto> {
        var catalogueItems = catalogueEsService.getState(catalogueId)?.catalogueProducts?.toList()
            ?: throw IllegalStateException("No products in catalogue with id $catalogueId")
        if (available) {
            catalogueItems = catalogueItems.filter { it.second.amount >= 1 }
        }
        return catalogueItems.take(size).map { it.second }
            .map { ListCatalogueItemDto(it.productId, it.title, it.description, it.price, it.amount) }
    }
}