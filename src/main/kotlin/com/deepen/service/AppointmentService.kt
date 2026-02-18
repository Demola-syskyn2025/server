package com.deepen.service

import com.deepen.dto.AppointmentDto
import com.deepen.dto.CreateAppointmentRequest
import com.deepen.dto.CreateRecurringAppointmentRequest
import com.deepen.dto.UpdateAppointmentRequest
import com.deepen.model.Appointment
import com.deepen.model.AppointmentStatus
import com.deepen.model.RecurringFrequency
import com.deepen.repository.AppointmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val userService: UserService
) {
    
    fun findById(id: Long): Appointment? = appointmentRepository.findById(id).orElse(null)

    fun saveAppointment(appointment: Appointment): Appointment = appointmentRepository.save(appointment)
    
    fun findByPatientId(patientId: Long): List<Appointment> = appointmentRepository.findByPatientId(patientId)
    
    fun findByStaffId(staffId: Long): List<Appointment> = appointmentRepository.findByStaffId(staffId)
    
    fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<Appointment> =
        appointmentRepository.findByDateRange(startDate, endDate)

    fun findByRecurringGroupId(groupId: String): List<Appointment> =
        appointmentRepository.findByRecurringGroupId(groupId)
    
    @Transactional
    fun createAppointment(request: CreateAppointmentRequest): Appointment {
        val patient = userService.findById(request.patientId)
            ?: throw IllegalArgumentException("Patient not found")
        val staff = userService.findById(request.staffId)
            ?: throw IllegalArgumentException("Staff not found")
        
        val appointment = Appointment(
            patient = patient,
            staff = staff,
            scheduledAt = request.scheduledAt,
            estimatedDurationMinutes = request.estimatedDurationMinutes,
            type = request.type,
            notes = request.notes,
            location = request.location
        )
        
        return appointmentRepository.save(appointment)
    }

    @Transactional
    fun createRecurringSeries(request: CreateRecurringAppointmentRequest): List<Appointment> {
        val patient = userService.findById(request.patientId)
            ?: throw IllegalArgumentException("Patient not found")
        val staff = userService.findById(request.staffId)
            ?: throw IllegalArgumentException("Staff not found")

        val groupId = UUID.randomUUID().toString()
        val endDate = request.endDate
        val appointments = mutableListOf<Appointment>()

        var current = request.scheduledAt
        while (!current.toLocalDate().isAfter(endDate)) {
            val appointment = appointmentRepository.save(
                Appointment(
                    patient = patient,
                    staff = staff,
                    scheduledAt = current,
                    estimatedDurationMinutes = request.estimatedDurationMinutes,
                    type = request.type,
                    notes = request.notes,
                    location = request.location,
                    recurringGroupId = groupId,
                    recurringFrequency = request.frequency,
                    recurringEndDate = endDate
                )
            )
            appointments.add(appointment)

            current = when (request.frequency) {
                RecurringFrequency.WEEKLY -> current.plusWeeks(1)
                RecurringFrequency.BIWEEKLY -> current.plusWeeks(2)
                RecurringFrequency.MONTHLY -> current.plusMonths(1)
            }
        }

        return appointments
    }

    @Transactional
    fun cancelRecurringSeries(groupId: String): List<Appointment> {
        val appointments = appointmentRepository.findByRecurringGroupId(groupId)
        return appointments
            .filter { it.status != AppointmentStatus.COMPLETED && it.status != AppointmentStatus.CANCELLED }
            .map { appointmentRepository.save(it.copy(status = AppointmentStatus.CANCELLED, updatedAt = LocalDateTime.now())) }
    }
    
    @Transactional
    fun updateAppointment(id: Long, request: UpdateAppointmentRequest): Appointment {
        val appointment = findById(id) ?: throw IllegalArgumentException("Appointment not found")
        
        val updated = appointment.copy(
            scheduledAt = request.scheduledAt ?: appointment.scheduledAt,
            status = request.status ?: appointment.status,
            notes = request.notes ?: appointment.notes,
            location = request.location ?: appointment.location,
            updatedAt = LocalDateTime.now()
        )
        
        return appointmentRepository.save(updated)
    }
    
    @Transactional
    fun cancelAppointment(id: Long): Appointment {
        val appointment = findById(id) ?: throw IllegalArgumentException("Appointment not found")
        val updated = appointment.copy(status = AppointmentStatus.CANCELLED, updatedAt = LocalDateTime.now())
        return appointmentRepository.save(updated)
    }
    
    fun toDto(appointment: Appointment): AppointmentDto = AppointmentDto(
        id = appointment.id,
        patient = userService.toDto(appointment.patient),
        staff = userService.toDto(appointment.staff),
        scheduledAt = appointment.scheduledAt,
        estimatedDurationMinutes = appointment.estimatedDurationMinutes,
        type = appointment.type,
        status = appointment.status,
        notes = appointment.notes,
        location = appointment.location,
        recurringGroupId = appointment.recurringGroupId,
        recurringFrequency = appointment.recurringFrequency,
        recurringEndDate = appointment.recurringEndDate,
        createdAt = appointment.createdAt
    )
}
