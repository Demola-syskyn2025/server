package com.deepen.repository

import com.deepen.model.RescheduleRequest
import com.deepen.model.RescheduleStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RescheduleRequestRepository : JpaRepository<RescheduleRequest, Long> {
    fun findByAppointmentId(appointmentId: Long): List<RescheduleRequest>
    fun findByRequestedById(userId: Long): List<RescheduleRequest>
    fun findByStatus(status: RescheduleStatus): List<RescheduleRequest>

    @Query("SELECT r FROM RescheduleRequest r WHERE r.appointment.staff.id = :staffId AND r.status = 'PENDING'")
    fun findPendingByStaffId(staffId: Long): List<RescheduleRequest>
}
