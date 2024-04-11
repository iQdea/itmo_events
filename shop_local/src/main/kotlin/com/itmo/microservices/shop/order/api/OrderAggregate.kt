package com.itmo.microservices.shop.order.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "Order")
class OrderAggregate: Aggregate