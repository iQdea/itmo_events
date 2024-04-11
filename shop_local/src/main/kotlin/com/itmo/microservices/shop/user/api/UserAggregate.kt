package com.itmo.microservices.shop.user.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "User")
class UserAggregate: Aggregate