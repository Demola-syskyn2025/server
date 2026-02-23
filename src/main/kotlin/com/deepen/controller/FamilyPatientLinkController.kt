package com.deepen.controller

import com.deepen.dto.CreateFamilyPatientLinkRequest
import com.deepen.dto.FamilyPatientLinkDto
import com.deepen.service.FamilyPatientLinkService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/family-links")
class FamilyPatientLinkController(
    private val familyPatientLinkService: FamilyPatientLinkService
) {

    @GetMapping("/family/{familyMemberId}")
    fun getByFamilyMember(@PathVariable familyMemberId: Long): ResponseEntity<List<FamilyPatientLinkDto>> {
        val links = familyPatientLinkService.findByFamilyMemberId(familyMemberId)
        return ResponseEntity.ok(links.map { familyPatientLinkService.toDto(it) })
    }

    @GetMapping("/patient/{patientId}")
    fun getByPatient(@PathVariable patientId: Long): ResponseEntity<List<FamilyPatientLinkDto>> {
        val links = familyPatientLinkService.findByPatientId(patientId)
        return ResponseEntity.ok(links.map { familyPatientLinkService.toDto(it) })
    }

    @PostMapping
    fun createLink(
        @RequestParam familyMemberId: Long,
        @RequestBody request: CreateFamilyPatientLinkRequest
    ): ResponseEntity<FamilyPatientLinkDto> {
        val link = familyPatientLinkService.createLink(familyMemberId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(familyPatientLinkService.toDto(link))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun removeLink(@PathVariable id: Long): ResponseEntity<Void> {
        familyPatientLinkService.removeLink(id)
        return ResponseEntity.noContent().build()
    }
}
