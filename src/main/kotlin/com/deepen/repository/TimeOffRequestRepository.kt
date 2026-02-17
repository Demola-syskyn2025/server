package com.deepen.repository

import com.deepen.model.TimeOffRequest
import com.deepen.model.TimeOffStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TimeOffRequestRepository : JpaRepository<TimeOffRequest, Long> {
    fun findByStaffId(staffId: Long): List<TimeOffRequest>
    fun findByStatus(status: TimeOffStatus): List<TimeOffRequest>

    @Query("SELECT t FROM TimeOffRequest t WHERE t.staff.id = :staffId AND t.status = 'APPROVED' AND t.startDate <= :date AND t.endDate >= :date")
    fun findApprovedByStaffIdAndDate(staffId: Long, date: LocalDate): List<TimeOffRequest>
}
