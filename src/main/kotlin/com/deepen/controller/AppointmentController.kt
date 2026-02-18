package com.deepen.controller

import com.deepen.dto.AppointmentDto
import com.deepen.dto.CreateAppointmentRequest
import com.deepen.dto.CreateRecurringAppointmentRequest
import com.deepen.dto.UpdateAppointmentRequest
import com.deepen.service.AppointmentService
import com.deepen.service.SchedulingValidationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/appointments")
class AppointmentController(
    private val appointmentService: AppointmentService,
    private val schedulingValidationService: SchedulingValidationService
) {
    
    @GetMapping("/{id}")
    fun getAppointment(@PathVariable id: Long): ResponseEntity<AppointmentDto> {
        val appointment = appointmentService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(appointmentService.toDto(appointment))
    }
    
    @GetMapping("/patient/{patientId}")
    fun getPatientAppointments(@PathVariable patientId: Long): ResponseEntity<List<AppointmentDto>> {
        val appointments = appointmentService.findByPatientId(patientId)
        return ResponseEntity.ok(appointments.map { appointmentService.toDto(it) })
    }
    
    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun getStaffAppointments(@PathVariable staffId: Long): ResponseEntity<List<AppointmentDto>> {
        val appointments = appointmentService.findByStaffId(staffId)
        return ResponseEntity.ok(appointments.map { appointmentService.toDto(it) })
    }
    
    @GetMapping
    fun getAppointmentsByDateRange(
        @RequestParam startDate: LocalDateTime,
        @RequestParam endDate: LocalDateTime
    ): ResponseEntity<List<AppointmentDto>> {
        val appointments = appointmentService.findByDateRange(startDate, endDate)
        return ResponseEntity.ok(appointments.map { appointmentService.toDto(it) })
    }

    @GetMapping("/recurring/{groupId}")
    fun getRecurringSeries(@PathVariable groupId: String): ResponseEntity<List<AppointmentDto>> {
        val appointments = appointmentService.findByRecurringGroupId(groupId)
        if (appointments.isEmpty()) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(appointments.map { appointmentService.toDto(it) })
    }
    
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun validateAppointment(@RequestBody request: CreateAppointmentRequest): ResponseEntity<Map<String, Any>> {
        val result = schedulingValidationService.validateAppointment(
            staffId = request.staffId,
            patientId = request.patientId,
            scheduledAt = request.scheduledAt,
            durationMinutes = request.estimatedDurationMinutes
        )
        return ResponseEntity.ok(mapOf("valid" to result.valid, "errors" to result.errors))
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun createAppointment(@RequestBody request: CreateAppointmentRequest): ResponseEntity<Any> {
        val validation = schedulingValidationService.validateAppointment(
            staffId = request.staffId,
            patientId = request.patientId,
            scheduledAt = request.scheduledAt,
            durationMinutes = request.estimatedDurationMinutes
        )
        if (!validation.valid) {
            return ResponseEntity.badRequest().body(mapOf("errors" to validation.errors))
        }
        val appointment = appointmentService.createAppointment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.toDto(appointment))
    }

    @PostMapping("/recurring")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun createRecurringSeries(@RequestBody request: CreateRecurringAppointmentRequest): ResponseEntity<Any> {
        // Validate the first appointment in the series
        val validation = schedulingValidationService.validateAppointment(
            staffId = request.staffId,
            patientId = request.patientId,
            scheduledAt = request.scheduledAt,
            durationMinutes = request.estimatedDurationMinutes
        )
        if (!validation.valid) {
            return ResponseEntity.badRequest().body(mapOf("errors" to validation.errors))
        }
        val appointments = appointmentService.createRecurringSeries(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(appointments.map { appointmentService.toDto(it) })
    }

    @DeleteMapping("/recurring/{groupId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun cancelRecurringSeries(@PathVariable groupId: String): ResponseEntity<List<AppointmentDto>> {
        val cancelled = appointmentService.cancelRecurringSeries(groupId)
        return ResponseEntity.ok(cancelled.map { appointmentService.toDto(it) })
    }
    
    @PatchMapping("/{id}")
    fun updateAppointment(
        @PathVariable id: Long,
        @RequestBody request: UpdateAppointmentRequest
    ): ResponseEntity<AppointmentDto> {
        val appointment = appointmentService.updateAppointment(id, request)
        return ResponseEntity.ok(appointmentService.toDto(appointment))
    }
    
    @DeleteMapping("/{id}")
    fun cancelAppointment(@PathVariable id: Long): ResponseEntity<AppointmentDto> {
        val appointment = appointmentService.cancelAppointment(id)
        return ResponseEntity.ok(appointmentService.toDto(appointment))
    }
}
