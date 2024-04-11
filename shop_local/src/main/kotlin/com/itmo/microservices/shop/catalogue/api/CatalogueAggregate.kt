package com.itmo.microservices.shop.catalogue.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "Catalogue")
class CatalogueAggregate: Aggregate