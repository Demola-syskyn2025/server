package com.deepen.dto

import com.deepen.model.AppointmentType
import java.time.LocalDateTime

data class SuggestedAppointment(
    val patientId: Long,
    val patientName: String,
    val scheduledAt: LocalDateTime,
    val estimatedDurationMinutes: Int,
    val type: AppointmentType,
    val notes: String? = null,
    val location: String? = null,
    val reason: String? = null
)

data class UnscheduledPatient(
    val patientId: Long,
    val patientName: String,
    val reason: String,
    val lastVisitDate: LocalDateTime? = null,
    val recommendedFrequency: String? = null
)

data class ScheduleSuggestionResponse(
    val staffId: Long,
    val startDate: String,
    val endDate: String,
    val suggestions: List<SuggestedAppointment>,
    val alreadyScheduled: List<AppointmentDto>,
    val unscheduledPatients: List<UnscheduledPatient>
)

data class BatchCreateRequest(
    val appointments: List<CreateAppointmentRequest>
)

data class BatchCreateResponse(
    val totalCreated: Int,
    val totalErrors: Int,
    val created: List<AppointmentDto>,
    val errors: List<String>
)
