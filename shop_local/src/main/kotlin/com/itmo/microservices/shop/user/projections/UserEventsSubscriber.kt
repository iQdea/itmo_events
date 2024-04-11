package com.itmo.microservices.shop.user.projections


import com.itmo.microservices.shop.user.api.UserAggregate
import com.itmo.microservices.shop.user.api.UserCreatedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.UUID
import javax.annotation.PostConstruct
import javax.persistence.Id

@Service
class UserEventsSubscriber (
    private val userCacheRepository: UserCacheRepository,
    private val subscriptionsManager: AggregateSubscriptionsManager
) {
    private val logger: Logger = LoggerFactory.getLogger(UserEventsSubscriber::class.java)

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(UserAggregate::class, "users::cache") {
            `when`(UserCreatedEvent::class) { event ->
                withContext(Dispatchers.IO) {
                    userCacheRepository.save(User(event.userId, event.userName, event.password))
                }
                logger.info("Update users cache, register user ${event.userId}")
            }
        }
    }
}

@Document("users-cache")
data class User(
    @Id
    var id: UUID = UUID.randomUUID(),
    var name: String? = null,
    var password: String? = null
)


@Repository
interface UserCacheRepository : MongoRepository<User, UUID>
