package com.deepen.controller

import com.deepen.dto.*
import com.deepen.model.AppointmentType
import com.deepen.model.PlanStatus
import com.deepen.repository.AppointmentRepository
import com.deepen.repository.SchedulePlanRepository
import com.deepen.service.AppointmentService
import com.deepen.service.ScheduleEngineService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.DayOfWeek
import java.time.LocalDate

@RestController
@RequestMapping("/api/schedule-plans")
class SchedulePlanController(
    private val scheduleEngineService: ScheduleEngineService,
    private val schedulePlanRepository: SchedulePlanRepository,
    private val appointmentRepository: AppointmentRepository,
    private val appointmentService: AppointmentService
) {

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun generatePlan(@RequestBody request: GeneratePlanRequest): ResponseEntity<Any> {
        return try {
            val (plan, violations) = scheduleEngineService.generateWeeklyPlan(request.weekStartDate)
            val appointments = appointmentRepository.findByPlanId(plan.id)
            val dto = SchedulePlanDto(
                id = plan.id,
                weekStartDate = plan.weekStartDate,
                status = plan.status,
                createdBy = plan.createdBy?.let {
                    UserDto(it.id, it.email, it.firstName, it.lastName, it.phoneNumber, it.role)
                },
                createdAt = plan.createdAt,
                confirmedAt = plan.confirmedAt,
                appointments = appointments.map { appointmentService.toDto(it) },
                violations = violations
            )
            ResponseEntity.status(HttpStatus.CREATED).body(dto)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/{planId}/confirm")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun confirmPlan(@PathVariable planId: Long): ResponseEntity<Any> {
        return try {
            val plan = scheduleEngineService.confirmPlan(planId)
            val appointments = appointmentRepository.findByPlanId(plan.id)
            val dto = SchedulePlanDto(
                id = plan.id,
                weekStartDate = plan.weekStartDate,
                status = plan.status,
                createdBy = plan.createdBy?.let {
                    UserDto(it.id, it.email, it.firstName, it.lastName, it.phoneNumber, it.role)
                },
                createdAt = plan.createdAt,
                confirmedAt = plan.confirmedAt,
                appointments = appointments.map { appointmentService.toDto(it) },
                violations = emptyList()
            )
            ResponseEntity.ok(dto)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/{planId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun getPlan(@PathVariable planId: Long): ResponseEntity<Any> {
        val plan = schedulePlanRepository.findById(planId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val appointments = appointmentRepository.findByPlanId(plan.id)
        val dto = SchedulePlanDto(
            id = plan.id,
            weekStartDate = plan.weekStartDate,
            status = plan.status,
            createdBy = plan.createdBy?.let {
                UserDto(it.id, it.email, it.firstName, it.lastName, it.phoneNumber, it.role)
            },
            createdAt = plan.createdAt,
            confirmedAt = plan.confirmedAt,
            appointments = appointments.map { appointmentService.toDto(it) },
            violations = emptyList()
        )
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/week/{weekStartDate}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun getPlanByWeek(@PathVariable weekStartDate: String): ResponseEntity<Any> {
        val date = LocalDate.parse(weekStartDate)
        if (date.dayOfWeek != DayOfWeek.MONDAY) {
            return ResponseEntity.badRequest().body(mapOf("error" to "weekStartDate must be a Monday"))
        }

        val plans = schedulePlanRepository.findByWeekStartDate(date)
        if (plans.isEmpty()) {
            return ResponseEntity.ok(mapOf("message" to "No plan found for week $weekStartDate"))
        }

        // Prefer confirmed plan, else latest draft
        val plan = plans.firstOrNull { it.status == PlanStatus.CONFIRMED }
            ?: plans.maxByOrNull { it.createdAt }!!

        val appointments = appointmentRepository.findByPlanId(plan.id)
        val dto = SchedulePlanDto(
            id = plan.id,
            weekStartDate = plan.weekStartDate,
            status = plan.status,
            createdBy = plan.createdBy?.let {
                UserDto(it.id, it.email, it.firstName, it.lastName, it.phoneNumber, it.role)
            },
            createdAt = plan.createdAt,
            confirmedAt = plan.confirmedAt,
            appointments = appointments.map { appointmentService.toDto(it) },
            violations = emptyList()
        )
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/{planId}/summary")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun getPlanSummary(@PathVariable planId: Long): ResponseEntity<Any> {
        val plan = schedulePlanRepository.findById(planId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val appointments = appointmentRepository.findByPlanId(plan.id)
        val weekDays = (0L..6L).map { plan.weekStartDate.plusDays(it) }

        val staffGroups = appointments.groupBy { it.staff.id }

        val staffSummaries = staffGroups.map { (staffId, staffAppts) ->
            val staff = staffAppts.first().staff
            val dailyBreakdown = weekDays.map { date ->
                val dayAppts = staffAppts.filter { it.scheduledAt.toLocalDate() == date }
                val visits = dayAppts.count { it.type != AppointmentType.OFFICE_WORK }
                val officeMin = dayAppts.filter { it.type == AppointmentType.OFFICE_WORK }
                    .sumOf { it.estimatedDurationMinutes }
                val workMin = dayAppts.sumOf { it.estimatedDurationMinutes }
                DayBreakdownDto(
                    date = date,
                    dayOfWeek = date.dayOfWeek.name,
                    isDayOff = dayAppts.isEmpty(),
                    workMinutes = workMin,
                    visits = visits,
                    officeMinutes = officeMin
                )
            }
            StaffWeekSummaryDto(
                staffId = staffId,
                staffName = "${staff.firstName} ${staff.lastName}",
                role = staff.role.name,
                dayOff = dailyBreakdown.firstOrNull { it.isDayOff }?.dayOfWeek,
                totalWorkMinutes = staffAppts.sumOf { it.estimatedDurationMinutes },
                totalVisits = staffAppts.count { it.type != AppointmentType.OFFICE_WORK },
                totalOfficeBlocks = staffAppts.count { it.type == AppointmentType.OFFICE_WORK },
                dailyBreakdown = dailyBreakdown
            )
        }

        val summary = PlanSummaryDto(
            planId = plan.id,
            weekStartDate = plan.weekStartDate,
            status = plan.status,
            staffSummaries = staffSummaries,
            totalVisits = appointments.count { it.type != AppointmentType.OFFICE_WORK },
            totalOfficeBlocks = appointments.count { it.type == AppointmentType.OFFICE_WORK },
            violations = emptyList()
        )
        return ResponseEntity.ok(summary)
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun listPlans(): ResponseEntity<List<Map<String, Any?>>> {
        val plans = schedulePlanRepository.findAll()
        val result = plans.map { plan ->
            mapOf(
                "id" to plan.id,
                "weekStartDate" to plan.weekStartDate,
                "status" to plan.status,
                "createdAt" to plan.createdAt,
                "confirmedAt" to plan.confirmedAt,
                "appointmentCount" to appointmentRepository.findByPlanId(plan.id).size
            )
        }
        return ResponseEntity.ok(result)
    }
}
