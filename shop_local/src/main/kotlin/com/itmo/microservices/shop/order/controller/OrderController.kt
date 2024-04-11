package com.itmo.microservices.shop.order.controller

import com.itmo.microservices.shop.delivery.api.model.DeliveryInfoRecord
import com.itmo.microservices.shop.delivery.api.model.DeliveryStatusDto
import com.itmo.microservices.shop.delivery.service.DeliveryService
import com.itmo.microservices.shop.order.api.model.BookingDto
import com.itmo.microservices.shop.order.api.model.OrderResponseDto
import com.itmo.microservices.shop.order.api.model.BookingLogRecord
import com.itmo.microservices.shop.order.service.OrderService
import com.itmo.microservices.shop.payment.api.model.PaymentSubmissionDto
import com.itmo.microservices.shop.payment.service.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping()
class OrderController(
    private val orderService: OrderService,
    private val paymentService: PaymentService,
    private val deliveryService: DeliveryService
) {

    @PostMapping("/orders")
    @Operation(
        summary = "Создание нового заказа",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun createCatalogue(): OrderResponseDto = orderService.createOrderRequest()

    @PostMapping("/orders/{order_id}/items/{item_id}")
    @Operation(
        summary = "Помещение товара в корзину",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun createItem(@PathVariable order_id: UUID, @PathVariable item_id: UUID, @RequestParam amount: Int): Unit =
        orderService.putItem(order_id, item_id, amount)


    @PostMapping("/orders/{order_id}/payment")
    @Operation(
        summary = "Оплата заказа",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = "not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun pay(@PathVariable order_id: UUID, @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails): PaymentSubmissionDto =
        paymentService.processPayment(order_id, user)


    @PostMapping("/orders/{order_id}/bookings")
    @Operation(
        summary = "Оформление (финализация/бронирование) заказа",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun bookOrder(@PathVariable order_id: UUID): BookingDto = orderService.processBooking(order_id)

    @PostMapping("/orders/{order_id}/delivery")
    @Operation(
        summary = "Установление желаемого времени доставки",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun createDelivery(@PathVariable order_id: UUID, @RequestParam slot: Instant): DeliveryInfoRecord =
        deliveryService.processDelivery(orderId = order_id, time = slot)

    @PostMapping("/orders/delivery/{delivery_id}/confirm")
    @Operation(
        summary = "Подтвердить успешную доставку",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun confirmDelivery(@PathVariable delivery_id: UUID): DeliveryStatusDto =
        deliveryService.confirmOrderDelivery(delivery_id)

    @GetMapping("/orders/{order_id}")
    @Operation(
        summary = "Получение заказа",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getItems(@PathVariable order_id: UUID): OrderResponseDto = orderService.getOrderRequest(order_id)

    @GetMapping("/_internal/bookingHistory/{bookingId}")
    @Operation(
        summary = "Получить список забронированных товаров по bookingId",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getBookingHistory(@PathVariable bookingId: UUID): List<BookingLogRecord> = orderService.getBookingHistory(bookingId)
}