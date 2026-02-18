package com.deepen.service

import com.deepen.model.PatientPreference
import com.deepen.model.User
import com.deepen.repository.PatientPreferenceRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PatientPreferenceService(
    private val patientPreferenceRepository: PatientPreferenceRepository
) {
    
    fun getPatientPreference(patientId: Long): PatientPreference? {
        return patientPreferenceRepository.findByPatientId(patientId)
    }
    
    fun getPatientPreference(patient: User): PatientPreference? {
        return patientPreferenceRepository.findByPatient(patient)
    }
    
    fun saveOrUpdatePreference(preference: PatientPreference): PatientPreference {
        val existing = patientPreferenceRepository.findByPatient(preference.patient)
        
        return if (existing != null) {
            val updated = existing.copy(
                preferredDayOfWeek = preference.preferredDayOfWeek,
                preferredTimeStart = preference.preferredTimeStart,
                preferredTimeEnd = preference.preferredTimeEnd,
                preferredVisitType = preference.preferredVisitType,
                avoidMornings = preference.avoidMornings,
                avoidEvenings = preference.avoidEvenings,
                preferredLocation = preference.preferredLocation,
                notes = preference.notes,
                updatedAt = LocalDateTime.now()
            )
            patientPreferenceRepository.save(updated)
        } else {
            patientPreferenceRepository.save(preference)
        }
    }
    
    fun deletePreference(patientId: Long): Boolean {
        val preference = patientPreferenceRepository.findByPatientId(patientId)
        return if (preference != null) {
            patientPreferenceRepository.delete(preference)
            true
        } else {
            false
        }
    }
}
