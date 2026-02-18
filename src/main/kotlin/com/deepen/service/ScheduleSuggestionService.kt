package com.deepen.service

import com.deepen.dto.*
import com.deepen.model.*
import com.deepen.repository.AppointmentRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class ScheduleSuggestionService(
    private val careAssignmentService: CareAssignmentService,
    private val staffAvailabilityService: StaffAvailabilityService,
    private val timeOffService: TimeOffService,
    private val appointmentRepository: AppointmentRepository,
    private val appointmentService: AppointmentService,
    private val userService: UserService
) {

    /**
     * Generate a suggested schedule for a staff member for a given period.
     *
     * Logic:
     * 1. Get all patients assigned to this staff
     * 2. Get existing appointments in the period (already scheduled)
     * 3. For each patient, determine visit frequency from their most recent recurring series
     * 4. Check if the patient already has enough visits in the period
     * 5. If not, suggest time slots based on:
     *    - Their usual visit day/time (from history)
     *    - Staff availability for that day
     *    - No conflicts with already-scheduled or already-suggested slots
     */
    fun suggestSchedule(staffId: Long, startDate: LocalDate, endDate: LocalDate): ScheduleSuggestionResponse {
        val staff = userService.findById(staffId)
            ?: throw IllegalArgumentException("Staff not found")

        // 1. Get assigned patients
        val assignments = careAssignmentService.findByStaffId(staffId)
        val patients = assignments.map { it.patient }

        // 2. Get existing appointments in period
        val periodStart = startDate.atStartOfDay()
        val periodEnd = endDate.atTime(23, 59, 59)
        val existingAppointments = appointmentRepository.findByStaffIdAndDateRange(staffId, periodStart, periodEnd)
            .filter { it.status != AppointmentStatus.CANCELLED }

        // 3. Get staff availability
        val availabilitySlots = staffAvailabilityService.findByStaffId(staffId)
            .filter { it.isAvailable }

        // Track occupied time slots (existing + newly suggested)
        val occupiedSlots = existingAppointments.map { appt ->
            TimeSlot(appt.scheduledAt, appt.scheduledAt.plusMinutes(appt.estimatedDurationMinutes.toLong()))
        }.toMutableList()

        val suggestions = mutableListOf<SuggestedAppointment>()
        val unscheduledPatients = mutableListOf<UnscheduledPatient>()

        for (patient in patients) {
            // Find the patient's visit pattern from recent history
            val patientHistory = appointmentRepository.findByPatientIdAndStaffId(patient.id, staffId)
            val visitPattern = determineVisitPattern(patientHistory)

            // How many visits does this patient need in this period?
            val requiredVisits = calculateRequiredVisits(visitPattern, startDate, endDate)

            // How many already scheduled?
            val alreadyScheduled = existingAppointments.count { it.patient.id == patient.id }

            val visitsNeeded = (requiredVisits - alreadyScheduled).coerceAtLeast(0)
            if (visitsNeeded == 0) continue

            // Try to suggest time slots
            val patientName = "${patient.firstName} ${patient.lastName}"
            var scheduled = 0

            // Determine preferred day/time from history
            val preferredSlot = findPreferredSlot(patientHistory)

            // Generate candidate dates in the period
            val candidateDates = generateCandidateDates(
                startDate, endDate, visitPattern.frequency, preferredSlot?.dayOfWeek
            )

            for (candidateDate in candidateDates) {
                if (scheduled >= visitsNeeded) break

                // Check if staff is on time off
                if (timeOffService.isStaffOnTimeOff(staffId, candidateDate)) continue

                // Check staff availability for this day
                val dayOfWeek = candidateDate.dayOfWeek.value % 7
                val dayAvailability = availabilitySlots.filter { it.dayOfWeek == dayOfWeek }
                if (dayAvailability.isEmpty()) continue

                // Find a free time slot on this day
                val slotTime = findFreeSlot(
                    candidateDate,
                    dayAvailability,
                    occupiedSlots,
                    visitPattern.durationMinutes,
                    preferredSlot?.time
                )

                if (slotTime != null) {
                    val scheduledAt = candidateDate.atTime(slotTime)
                    val endAt = scheduledAt.plusMinutes(visitPattern.durationMinutes.toLong())

                    suggestions.add(SuggestedAppointment(
                        patientId = patient.id,
                        patientName = patientName,
                        scheduledAt = scheduledAt,
                        estimatedDurationMinutes = visitPattern.durationMinutes,
                        type = visitPattern.type,
                        notes = visitPattern.notes,
                        location = visitPattern.location,
                        reason = buildReason(visitPattern),
                        isFromRecurring = visitPattern.recurringGroupId != null,
                        recurringGroupId = visitPattern.recurringGroupId
                    ))

                    // Mark this slot as occupied
                    occupiedSlots.add(TimeSlot(scheduledAt, endAt))
                    scheduled++
                }
            }

            if (scheduled < visitsNeeded) {
                unscheduledPatients.add(UnscheduledPatient(
                    patientId = patient.id,
                    patientName = patientName,
                    recommendedFrequency = visitPattern.frequency,
                    lastVisitDate = patientHistory
                        .filter { it.status == AppointmentStatus.COMPLETED }
                        .maxByOrNull { it.scheduledAt }?.scheduledAt,
                    reason = "Could only schedule $scheduled of $visitsNeeded needed visits — no available slots"
                ))
            }
        }

        // Sort suggestions by date/time
        val sortedSuggestions = suggestions.sortedBy { it.scheduledAt }

        return ScheduleSuggestionResponse(
            staffId = staffId,
            staffName = "${staff.firstName} ${staff.lastName}",
            periodStart = startDate,
            periodEnd = endDate,
            suggestions = sortedSuggestions,
            alreadyScheduled = existingAppointments.map { appointmentService.toDto(it) },
            unscheduledPatients = unscheduledPatients
        )
    }

    // ── Internal helpers ──────────────────────────────────

    private data class VisitPattern(
        val frequency: RecurringFrequency?,
        val durationMinutes: Int,
        val type: AppointmentType,
        val notes: String?,
        val location: String?,
        val recurringGroupId: String?
    )

    private data class PreferredSlot(
        val dayOfWeek: java.time.DayOfWeek,
        val time: LocalTime
    )

    private data class TimeSlot(
        val start: LocalDateTime,
        val end: LocalDateTime
    )

    /**
     * Analyze appointment history to determine visit pattern for a patient.
     */
    private fun determineVisitPattern(history: List<Appointment>): VisitPattern {
        // First check for active recurring series
        val recurring = history
            .filter { it.recurringGroupId != null && it.status != AppointmentStatus.CANCELLED }
            .maxByOrNull { it.scheduledAt }

        if (recurring != null) {
            return VisitPattern(
                frequency = recurring.recurringFrequency,
                durationMinutes = recurring.estimatedDurationMinutes,
                type = recurring.type,
                notes = recurring.notes,
                location = recurring.location,
                recurringGroupId = recurring.recurringGroupId
            )
        }

        // Fall back to most recent appointment
        val recent = history
            .filter { it.status != AppointmentStatus.CANCELLED }
            .maxByOrNull { it.scheduledAt }

        return VisitPattern(
            frequency = RecurringFrequency.MONTHLY, // default for non-recurring
            durationMinutes = recent?.estimatedDurationMinutes ?: 30,
            type = recent?.type ?: AppointmentType.HOME_VISIT,
            notes = recent?.notes,
            location = recent?.location,
            recurringGroupId = null
        )
    }

    /**
     * Calculate how many visits a patient needs in a given period.
     */
    private fun calculateRequiredVisits(pattern: VisitPattern, start: LocalDate, end: LocalDate): Int {
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1
        val weeks = (days / 7.0).coerceAtLeast(1.0)

        return when (pattern.frequency) {
            RecurringFrequency.WEEKLY -> weeks.toInt()
            RecurringFrequency.BIWEEKLY -> (weeks / 2).toInt().coerceAtLeast(1)
            RecurringFrequency.MONTHLY -> (days / 30.0).toInt().coerceAtLeast(1)
            null -> 1 // default: at least one visit
        }
    }

    /**
     * Find the patient's preferred visit day/time from history.
     */
    private fun findPreferredSlot(history: List<Appointment>): PreferredSlot? {
        val recent = history
            .filter { it.status != AppointmentStatus.CANCELLED }
            .sortedByDescending { it.scheduledAt }
            .firstOrNull() ?: return null

        return PreferredSlot(
            dayOfWeek = recent.scheduledAt.dayOfWeek,
            time = recent.scheduledAt.toLocalTime()
        )
    }

    /**
     * Generate candidate dates within the period based on frequency and preferred day.
     */
    private fun generateCandidateDates(
        start: LocalDate,
        end: LocalDate,
        frequency: RecurringFrequency?,
        preferredDay: java.time.DayOfWeek?
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start

        // If we have a preferred day, start from the first occurrence of that day
        if (preferredDay != null) {
            while (current.dayOfWeek != preferredDay && !current.isAfter(end)) {
                current = current.plusDays(1)
            }
        }

        val stepDays = when (frequency) {
            RecurringFrequency.WEEKLY -> 7L
            RecurringFrequency.BIWEEKLY -> 14L
            RecurringFrequency.MONTHLY -> 28L
            null -> 7L
        }

        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(stepDays)
        }

        return dates
    }

    /**
     * Find a free time slot on a given date, respecting availability and existing appointments.
     * Prefers the patient's usual time, falls back to earliest available slot.
     */
    private fun findFreeSlot(
        date: LocalDate,
        availability: List<StaffAvailability>,
        occupiedSlots: List<TimeSlot>,
        durationMinutes: Int,
        preferredTime: LocalTime?
    ): LocalTime? {
        // Availability windows for this day
        val windows = availability.sortedBy { it.startTime }

        // First try preferred time
        if (preferredTime != null && isSlotFree(date, preferredTime, durationMinutes, windows, occupiedSlots)) {
            return preferredTime
        }

        // Otherwise scan in 30-minute increments
        for (window in windows) {
            var candidate = window.startTime
            while (candidate.plusMinutes(durationMinutes.toLong()) <= window.endTime) {
                if (isSlotFree(date, candidate, durationMinutes, windows, occupiedSlots)) {
                    return candidate
                }
                candidate = candidate.plusMinutes(30)
            }
        }

        return null // no free slot found
    }

    private fun isSlotFree(
        date: LocalDate,
        time: LocalTime,
        durationMinutes: Int,
        availability: List<StaffAvailability>,
        occupiedSlots: List<TimeSlot>
    ): Boolean {
        val slotStart = date.atTime(time)
        val slotEnd = slotStart.plusMinutes(durationMinutes.toLong())

        // Check within availability window
        val inWindow = availability.any { time >= it.startTime && time.plusMinutes(durationMinutes.toLong()) <= it.endTime }
        if (!inWindow) return false

        // Check no conflicts
        return occupiedSlots.none { occupied ->
            slotStart.isBefore(occupied.end) && slotEnd.isAfter(occupied.start)
        }
    }

    private fun buildReason(pattern: VisitPattern): String {
        val freqLabel = when (pattern.frequency) {
            RecurringFrequency.WEEKLY -> "Weekly"
            RecurringFrequency.BIWEEKLY -> "Biweekly"
            RecurringFrequency.MONTHLY -> "Monthly"
            null -> "Routine"
        }
        val typeLabel = when (pattern.type) {
            AppointmentType.HOME_VISIT -> "home visit"
            AppointmentType.TELECONSULTATION -> "teleconsultation"
            AppointmentType.HOSPITAL_VISIT -> "hospital visit"
        }
        val suffix = if (pattern.recurringGroupId != null) " (recurring series)" else ""
        return "$freqLabel $typeLabel$suffix"
    }
}
