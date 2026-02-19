package com.deepen.dto

import com.deepen.model.PlanStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class SchedulePlanDto(
    val id: Long,
    val weekStartDate: LocalDate,
    val status: PlanStatus,
    val createdBy: UserDto?,
    val createdAt: LocalDateTime,
    val confirmedAt: LocalDateTime?,
    val appointments: List<AppointmentDto>,
    val violations: List<String>
)

data class GeneratePlanRequest(
    val weekStartDate: LocalDate // Must be a Monday, must be a future week
)

data class ConfirmPlanRequest(
    val planId: Long
)

data class PlanSummaryDto(
    val planId: Long,
    val weekStartDate: LocalDate,
    val status: PlanStatus,
    val staffSummaries: List<StaffWeekSummaryDto>,
    val totalVisits: Int,
    val totalOfficeBlocks: Int,
    val violations: List<String>
)

data class StaffWeekSummaryDto(
    val staffId: Long,
    val staffName: String,
    val role: String,
    val dayOff: String?, // e.g. "WEDNESDAY"
    val totalWorkMinutes: Int,
    val totalVisits: Int,
    val totalOfficeBlocks: Int,
    val dailyBreakdown: List<DayBreakdownDto>
)

data class DayBreakdownDto(
    val date: LocalDate,
    val dayOfWeek: String,
    val isDayOff: Boolean,
    val workMinutes: Int,
    val visits: Int,
    val officeMinutes: Int
)
