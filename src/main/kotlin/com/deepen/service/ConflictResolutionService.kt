package com.deepen.service

import com.deepen.dto.AlternativeTimeSuggestion
import com.deepen.dto.AvailabilityCheckRequest
import com.deepen.dto.AvailabilityCheckResponse
import com.deepen.model.Appointment
import com.deepen.model.AppointmentType
import com.deepen.model.PatientPreference
import com.deepen.model.StaffAvailability
import com.deepen.repository.AppointmentRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Service
class ConflictResolutionService(
    private val appointmentRepository: AppointmentRepository,
    private val staffAvailabilityService: StaffAvailabilityService,
    private val timeOffService: TimeOffService,
    private val patientPreferenceService: PatientPreferenceService
) {
    
    /**
     * Check real-time availability for a specific time slot
     */
    fun checkAvailability(request: AvailabilityCheckRequest): AvailabilityCheckResponse {
        val conflicts = findConflicts(
            request.staffId,
            request.patientId,
            request.scheduledAt,
            request.durationMinutes
        )
        
        val isAvailable = conflicts.isEmpty()
        
        return AvailabilityCheckResponse(
            isAvailable = isAvailable,
            conflicts = conflicts,
            alternativeTimes = if (!isAvailable) {
                findAlternativeTimes(request)
            } else emptyList()
        )
    }
    
    /**
     * Find conflicts for a proposed appointment
     */
    private fun findConflicts(
        staffId: Long,
        patientId: Long,
        proposedTime: LocalDateTime,
        durationMinutes: Int
    ): List<String> {
        val conflicts = mutableListOf<String>()
        
        // Check staff availability
        val dayOfWeek = proposedTime.dayOfWeek.value % 7
        val availability = staffAvailabilityService.findByStaffId(staffId)
            .filter { it.dayOfWeek == dayOfWeek && it.isAvailable }
        
        if (availability.isEmpty()) {
            conflicts.add("Staff not available on ${proposedTime.dayOfWeek}")
        } else {
            val timeInRange = availability.any { avail ->
                val startTime = proposedTime.toLocalTime()
                startTime >= avail.startTime && 
                startTime.plusMinutes(durationMinutes.toLong()) <= avail.endTime
            }
            
            if (!timeInRange) {
                conflicts.add("Time outside staff working hours")
            }
        }
        
        // Check time-off
        if (timeOffService.isStaffOnTimeOff(staffId, proposedTime.toLocalDate())) {
            conflicts.add("Staff on time-off")
        }
        
        // Check existing appointments
        val endTime = proposedTime.plusMinutes(durationMinutes.toLong())
        val existingAppointments = appointmentRepository.findByStaffIdAndDateRange(
            staffId, 
            proposedTime.minusMinutes(30), 
            endTime.plusMinutes(30)
        ).filter { it.status != com.deepen.model.AppointmentStatus.CANCELLED }
        
        for (existing in existingAppointments) {
            val existingEnd = existing.scheduledAt.plusMinutes(existing.estimatedDurationMinutes.toLong())
            
            if (proposedTime.isBefore(existingEnd) && endTime.isAfter(existing.scheduledAt)) {
                conflicts.add("Conflict with existing appointment at ${existing.scheduledAt}")
            }
        }
        
        // Check patient preferences
        val patientPreference = patientPreferenceService.getPatientPreference(patientId)
        if (patientPreference != null) {
            if (patientPreference.avoidMornings && proposedTime.hour < 12) {
                conflicts.add("Patient prefers to avoid mornings")
            }
            if (patientPreference.avoidEvenings && proposedTime.hour >= 17) {
                conflicts.add("Patient prefers to avoid evenings")
            }
            
            patientPreference.preferredDayOfWeek?.let { preferredDay ->
                if (proposedTime.dayOfWeek != preferredDay) {
                    conflicts.add("Patient prefers ${preferredDay}")
                }
            }
            
            patientPreference.preferredTimeStart?.let { start ->
                patientPreference.preferredTimeEnd?.let { end ->
                    val currentTime = proposedTime.toLocalTime()
                    if (currentTime.isBefore(start) || currentTime.plusMinutes(durationMinutes.toLong()).isAfter(end)) {
                        conflicts.add("Patient prefers time between $start and $end")
                    }
                }
            }
        }
        
        return conflicts
    }
    
    /**
     * Find alternative time suggestions when conflicts occur
     */
    private fun findAlternativeTimes(request: AvailabilityCheckRequest): List<AlternativeTimeSuggestion> {
        val alternatives = mutableListOf<AlternativeTimeSuggestion>()
        val patientPreference = patientPreferenceService.getPatientPreference(request.patientId)
        
        // Get staff availability for the week
        val availability = staffAvailabilityService.findByStaffId(request.staffId)
            .filter { it.isAvailable }
        
        // Search for alternatives within the next 7 days
        var currentDay = request.scheduledAt.toLocalDate()
        val maxDays = 7
        
        for (dayOffset in 0..maxDays) {
            if (alternatives.size >= 5) break // Limit to 5 suggestions
            
            val searchDate = currentDay.plusDays(dayOffset.toLong())
            val dayOfWeek = searchDate.dayOfWeek.value % 7
            
            // Skip if staff on time-off
            if (timeOffService.isStaffOnTimeOff(request.staffId, searchDate)) continue
            
            // Get availability for this day
            val dayAvailability = availability.filter { it.dayOfWeek == dayOfWeek }
            if (dayAvailability.isEmpty()) continue
            
            // Check patient preference for this day
            if (patientPreference?.preferredDayOfWeek != null && 
                searchDate.dayOfWeek != patientPreference.preferredDayOfWeek) {
                // Still include but mark as not preferred
            }
            
            // Find available slots
            for (avail in dayAvailability) {
                if (alternatives.size >= 5) break
                
                var slotTime = avail.startTime
                val endTime = avail.endTime
                
                while (slotTime.plusMinutes(request.durationMinutes.toLong()) <= endTime && alternatives.size < 5) {
                    val proposedDateTime = searchDate.atTime(slotTime)
                    
                    // Check if this slot is actually free
                    val conflicts = findConflicts(
                        request.staffId,
                        request.patientId,
                        proposedDateTime,
                        request.durationMinutes
                    )
                    
                    if (conflicts.isEmpty()) {
                        val isPreferred = isPreferredTime(proposedDateTime, patientPreference)
                        
                        alternatives.add(
                            AlternativeTimeSuggestion(
                                scheduledAt = proposedDateTime,
                                reason = if (isPreferred) "Matches patient preferences" else "Available slot",
                                isPreferred = isPreferred,
                                confidence = calculateConfidence(proposedDateTime, patientPreference, request.scheduledAt)
                            )
                        )
                    }
                    
                    slotTime = slotTime.plusMinutes(30) // Check in 30-minute increments
                }
            }
        }
        
        // Sort by preference and confidence
        return alternatives.sortedWith(compareByDescending<AlternativeTimeSuggestion> { it.isPreferred }
            .thenByDescending { it.confidence }
            .thenBy { it.scheduledAt })
            .take(5)
    }
    
    private fun isPreferredTime(dateTime: LocalDateTime, preference: PatientPreference?): Boolean {
        if (preference == null) return false
        
        val time = dateTime.toLocalTime()
        val day = dateTime.dayOfWeek
        
        // Check day preference
        if (preference.preferredDayOfWeek != null && day != preference.preferredDayOfWeek) {
            return false
        }
        
        // Check time range preference
        preference.preferredTimeStart?.let { start ->
            preference.preferredTimeEnd?.let { end ->
                if (time.isBefore(start) || time.plusMinutes(30).isAfter(end)) {
                    return false
                }
            }
        }
        
        // Check avoid times
        if (preference.avoidMornings && time.hour < 12) return false
        if (preference.avoidEvenings && time.hour >= 17) return false
        
        return true
    }
    
    private fun calculateConfidence(
        suggestedTime: LocalDateTime,
        preference: PatientPreference?,
        originalTime: LocalDateTime
    ): Double {
        var confidence = 0.5 // Base confidence
        
        if (preference != null) {
            // Boost for matching preferences
            if (isPreferredTime(suggestedTime, preference)) {
                confidence += 0.3
            }
            
            // Boost for preferred visit type
            // (This would need visit type parameter in the request)
        }
        
        // Reduce confidence for time distance from original request
        val hoursDifference = ChronoUnit.HOURS.between(originalTime, suggestedTime)
        confidence -= (hoursDifference * 0.02).coerceAtMost(0.2)
        
        return confidence.coerceIn(0.0, 1.0)
    }
}
