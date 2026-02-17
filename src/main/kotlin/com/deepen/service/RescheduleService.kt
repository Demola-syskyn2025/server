package com.deepen.service

import com.deepen.dto.CreateRescheduleRequest
import com.deepen.dto.RescheduleRequestDto
import com.deepen.dto.ReviewRescheduleRequest
import com.deepen.model.AppointmentStatus
import com.deepen.model.RescheduleRequest
import com.deepen.model.RescheduleStatus
import com.deepen.repository.RescheduleRequestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RescheduleService(
    private val rescheduleRequestRepository: RescheduleRequestRepository,
    private val appointmentService: AppointmentService,
    private val userService: UserService
) {

    fun findByAppointmentId(appointmentId: Long): List<RescheduleRequest> =
        rescheduleRequestRepository.findByAppointmentId(appointmentId)

    fun findPendingByStaffId(staffId: Long): List<RescheduleRequest> =
        rescheduleRequestRepository.findPendingByStaffId(staffId)

    fun findByRequestedById(userId: Long): List<RescheduleRequest> =
        rescheduleRequestRepository.findByRequestedById(userId)

    @Transactional
    fun createRequest(userId: Long, request: CreateRescheduleRequest): RescheduleRequest {
        val user = userService.findById(userId)
            ?: throw IllegalArgumentException("User not found")
        val appointment = appointmentService.findById(request.appointmentId)
            ?: throw IllegalArgumentException("Appointment not found")

        if (appointment.status == AppointmentStatus.CANCELLED || appointment.status == AppointmentStatus.COMPLETED) {
            throw IllegalArgumentException("Cannot reschedule a ${appointment.status} appointment")
        }

        return rescheduleRequestRepository.save(
            RescheduleRequest(
                appointment = appointment,
                requestedBy = user,
                reason = request.reason,
                preferredDate1 = request.preferredDate1,
                preferredDate2 = request.preferredDate2,
                preferredDate3 = request.preferredDate3
            )
        )
    }

    @Transactional
    fun reviewRequest(id: Long, request: ReviewRescheduleRequest): RescheduleRequest {
        val reschedule = rescheduleRequestRepository.findById(id).orElse(null)
            ?: throw IllegalArgumentException("Reschedule request not found")

        if (reschedule.status != RescheduleStatus.PENDING) {
            throw IllegalArgumentException("Request has already been reviewed")
        }

        // If approved or alternative offered, update the appointment
        if (request.status == RescheduleStatus.APPROVED || request.status == RescheduleStatus.ALTERNATIVE_OFFERED) {
            val newTime = request.newScheduledAt
                ?: throw IllegalArgumentException("New scheduled time is required when approving")

            val appointment = reschedule.appointment
            val updated = appointment.copy(
                scheduledAt = newTime,
                status = AppointmentStatus.RESCHEDULED,
                updatedAt = LocalDateTime.now()
            )
            appointmentService.saveAppointment(updated)
        }

        val updatedRequest = reschedule.copy(
            status = request.status,
            staffResponse = request.staffResponse,
            newScheduledAt = request.newScheduledAt,
            reviewedAt = LocalDateTime.now()
        )

        return rescheduleRequestRepository.save(updatedRequest)
    }

    fun toDto(reschedule: RescheduleRequest): RescheduleRequestDto = RescheduleRequestDto(
        id = reschedule.id,
        appointmentId = reschedule.appointment.id,
        patientName = "${reschedule.appointment.patient.firstName} ${reschedule.appointment.patient.lastName}",
        requestedBy = userService.toDto(reschedule.requestedBy),
        reason = reschedule.reason,
        preferredDate1 = reschedule.preferredDate1,
        preferredDate2 = reschedule.preferredDate2,
        preferredDate3 = reschedule.preferredDate3,
        status = reschedule.status,
        staffResponse = reschedule.staffResponse,
        newScheduledAt = reschedule.newScheduledAt,
        requestedAt = reschedule.requestedAt,
        reviewedAt = reschedule.reviewedAt
    )
}
