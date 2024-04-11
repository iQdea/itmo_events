package com.itmo.microservices.shop.delivery.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "Delivery")
class DeliveryAggregate: Aggregate