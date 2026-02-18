package com.deepen.service

import com.deepen.dto.CreateTimeOffRequest
import com.deepen.dto.ReviewTimeOffRequest
import com.deepen.dto.TimeOffRequestDto
import com.deepen.model.TimeOffRequest
import com.deepen.model.TimeOffStatus
import com.deepen.repository.TimeOffRequestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class TimeOffService(
    private val timeOffRequestRepository: TimeOffRequestRepository,
    private val userService: UserService
) {

    fun findByStaffId(staffId: Long): List<TimeOffRequest> =
        timeOffRequestRepository.findByStaffId(staffId)

    fun findPending(): List<TimeOffRequest> =
        timeOffRequestRepository.findByStatus(TimeOffStatus.PENDING)

    fun isStaffOnTimeOff(staffId: Long, date: LocalDate): Boolean =
        timeOffRequestRepository.findApprovedByStaffIdAndDate(staffId, date).isNotEmpty()

    @Transactional
    fun createRequest(staffId: Long, request: CreateTimeOffRequest): TimeOffRequest {
        val staff = userService.findById(staffId)
            ?: throw IllegalArgumentException("Staff not found")

        if (request.startDate.isAfter(request.endDate)) {
            throw IllegalArgumentException("Start date must be before or equal to end date")
        }
        if (request.startDate.isBefore(LocalDate.now())) {
            throw IllegalArgumentException("Cannot request time off in the past")
        }

        return timeOffRequestRepository.save(
            TimeOffRequest(
                staff = staff,
                startDate = request.startDate,
                endDate = request.endDate,
                reason = request.reason
            )
        )
    }

    @Transactional
    fun reviewRequest(id: Long, reviewerId: Long, request: ReviewTimeOffRequest): TimeOffRequest {
        val timeOff = timeOffRequestRepository.findById(id).orElse(null)
            ?: throw IllegalArgumentException("Time off request not found")

        if (timeOff.status != TimeOffStatus.PENDING) {
            throw IllegalArgumentException("Request has already been reviewed")
        }

        val reviewer = userService.findById(reviewerId)
            ?: throw IllegalArgumentException("Reviewer not found")

        val updated = timeOff.copy(
            status = request.status,
            reviewedAt = LocalDateTime.now(),
            reviewedBy = reviewer
        )

        return timeOffRequestRepository.save(updated)
    }

    fun toDto(timeOff: TimeOffRequest): TimeOffRequestDto = TimeOffRequestDto(
        id = timeOff.id,
        staff = userService.toDto(timeOff.staff),
        startDate = timeOff.startDate,
        endDate = timeOff.endDate,
        reason = timeOff.reason,
        status = timeOff.status,
        requestedAt = timeOff.requestedAt,
        reviewedAt = timeOff.reviewedAt,
        reviewedBy = timeOff.reviewedBy?.let { userService.toDto(it) }
    )
}
