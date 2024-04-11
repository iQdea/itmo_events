package com.itmo.microservices.shop.payment.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "Payment")
class PaymentAggregate: Aggregate