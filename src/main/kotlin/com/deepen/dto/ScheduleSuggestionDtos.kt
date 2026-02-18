package com.deepen.dto

import com.deepen.model.AppointmentType
import com.deepen.model.RecurringFrequency
import java.time.LocalDate
import java.time.LocalDateTime

// Request: "suggest a schedule for me for this period"
data class ScheduleSuggestionRequest(
    val startDate: LocalDate,
    val endDate: LocalDate
)

// Each suggested appointment slot
data class SuggestedAppointment(
    val patientId: Long,
    val patientName: String,
    val scheduledAt: LocalDateTime,
    val estimatedDurationMinutes: Int,
    val type: AppointmentType,
    val notes: String?,
    val location: String?,
    val reason: String, // why this was suggested (e.g. "Weekly wound care — recurring")
    val isFromRecurring: Boolean,
    val recurringGroupId: String? = null
)

// Full response
data class ScheduleSuggestionResponse(
    val staffId: Long,
    val staffName: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val suggestions: List<SuggestedAppointment>,
    val alreadyScheduled: List<AppointmentDto>, // existing appointments in this period
    val unscheduledPatients: List<UnscheduledPatient> // patients who need a visit but couldn't be auto-slotted
)

data class UnscheduledPatient(
    val patientId: Long,
    val patientName: String,
    val recommendedFrequency: RecurringFrequency?,
    val lastVisitDate: LocalDateTime?,
    val reason: String // why they weren't auto-scheduled
)

// Batch confirm: staff approves the suggested schedule (with possible adjustments)
data class BatchCreateAppointmentRequest(
    val appointments: List<CreateAppointmentRequest>
)
