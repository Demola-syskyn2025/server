package com.deepen.service

import com.deepen.dto.CreateVisitSummaryRequest
import com.deepen.dto.UpdateVisitSummaryRequest
import com.deepen.dto.VisitSummaryDto
import com.deepen.model.VisitSummary
import com.deepen.repository.VisitSummaryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class VisitSummaryService(
    private val visitSummaryRepository: VisitSummaryRepository,
    private val appointmentService: AppointmentService,
    private val userService: UserService
) {

    fun findById(id: Long): VisitSummary? = visitSummaryRepository.findById(id).orElse(null)

    fun findByAppointmentId(appointmentId: Long): VisitSummary? =
        visitSummaryRepository.findByAppointmentId(appointmentId)

    fun findByPatientId(patientId: Long): List<VisitSummary> =
        visitSummaryRepository.findByPatientId(patientId)

    @Transactional
    fun createVisitSummary(request: CreateVisitSummaryRequest, createdById: Long): VisitSummary {
        val appointment = appointmentService.findById(request.appointmentId)
            ?: throw IllegalArgumentException("Appointment not found")

        val existingSummary = findByAppointmentId(request.appointmentId)
        if (existingSummary != null) {
            throw IllegalArgumentException("Visit summary already exists for this appointment")
        }

        val createdBy = userService.findById(createdById)
            ?: throw IllegalArgumentException("User not found")

        val visitSummary = VisitSummary(
            appointment = appointment,
            summary = request.summary,
            recommendations = request.recommendations,
            medications = request.medications,
            nextVisitRecommendation = request.nextVisitRecommendation,
            createdBy = createdBy
        )

        return visitSummaryRepository.save(visitSummary)
    }

    @Transactional
    fun updateVisitSummary(id: Long, request: UpdateVisitSummaryRequest): VisitSummary {
        val visitSummary = findById(id)
            ?: throw IllegalArgumentException("Visit summary not found")

        val updated = visitSummary.copy(
            summary = request.summary ?: visitSummary.summary,
            recommendations = request.recommendations ?: visitSummary.recommendations,
            medications = request.medications ?: visitSummary.medications,
            nextVisitRecommendation = request.nextVisitRecommendation ?: visitSummary.nextVisitRecommendation
        )

        return visitSummaryRepository.save(updated)
    }

    fun toDto(visitSummary: VisitSummary): VisitSummaryDto = VisitSummaryDto(
        id = visitSummary.id,
        appointmentId = visitSummary.appointment.id,
        patientName = "${visitSummary.appointment.patient.firstName} ${visitSummary.appointment.patient.lastName}",
        staffName = "${visitSummary.appointment.staff.firstName} ${visitSummary.appointment.staff.lastName}",
        summary = visitSummary.summary,
        recommendations = visitSummary.recommendations,
        medications = visitSummary.medications,
        nextVisitRecommendation = visitSummary.nextVisitRecommendation,
        createdBy = userService.toDto(visitSummary.createdBy),
        createdAt = visitSummary.createdAt
    )
}
