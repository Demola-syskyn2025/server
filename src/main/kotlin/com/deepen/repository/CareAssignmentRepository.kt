package com.deepen.repository

import com.deepen.model.CareAssignment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CareAssignmentRepository : JpaRepository<CareAssignment, Long> {
    fun findByPatientId(patientId: Long): List<CareAssignment>
    fun findByStaffId(staffId: Long): List<CareAssignment>
    fun findByPatientIdAndStaffId(patientId: Long, staffId: Long): CareAssignment?
    fun existsByPatientIdAndStaffId(patientId: Long, staffId: Long): Boolean
}
