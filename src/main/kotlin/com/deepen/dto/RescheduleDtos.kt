package com.deepen.dto

import com.deepen.model.RescheduleStatus
import com.deepen.model.RequestType
import java.time.LocalDateTime

data class CreateRescheduleRequest(
    val appointmentId: Long,
    val reason: String? = null,
    val requestType: RequestType = RequestType.RESCHEDULE,
    val preferredDate1: LocalDateTime? = null,
    val preferredDate2: LocalDateTime? = null,
    val preferredDate3: LocalDateTime? = null
)

data class ReviewRescheduleRequest(
    val status: RescheduleStatus, // APPROVED, REJECTED, ALTERNATIVE_OFFERED
    val staffResponse: String? = null,
    val newScheduledAt: LocalDateTime? = null // required for APPROVED or ALTERNATIVE_OFFERED
)

data class RescheduleRequestDto(
    val id: Long,
    val appointmentId: Long,
    val patientName: String,
    val requestedBy: UserDto,
    val reason: String?,
    val requestType: RequestType,
    val preferredDate1: LocalDateTime?,
    val preferredDate2: LocalDateTime?,
    val preferredDate3: LocalDateTime?,
    val status: RescheduleStatus,
    val staffResponse: String?,
    val newScheduledAt: LocalDateTime?,
    val requestedAt: LocalDateTime,
    val reviewedAt: LocalDateTime?
)
