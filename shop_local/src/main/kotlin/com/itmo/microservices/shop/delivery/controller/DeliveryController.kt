package com.itmo.microservices.shop.delivery.controller

import com.itmo.microservices.shop.delivery.api.model.DeliveryInfoRecord
import com.itmo.microservices.shop.delivery.service.DeliveryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping
class DeliveryController(private val deliveryService: DeliveryService) {
    @GetMapping("/delivery/slots")
    @Operation(
        summary = "Получение возможных сейчас слотов доставки",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = "not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getSlots(@RequestParam number: Int): List<Instant> =
        deliveryService.generateSlots(number)

    @GetMapping("/_internal/delivery/{order_id}")
    @Operation(
        summary = "Получить историю доставки заказа по orderId",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = "not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getHistory(@PathVariable order_id: UUID): List<DeliveryInfoRecord> = deliveryService.getHistory(order_id)
}