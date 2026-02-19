package com.deepen.controller

import com.deepen.dto.*
import com.deepen.service.AppointmentService
import com.deepen.service.ScheduleSuggestionService
import com.deepen.service.SchedulingValidationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/appointments")
class AppointmentController(
    private val appointmentService: AppointmentService,
    private val schedulingValidationService: SchedulingValidationService,
    private val scheduleSuggestionService: ScheduleSuggestionService
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

    @GetMapping("/suggest")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun getScheduleSuggestions(
        @RequestParam staffId: Long,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<ScheduleSuggestionResponse> {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        val response = scheduleSuggestionService.generateSuggestions(staffId, start, end)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun batchCreateAppointments(@RequestBody request: BatchCreateRequest): ResponseEntity<BatchCreateResponse> {
        val created = mutableListOf<AppointmentDto>()
        val errors = mutableListOf<String>()

        for (apptReq in request.appointments) {
            try {
                val validation = schedulingValidationService.validateAppointment(
                    staffId = apptReq.staffId,
                    patientId = apptReq.patientId,
                    scheduledAt = apptReq.scheduledAt,
                    durationMinutes = apptReq.estimatedDurationMinutes
                )
                if (!validation.valid) {
                    errors.add("Patient ${apptReq.patientId} at ${apptReq.scheduledAt}: ${validation.errors.joinToString()}")
                    continue
                }
                val appointment = appointmentService.createAppointment(apptReq)
                created.add(appointmentService.toDto(appointment))
            } catch (e: Exception) {
                errors.add("Patient ${apptReq.patientId}: ${e.message}")
            }
        }

        return ResponseEntity.ok(
            BatchCreateResponse(
                totalCreated = created.size,
                totalErrors = errors.size,
                created = created,
                errors = errors
            )
        )
    }
}

