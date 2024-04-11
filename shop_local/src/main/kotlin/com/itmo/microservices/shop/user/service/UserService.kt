package com.itmo.microservices.shop.user.service

import com.itmo.microservices.shop.common.exception.NotFoundException
import com.itmo.microservices.shop.user.api.UserAggregate
import com.itmo.microservices.shop.user.api.UserCreatedEvent
import com.itmo.microservices.shop.user.api.model.AppUserModel
import com.itmo.microservices.shop.user.api.model.RegistrationRequest
import com.itmo.microservices.shop.user.api.model.UserResponseDto
import com.itmo.microservices.shop.user.logic.UserState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import ru.quipy.core.EventSourcingService
import java.util.*

@Service
class UserService(
    private val userEsService: EventSourcingService<UUID, UserAggregate, UserState>,
    private val passwordEncoder: PasswordEncoder
) {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    fun findUserByName(name: String): AppUserModel? {
        val criteria = Criteria.where("name").`is`(name)
        return mongoTemplate.findOne(
            Query().addCriteria(criteria),
            AppUserModel::class.java,
            "users-cache"
        )
    }

    fun findUserById(id: UUID): AppUserModel? {
        val criteria = Criteria.where("_id").`is`(id)
        return mongoTemplate.findOne(
            Query().addCriteria(criteria),
            AppUserModel::class.java,
            "users-cache"
        )
    }

    fun getAccountDataByName(requester: UserDetails): UserResponseDto {
        val account = findUserByName(requester.username)
            ?: throw NotFoundException("User with username ${requester.username} not found")
        return UserResponseDto(account.id, account.name)
    }

    fun getAccountDataById(id: UUID): UserResponseDto {
        val account = findUserById(id)
            ?: throw NotFoundException("User with id $id not found")
        return UserResponseDto(account.id, account.name)
    }

    fun registerRequest(request: RegistrationRequest): UserResponseDto {
        val user = registerUser(request)
        return UserResponseDto(user.userId, user.userName)
    }
    fun registerUser(request: RegistrationRequest): UserCreatedEvent {
        val criteria = Criteria.where("name").`is`(request.name)
        val exists = mongoTemplate.exists(
            Query().addCriteria(criteria),
            AppUserModel::class.java,
            "users-cache"
        )
        if (exists) {
            throw IllegalStateException("Cannot create user with name ${request.name}. Name is busy")
        }
        return userEsService.create { user ->
            user.registerUser(name = request.name, password = passwordEncoder.encode(request.password))
        }
    }
}
