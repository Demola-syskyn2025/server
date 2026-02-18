package com.deepen.controller

import com.deepen.dto.*
import com.deepen.service.AppointmentService
import com.deepen.service.ScheduleSuggestionService
import com.deepen.service.SchedulingValidationService
import com.deepen.service.ConflictResolutionService
import com.deepen.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/appointments")
class AppointmentController(
    private val appointmentService: AppointmentService,
    private val schedulingValidationService: SchedulingValidationService,
    private val scheduleSuggestionService: ScheduleSuggestionService,
    private val conflictResolutionService: ConflictResolutionService,
    private val userService: UserService
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
    fun createAppointment(@RequestBody request: CreateAppointmentRequest): ResponseEntity<AppointmentCreationResponse> {
        // Check for conflicts using the enhanced conflict resolution service
        val availabilityRequest = AvailabilityCheckRequest(
            staffId = request.staffId,
            patientId = request.patientId,
            scheduledAt = request.scheduledAt,
            durationMinutes = request.estimatedDurationMinutes
        )
        
        val availabilityCheck = conflictResolutionService.checkAvailability(availabilityRequest)
        
        if (!availabilityCheck.isAvailable) {
            return ResponseEntity.ok(AppointmentCreationResponse(
                success = false,
                conflicts = availabilityCheck.conflicts,
                alternativeTimes = availabilityCheck.alternativeTimes,
                message = "Appointment conflicts detected. Please choose an alternative time."
            ))
        }
        
        // No conflicts, create the appointment
        val appointment = appointmentService.createAppointment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentCreationResponse(
            success = true,
            appointment = appointmentService.toDto(appointment),
            message = "Appointment created successfully"
        ))
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

    // ========== Schedule Suggestion ==========

    @GetMapping("/suggest")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun suggestSchedule(
        @RequestParam staffId: Long,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate
    ): ResponseEntity<ScheduleSuggestionResponse> {
        val suggestion = scheduleSuggestionService.suggestSchedule(staffId, startDate, endDate)
        return ResponseEntity.ok(suggestion)
    }

    // ========== Conflict Resolution ==========

    @PostMapping("/check-availability")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun checkAvailability(@RequestBody request: AvailabilityCheckRequest): ResponseEntity<AvailabilityCheckResponse> {
        val response = conflictResolutionService.checkAvailability(request)
        return ResponseEntity.ok(response)
    }

    // ========== Batch Create (confirm suggested schedule) ==========

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun batchCreateAppointments(@RequestBody request: BatchCreateAppointmentRequest): ResponseEntity<BatchCreationWithConflictsResponse> {
        val created = mutableListOf<AppointmentDto>()
        val conflicts = mutableListOf<AppointmentConflict>()

        for ((index, apptRequest) in request.appointments.withIndex()) {
            // Check for conflicts using the enhanced conflict resolution service
            val availabilityRequest = AvailabilityCheckRequest(
                staffId = apptRequest.staffId,
                patientId = apptRequest.patientId,
                scheduledAt = apptRequest.scheduledAt,
                durationMinutes = apptRequest.estimatedDurationMinutes
            )
            
            val availabilityCheck = conflictResolutionService.checkAvailability(availabilityRequest)
            
            if (!availabilityCheck.isAvailable) {
                // Get patient name for the conflict response
                val patient = userService.findById(apptRequest.patientId)
                val patientName = if (patient != null) "${patient.firstName} ${patient.lastName}" else "Unknown Patient"
                
                conflicts.add(AppointmentConflict(
                    index = index,
                    patientId = apptRequest.patientId,
                    patientName = patientName,
                    requestedTime = apptRequest.scheduledAt,
                    conflicts = availabilityCheck.conflicts,
                    alternativeTimes = availabilityCheck.alternativeTimes
                ))
                continue
            }
            
            // No conflicts, create the appointment
            val appointment = appointmentService.createAppointment(apptRequest)
            created.add(appointmentService.toDto(appointment))
        }

        val message = if (conflicts.isEmpty()) {
            "All appointments created successfully"
        } else if (created.isEmpty()) {
            "All appointments had conflicts. Please review and reschedule."
        } else {
            "Created ${created.size} appointments. ${conflicts.size} appointments had conflicts."
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(BatchCreationWithConflictsResponse(
            totalRequested = request.appointments.size,
            totalCreated = created.size,
            totalConflicts = conflicts.size,
            created = created,
            conflicts = conflicts,
            message = message
        ))
    }
}
