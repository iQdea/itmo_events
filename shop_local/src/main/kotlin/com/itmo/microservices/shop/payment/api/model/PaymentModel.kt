package com.itmo.microservices.shop.payment.api.model

import java.util.*

enum class PaymentStatus {
    CREATED,
    FAILED,
    SUCCESS
}

data class PaymentModel(
    val paymentId: UUID,
    val transactionId: UUID,
    val user: String,
    val timestamp: Long,
    val status: PaymentStatus,
    val amount: Int
)

data class UserAccountFinancialLogRecordDto(
    val type: FinancialOperationType,
    val amount: Int,
    val orderId: UUID,
    val paymentTransactionId: UUID,
    val timestamp: Long
)

enum class FinancialOperationType {
    WITHDRAW,
    REFUND
}

data class PaymentSubmissionDto (
    val timestamp: Long,
    val transactionId: UUID
)