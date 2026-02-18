package com.deepen.repository

import com.deepen.model.PatientPreference
import com.deepen.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PatientPreferenceRepository : JpaRepository<PatientPreference, Long> {
    fun findByPatient(patient: User): PatientPreference?
    fun findByPatientId(patientId: Long): PatientPreference?
}
