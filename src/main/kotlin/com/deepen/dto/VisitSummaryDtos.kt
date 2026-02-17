package com.deepen.dto

import java.time.LocalDateTime

data class CreateVisitSummaryRequest(
    val appointmentId: Long,
    val summary: String,
    val recommendations: String? = null,
    val medications: String? = null,
    val nextVisitRecommendation: LocalDateTime? = null
)

data class UpdateVisitSummaryRequest(
    val summary: String? = null,
    val recommendations: String? = null,
    val medications: String? = null,
    val nextVisitRecommendation: LocalDateTime? = null
)

data class VisitSummaryDto(
    val id: Long,
    val appointmentId: Long,
    val patientName: String,
    val staffName: String,
    val summary: String,
    val recommendations: String?,
    val medications: String?,
    val nextVisitRecommendation: LocalDateTime?,
    val createdBy: UserDto,
    val createdAt: LocalDateTime
)
