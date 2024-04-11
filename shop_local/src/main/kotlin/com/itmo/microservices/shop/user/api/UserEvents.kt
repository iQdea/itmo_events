package com.itmo.microservices.shop.user.api

import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.util.*

const val USER_CREATED = "USER_CREATED"

@DomainEvent( name = USER_CREATED)
data class UserCreatedEvent(
    val userId: UUID,
    val userName: String,
    val password: String
) : Event<UserAggregate> (
    name = USER_CREATED
)