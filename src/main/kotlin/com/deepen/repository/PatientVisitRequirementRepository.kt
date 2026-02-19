package com.deepen.repository

import com.deepen.model.PatientVisitRequirement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PatientVisitRequirementRepository : JpaRepository<PatientVisitRequirement, Long> {
    fun findByPatientId(patientId: Long): List<PatientVisitRequirement>
    fun findByIsActiveTrue(): List<PatientVisitRequirement>
}
