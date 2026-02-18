package com.deepen.dto

import java.time.LocalDateTime

data class AvailabilityCheckRequest(
    val staffId: Long,
    val patientId: Long,
    val scheduledAt: LocalDateTime,
    val durationMinutes: Int
)

data class AvailabilityCheckResponse(
    val isAvailable: Boolean,
    val conflicts: List<String>,
    val alternativeTimes: List<AlternativeTimeSuggestion>
)

data class AlternativeTimeSuggestion(
    val scheduledAt: LocalDateTime,
    val reason: String,
    val isPreferred: Boolean,
    val confidence: Double
)
