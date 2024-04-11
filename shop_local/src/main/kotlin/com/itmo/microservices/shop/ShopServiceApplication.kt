package com.itmo.microservices.shop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication
@EnableMongoRepositories
class ShopServiceApplication

fun main(args: Array<String>) {
    runApplication<ShopServiceApplication>(*args)
}