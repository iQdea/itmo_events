package com.itmo.microservices.shop.auth.api.model

data class AuthenticationResult(val accessToken: String, val refreshToken: String)
