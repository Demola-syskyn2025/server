package com.deepen.repository

import com.deepen.model.FamilyPatientLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FamilyPatientLinkRepository : JpaRepository<FamilyPatientLink, Long> {
    fun findByFamilyMemberId(familyMemberId: Long): List<FamilyPatientLink>
    fun findByPatientId(patientId: Long): List<FamilyPatientLink>
    fun findByFamilyMemberIdAndPatientId(familyMemberId: Long, patientId: Long): FamilyPatientLink?
}
