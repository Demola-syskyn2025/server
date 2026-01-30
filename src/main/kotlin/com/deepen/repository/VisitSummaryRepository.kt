package com.deepen.repository

import com.deepen.model.VisitSummary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VisitSummaryRepository : JpaRepository<VisitSummary, Long> {
    fun findByAppointmentId(appointmentId: Long): VisitSummary?
    
    @Query("SELECT vs FROM VisitSummary vs WHERE vs.appointment.patient.id = :patientId ORDER BY vs.createdAt DESC")
    fun findByPatientId(patientId: Long): List<VisitSummary>
}
