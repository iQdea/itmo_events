package com.itmo.microservices.shop.delivery.api.model

import java.time.Instant
import java.util.*

enum class DeliverySubmissionOutcome {
    CREATED,
    SUCCESS,
    FAILURE,
    EXPIRED
}

data class DeliveryModel(
    val deliveryId: UUID,
    val orderId: UUID,
    val attempts: Int,
    val preparedTime: Long,
    val submittedTime: Long,
    val submissionStartedTime: Instant,
    val outcome: DeliverySubmissionOutcome
)

data class DeliveryInfoRecord(
    val outcome: DeliverySubmissionOutcome,
    val preparedTime: Long,
    val attempts: Int,
    val submittedTime: Long,
    val orderId: UUID,
    val submissionStartedTime: Instant
)

data class DeliveryStatusDto(
    val id: UUID,
    val outcome: DeliverySubmissionOutcome
)