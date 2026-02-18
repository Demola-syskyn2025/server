package com.deepen.repository

import com.deepen.model.StaffAvailability
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StaffAvailabilityRepository : JpaRepository<StaffAvailability, Long> {
    fun findByStaffId(staffId: Long): List<StaffAvailability>
    fun findByStaffIdAndDayOfWeek(staffId: Long, dayOfWeek: Int): List<StaffAvailability>
    fun deleteByStaffId(staffId: Long)
}
