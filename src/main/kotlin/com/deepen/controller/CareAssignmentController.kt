package com.deepen.controller

import com.deepen.dto.CareAssignmentDto
import com.deepen.dto.CreateCareAssignmentRequest
import com.deepen.service.CareAssignmentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/care-assignments")
class CareAssignmentController(
    private val careAssignmentService: CareAssignmentService
) {

    @GetMapping("/patient/{patientId}")
    fun getByPatient(@PathVariable patientId: Long): ResponseEntity<List<CareAssignmentDto>> {
        val assignments = careAssignmentService.findByPatientId(patientId)
        return ResponseEntity.ok(assignments.map { careAssignmentService.toDto(it) })
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun getByStaff(@PathVariable staffId: Long): ResponseEntity<List<CareAssignmentDto>> {
        val assignments = careAssignmentService.findByStaffId(staffId)
        return ResponseEntity.ok(assignments.map { careAssignmentService.toDto(it) })
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun createAssignment(@RequestBody request: CreateCareAssignmentRequest): ResponseEntity<CareAssignmentDto> {
        val assignment = careAssignmentService.createAssignment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(careAssignmentService.toDto(assignment))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun removeAssignment(@PathVariable id: Long): ResponseEntity<Void> {
        careAssignmentService.removeAssignment(id)
        return ResponseEntity.noContent().build()
    }
}
