package com.itmo.microservices.shop.payment.transactions.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "transactions")
class TransactionsAggregate: Aggregate