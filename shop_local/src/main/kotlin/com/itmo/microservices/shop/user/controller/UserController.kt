package com.itmo.microservices.shop.user.controller

import com.itmo.microservices.shop.user.api.model.RegistrationRequest
import com.itmo.microservices.shop.user.api.model.UserResponseDto
import com.itmo.microservices.shop.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {

    @PostMapping
    @Operation(
        summary = "Создание пользователя",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = "Bad request", responseCode = "400", content = [Content()])
        ]
    )
    fun register(@RequestBody request: RegistrationRequest): UserResponseDto = userService.registerRequest(request)

    @GetMapping("/me")
    @Operation(
        summary = "Получение текущего пользователя",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = "User not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getAccountData(@Parameter(hidden = true) @AuthenticationPrincipal user: UserDetails): UserResponseDto =
            userService.getAccountDataByName(user)

    @GetMapping("/{user_id}")
    @Operation(
        summary = "Получение пользователя",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = " not found", responseCode = "404", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getUser(@PathVariable user_id: UUID): UserResponseDto =
        userService.getAccountDataById(user_id)
}