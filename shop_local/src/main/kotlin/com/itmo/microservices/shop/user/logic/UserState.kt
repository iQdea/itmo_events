package com.itmo.microservices.shop.user.logic

import com.itmo.microservices.shop.user.api.UserAggregate
import com.itmo.microservices.shop.user.api.UserCreatedEvent
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.util.*

class UserState : AggregateState<UUID, UserAggregate> {
    private lateinit var userId: UUID

    override fun getId() = userId

    fun registerUser(id: UUID = UUID.randomUUID(), name: String, password: String): UserCreatedEvent {
        return UserCreatedEvent(id, name, password)
    }

    @StateTransitionFunc
    fun registerUser(event: UserCreatedEvent) {
        userId = event.userId
    }
}