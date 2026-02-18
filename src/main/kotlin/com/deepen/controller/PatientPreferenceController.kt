package com.deepen.controller

import com.deepen.dto.PatientPreferenceDto
import com.deepen.model.PatientPreference
import com.deepen.service.PatientPreferenceService
import com.deepen.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/patient-preferences")
class PatientPreferenceController(
    private val patientPreferenceService: PatientPreferenceService,
    private val userService: UserService
) {
    
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'PATIENT', 'FAMILY_MEMBER')")
    fun getPatientPreference(@PathVariable patientId: Long): ResponseEntity<PatientPreferenceDto> {
        val preference = patientPreferenceService.getPatientPreference(patientId)
        return if (preference != null) {
            ResponseEntity.ok(toDto(preference))
        } else {
            ResponseEntity.ok(PatientPreferenceDto(
                patientId = patientId,
                preferredDayOfWeek = null,
                preferredTimeStart = null,
                preferredTimeEnd = null,
                preferredVisitType = null,
                avoidMornings = false,
                avoidEvenings = false,
                preferredLocation = null,
                notes = null
            ))
        }
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun savePreference(@Valid @RequestBody preferenceDto: PatientPreferenceDto): ResponseEntity<PatientPreferenceDto> {
        val patient = userService.findById(preferenceDto.patientId)
            ?: return ResponseEntity.badRequest().build()
        
        val preference = PatientPreference(
            patient = patient,
            preferredDayOfWeek = preferenceDto.preferredDayOfWeek,
            preferredTimeStart = preferenceDto.preferredTimeStart,
            preferredTimeEnd = preferenceDto.preferredTimeEnd,
            preferredVisitType = preferenceDto.preferredVisitType,
            avoidMornings = preferenceDto.avoidMornings,
            avoidEvenings = preferenceDto.avoidEvenings,
            preferredLocation = preferenceDto.preferredLocation,
            notes = preferenceDto.notes
        )
        
        val saved = patientPreferenceService.saveOrUpdatePreference(preference)
        return ResponseEntity.ok(toDto(saved))
    }
    
    @DeleteMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun deletePreference(@PathVariable patientId: Long): ResponseEntity<Void> {
        val deleted = patientPreferenceService.deletePreference(patientId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    private fun toDto(preference: PatientPreference): PatientPreferenceDto {
        return PatientPreferenceDto(
            patientId = preference.patient.id,
            preferredDayOfWeek = preference.preferredDayOfWeek,
            preferredTimeStart = preference.preferredTimeStart,
            preferredTimeEnd = preference.preferredTimeEnd,
            preferredVisitType = preference.preferredVisitType,
            avoidMornings = preference.avoidMornings,
            avoidEvenings = preference.avoidEvenings,
            preferredLocation = preference.preferredLocation,
            notes = preference.notes
        )
    }
}
