package com.deepen.service

import com.deepen.dto.*
import com.deepen.model.AppointmentStatus
import com.deepen.model.AppointmentType
import com.deepen.repository.AppointmentRepository
import com.deepen.repository.CareAssignmentRepository
import com.deepen.repository.StaffAvailabilityRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class ScheduleSuggestionService(
    private val appointmentRepository: AppointmentRepository,
    private val careAssignmentRepository: CareAssignmentRepository,
    private val staffAvailabilityRepository: StaffAvailabilityRepository,
    private val appointmentService: AppointmentService,
    private val timeOffService: TimeOffService,
    private val userService: UserService
) {

    fun generateSuggestions(staffId: Long, startDate: LocalDate, endDate: LocalDate): ScheduleSuggestionResponse {
        // 1. Get all patients assigned to this staff
        val assignments = careAssignmentRepository.findByStaffId(staffId)
        if (assignments.isEmpty()) {
            return ScheduleSuggestionResponse(
                staffId = staffId,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                suggestions = emptyList(),
                alreadyScheduled = emptyList(),
                unscheduledPatients = emptyList()
            )
        }

        // 2. Get already-scheduled appointments in the period
        val periodStart = startDate.atStartOfDay()
        val periodEnd = endDate.atTime(23, 59, 59)
        val existingAppointments = appointmentRepository.findByStaffIdAndDateRange(staffId, periodStart, periodEnd)
            .filter { it.status != AppointmentStatus.CANCELLED }

        val alreadyScheduledDtos = existingAppointments.map { appointmentService.toDto(it) }

        // 3. Get staff availability
        val availability = staffAvailabilityRepository.findByStaffId(staffId)
            .filter { it.isAvailable }

        // 4. For each patient, check if they need an appointment this period
        val suggestions = mutableListOf<SuggestedAppointment>()
        val unscheduled = mutableListOf<UnscheduledPatient>()

        for (assignment in assignments) {
            val patient = assignment.patient
            val patientName = "${patient.firstName} ${patient.lastName}"

            // Check if patient already has an appointment in this period
            val hasExisting = existingAppointments.any { it.patient.id == patient.id }
            if (hasExisting) continue

            // Look at patient's appointment history to determine frequency
            val patientHistory = appointmentRepository.findByPatientId(patient.id)
                .filter { it.staff.id == staffId && it.status != AppointmentStatus.CANCELLED }
                .sortedByDescending { it.scheduledAt }

            val lastVisit = patientHistory.firstOrNull()
            val frequency = inferFrequency(patientHistory)
            val needsVisit = shouldScheduleVisit(lastVisit?.scheduledAt, frequency, startDate)

            if (!needsVisit) continue

            // Try to find a slot
            val preferredType = inferAppointmentType(patientHistory)
            val duration = inferDuration(patientHistory)
            val slot = findAvailableSlot(
                staffId = staffId,
                startDate = startDate,
                endDate = endDate,
                availability = availability.map { AvailSlot(it.dayOfWeek, it.startTime, it.endTime) },
                existingAppointments = existingAppointments.map { SlotRef(it.scheduledAt, it.estimatedDurationMinutes) }
                        + suggestions.map { SlotRef(it.scheduledAt, it.estimatedDurationMinutes) },
                durationMinutes = duration
            )

            if (slot != null) {
                suggestions.add(
                    SuggestedAppointment(
                        patientId = patient.id,
                        patientName = patientName,
                        scheduledAt = slot,
                        estimatedDurationMinutes = duration,
                        type = preferredType,
                        notes = "${frequency.lowercase()} visit (auto-suggested)",
                        location = inferLocation(patientHistory),
                        reason = "Based on ${frequency.lowercase()} visit pattern"
                    )
                )
            } else {
                unscheduled.add(
                    UnscheduledPatient(
                        patientId = patient.id,
                        patientName = patientName,
                        reason = "No available time slot found in this period",
                        lastVisitDate = lastVisit?.scheduledAt,
                        recommendedFrequency = frequency
                    )
                )
            }
        }

        return ScheduleSuggestionResponse(
            staffId = staffId,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            suggestions = suggestions.sortedBy { it.scheduledAt },
            alreadyScheduled = alreadyScheduledDtos,
            unscheduledPatients = unscheduled
        )
    }

    // --- Helper data classes ---
    private data class AvailSlot(val dayOfWeek: Int, val startTime: LocalTime, val endTime: LocalTime)
    private data class SlotRef(val scheduledAt: LocalDateTime, val durationMinutes: Int)

    // --- Infer visit frequency from history ---
    private fun inferFrequency(history: List<com.deepen.model.Appointment>): String {
        if (history.size < 2) return "WEEKLY"
        val intervals = history.zipWithNext { a, b ->
            java.time.Duration.between(b.scheduledAt, a.scheduledAt).toDays()
        }
        val avgDays = intervals.average()
        return when {
            avgDays <= 10 -> "WEEKLY"
            avgDays <= 21 -> "BIWEEKLY"
            else -> "MONTHLY"
        }
    }

    // --- Check if a visit should be scheduled ---
    private fun shouldScheduleVisit(lastVisit: LocalDateTime?, frequency: String, periodStart: LocalDate): Boolean {
        if (lastVisit == null) return true
        val daysSince = java.time.Duration.between(lastVisit, periodStart.atStartOfDay()).toDays()
        return when (frequency) {
            "WEEKLY" -> daysSince >= 5
            "BIWEEKLY" -> daysSince >= 12
            "MONTHLY" -> daysSince >= 25
            else -> daysSince >= 5
        }
    }

    // --- Infer appointment type from history ---
    private fun inferAppointmentType(history: List<com.deepen.model.Appointment>): AppointmentType {
        if (history.isEmpty()) return AppointmentType.HOME_VISIT
        val counts = history.groupBy { it.type }.mapValues { it.value.size }
        return counts.maxByOrNull { it.value }?.key ?: AppointmentType.HOME_VISIT
    }

    // --- Infer duration from history ---
    private fun inferDuration(history: List<com.deepen.model.Appointment>): Int {
        if (history.isEmpty()) return 30
        return history.map { it.estimatedDurationMinutes }.average().toInt()
    }

    // --- Infer location from history ---
    private fun inferLocation(history: List<com.deepen.model.Appointment>): String? {
        return history.firstOrNull { it.location != null }?.location
    }

    // --- Find an available time slot ---
    private fun findAvailableSlot(
        staffId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        availability: List<AvailSlot>,
        existingAppointments: List<SlotRef>,
        durationMinutes: Int
    ): LocalDateTime? {
        var current = startDate
        while (!current.isAfter(endDate)) {
            val dayOfWeek = current.dayOfWeek.value % 7 // Monday=1, Sunday=0
            val dayAvail = availability.filter { it.dayOfWeek == dayOfWeek }

            // Check time off
            if (!timeOffService.isStaffOnTimeOff(staffId, current)) {
                for (avail in dayAvail) {
                    var slotStart = avail.startTime
                    val slotEnd = avail.endTime

                    while (slotStart.plusMinutes(durationMinutes.toLong()) <= slotEnd) {
                        val candidateStart = current.atTime(slotStart)
                        val candidateEnd = candidateStart.plusMinutes(durationMinutes.toLong())

                        // Check no conflicts with existing appointments
                        val hasConflict = existingAppointments.any { ref ->
                            val refEnd = ref.scheduledAt.plusMinutes(ref.durationMinutes.toLong())
                            candidateStart.isBefore(refEnd) && candidateEnd.isAfter(ref.scheduledAt)
                        }

                        if (!hasConflict) {
                            return candidateStart
                        }

                        slotStart = slotStart.plusMinutes(30) // Try next 30-min slot
                    }
                }
            }

            current = current.plusDays(1)
        }
        return null
    }
}
