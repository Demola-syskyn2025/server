package com.deepen.controller

import com.deepen.dto.CreateVisitSummaryRequest
import com.deepen.dto.UpdateVisitSummaryRequest
import com.deepen.dto.VisitSummaryDto
import com.deepen.service.VisitSummaryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/visit-summaries")
class VisitSummaryController(
    private val visitSummaryService: VisitSummaryService
) {

    @GetMapping("/{id}")
    fun getVisitSummary(@PathVariable id: Long): ResponseEntity<VisitSummaryDto> {
        val summary = visitSummaryService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(visitSummaryService.toDto(summary))
    }

    @GetMapping("/appointment/{appointmentId}")
    fun getByAppointment(@PathVariable appointmentId: Long): ResponseEntity<VisitSummaryDto> {
        val summary = visitSummaryService.findByAppointmentId(appointmentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(visitSummaryService.toDto(summary))
    }

    @GetMapping("/patient/{patientId}")
    fun getByPatient(@PathVariable patientId: Long): ResponseEntity<List<VisitSummaryDto>> {
        val summaries = visitSummaryService.findByPatientId(patientId)
        return ResponseEntity.ok(summaries.map { visitSummaryService.toDto(it) })
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun createVisitSummary(
        @RequestBody request: CreateVisitSummaryRequest,
        authentication: Authentication
    ): ResponseEntity<VisitSummaryDto> {
        val userId = authentication.name.toLong()
        val summary = visitSummaryService.createVisitSummary(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(visitSummaryService.toDto(summary))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun updateVisitSummary(
        @PathVariable id: Long,
        @RequestBody request: UpdateVisitSummaryRequest
    ): ResponseEntity<VisitSummaryDto> {
        val summary = visitSummaryService.updateVisitSummary(id, request)
        return ResponseEntity.ok(visitSummaryService.toDto(summary))
    }
}
