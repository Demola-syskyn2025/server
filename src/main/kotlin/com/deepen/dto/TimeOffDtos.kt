package com.deepen.dto

import com.deepen.model.TimeOffStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateTimeOffRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String? = null
)

data class ReviewTimeOffRequest(
    val status: TimeOffStatus // APPROVED or REJECTED
)

data class TimeOffRequestDto(
    val id: Long,
    val staff: UserDto,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?,
    val status: TimeOffStatus,
    val requestedAt: LocalDateTime,
    val reviewedAt: LocalDateTime?,
    val reviewedBy: UserDto?
)
