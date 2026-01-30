package com.deepen.controller

import com.deepen.dto.AppointmentDto
import com.deepen.dto.CreateAppointmentRequest
import com.deepen.dto.UpdateAppointmentRequest
import com.deepen.service.AppointmentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/appointments")
class AppointmentController(
    private val appointmentService: AppointmentService
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
    
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun createAppointment(@RequestBody request: CreateAppointmentRequest): ResponseEntity<AppointmentDto> {
        val appointment = appointmentService.createAppointment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.toDto(appointment))
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
