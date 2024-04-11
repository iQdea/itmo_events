package com.itmo.microservices.shop.payment.controller

import com.itmo.microservices.shop.payment.api.model.UserAccountFinancialLogRecordDto
import com.itmo.microservices.shop.payment.service.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping
class PaymentController(private val paymentService: PaymentService) {

    @GetMapping("/finlog")
    @Operation(
        summary = "Получение информации о финансовых операциях с аккаунтом пользователя",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getItems(
        @RequestParam order_id: UUID,
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails
    ): List<UserAccountFinancialLogRecordDto> =
        paymentService.getFinLog(order_id, user)
}