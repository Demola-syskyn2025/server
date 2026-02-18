package com.deepen.dto

import java.time.LocalDateTime

data class AppointmentCreationResponse(
    val success: Boolean,
    val appointment: AppointmentDto? = null,
    val conflicts: List<String> = emptyList(),
    val alternativeTimes: List<AlternativeTimeSuggestion> = emptyList(),
    val message: String? = null
)

data class BatchCreationWithConflictsResponse(
    val totalRequested: Int,
    val totalCreated: Int,
    val totalConflicts: Int,
    val created: List<AppointmentDto>,
    val conflicts: List<AppointmentConflict>,
    val message: String
)

data class AppointmentConflict(
    val index: Int,
    val patientId: Long,
    val patientName: String,
    val requestedTime: LocalDateTime,
    val conflicts: List<String>,
    val alternativeTimes: List<AlternativeTimeSuggestion>
)
