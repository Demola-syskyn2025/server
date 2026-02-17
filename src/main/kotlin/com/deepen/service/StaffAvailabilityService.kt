package com.deepen.service

import com.deepen.dto.BulkSetAvailabilityRequest
import com.deepen.dto.SetAvailabilityRequest
import com.deepen.dto.StaffAvailabilityDto
import com.deepen.model.StaffAvailability
import com.deepen.repository.StaffAvailabilityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class StaffAvailabilityService(
    private val staffAvailabilityRepository: StaffAvailabilityRepository,
    private val userService: UserService
) {

    fun findByStaffId(staffId: Long): List<StaffAvailability> =
        staffAvailabilityRepository.findByStaffId(staffId)

    fun isStaffAvailable(staffId: Long, dayOfWeek: Int, time: LocalTime): Boolean {
        val slots = staffAvailabilityRepository.findByStaffIdAndDayOfWeek(staffId, dayOfWeek)
        return slots.any { it.isAvailable && time >= it.startTime && time < it.endTime }
    }

    @Transactional
    fun setAvailability(staffId: Long, request: SetAvailabilityRequest): StaffAvailability {
        val staff = userService.findById(staffId)
            ?: throw IllegalArgumentException("Staff not found")

        if (request.dayOfWeek < 0 || request.dayOfWeek > 6) {
            throw IllegalArgumentException("Day of week must be 0-6 (Sunday-Saturday)")
        }
        if (request.startTime >= request.endTime) {
            throw IllegalArgumentException("Start time must be before end time")
        }

        return staffAvailabilityRepository.save(
            StaffAvailability(
                staff = staff,
                dayOfWeek = request.dayOfWeek,
                startTime = request.startTime,
                endTime = request.endTime,
                isAvailable = request.isAvailable
            )
        )
    }

    @Transactional
    fun bulkSetAvailability(staffId: Long, request: BulkSetAvailabilityRequest): List<StaffAvailability> {
        val staff = userService.findById(staffId)
            ?: throw IllegalArgumentException("Staff not found")

        // Remove existing availability
        staffAvailabilityRepository.deleteByStaffId(staffId)

        // Create new slots
        return request.slots.map { slot ->
            if (slot.startTime >= slot.endTime) {
                throw IllegalArgumentException("Start time must be before end time for day ${slot.dayOfWeek}")
            }
            staffAvailabilityRepository.save(
                StaffAvailability(
                    staff = staff,
                    dayOfWeek = slot.dayOfWeek,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    isAvailable = slot.isAvailable
                )
            )
        }
    }

    @Transactional
    fun removeAvailability(id: Long) {
        if (!staffAvailabilityRepository.existsById(id)) {
            throw IllegalArgumentException("Availability slot not found")
        }
        staffAvailabilityRepository.deleteById(id)
    }

    private fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
        0 -> "Sunday"
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        else -> "Unknown"
    }

    fun toDto(availability: StaffAvailability): StaffAvailabilityDto = StaffAvailabilityDto(
        id = availability.id,
        staffId = availability.staff.id,
        staffName = "${availability.staff.firstName} ${availability.staff.lastName}",
        dayOfWeek = availability.dayOfWeek,
        dayName = dayName(availability.dayOfWeek),
        startTime = availability.startTime,
        endTime = availability.endTime,
        isAvailable = availability.isAvailable
    )
}
