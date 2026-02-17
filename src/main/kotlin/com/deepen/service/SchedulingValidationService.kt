package com.deepen.service

import com.deepen.repository.AppointmentRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class SchedulingValidationService(
    private val staffAvailabilityService: StaffAvailabilityService,
    private val timeOffService: TimeOffService,
    private val careAssignmentService: CareAssignmentService,
    private val appointmentRepository: AppointmentRepository
) {

    data class ValidationResult(
        val valid: Boolean,
        val errors: List<String> = emptyList()
    )

    fun validateAppointment(staffId: Long, patientId: Long, scheduledAt: LocalDateTime, durationMinutes: Int): ValidationResult {
        val errors = mutableListOf<String>()

        // 1. Check care assignment
        if (!careAssignmentService.isStaffAssignedToPatient(staffId, patientId)) {
            errors.add("Staff is not assigned to this patient")
        }

        // 2. Check staff availability for the day
        val dayOfWeek = scheduledAt.dayOfWeek.value % 7 // Convert: Monday=1 -> 1, Sunday=7 -> 0
        val appointmentTime = scheduledAt.toLocalTime()
        if (!staffAvailabilityService.isStaffAvailable(staffId, dayOfWeek, appointmentTime)) {
            errors.add("Staff is not available at this time")
        }

        // 3. Check time off
        if (timeOffService.isStaffOnTimeOff(staffId, scheduledAt.toLocalDate())) {
            errors.add("Staff is on approved time off for this date")
        }

        // 4. Check for conflicting appointments
        val endTime = scheduledAt.plusMinutes(durationMinutes.toLong())
        val existingAppointments = appointmentRepository.findByStaffId(staffId)
        val hasConflict = existingAppointments.any { existing ->
            val existingEnd = existing.scheduledAt.plusMinutes(existing.estimatedDurationMinutes.toLong())
            val overlaps = scheduledAt.isBefore(existingEnd) && endTime.isAfter(existing.scheduledAt)
            overlaps && existing.status.name !in listOf("CANCELLED", "RESCHEDULED")
        }
        if (hasConflict) {
            errors.add("Staff already has an appointment at this time")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors
        )
    }
}
