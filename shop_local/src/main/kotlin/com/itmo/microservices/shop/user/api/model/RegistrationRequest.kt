package com.itmo.microservices.shop.user.api.model

data class RegistrationRequest(
        val name: String,
        val password: String
)