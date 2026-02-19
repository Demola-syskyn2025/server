package com.deepen.repository

import com.deepen.model.PlanStatus
import com.deepen.model.SchedulePlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SchedulePlanRepository : JpaRepository<SchedulePlan, Long> {
    fun findByWeekStartDate(weekStartDate: LocalDate): List<SchedulePlan>
    fun findByWeekStartDateAndStatus(weekStartDate: LocalDate, status: PlanStatus): SchedulePlan?
    fun findByStatus(status: PlanStatus): List<SchedulePlan>
}
