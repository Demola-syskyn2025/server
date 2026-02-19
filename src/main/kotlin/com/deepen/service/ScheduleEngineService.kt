package com.deepen.service

import com.deepen.model.*
import com.deepen.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

@Service
class ScheduleEngineService(
    private val schedulePlanRepository: SchedulePlanRepository,
    private val appointmentRepository: AppointmentRepository,
    private val patientVisitRequirementRepository: PatientVisitRequirementRepository,
    private val careAssignmentRepository: CareAssignmentRepository,
    private val staffAvailabilityRepository: StaffAvailabilityRepository,
    private val timeOffService: TimeOffService,
    private val userService: UserService
) {
    private val log = LoggerFactory.getLogger(ScheduleEngineService::class.java)

    companion object {
        const val WORK_DAY_START_HOUR = 8
        const val WORK_DAY_END_HOUR = 16
        const val MAX_DAILY_MINUTES = 480       // 8 hours
        const val MIN_WEEKLY_MINUTES = 2160     // 36 hours
        const val MAX_WEEKLY_MINUTES = 2280     // 38 hours
        const val TRAVEL_BUFFER_MINUTES = 10
        const val OFFICE_BLOCK_MIN_MINUTES = 15 // minimum office block size
    }

    // ── Internal data structures ──────────────────────────────────

    /** A single time block in a staff member's day */
    data class TimeBlock(
        val start: LocalTime,
        val end: LocalTime,
        val type: BlockType,
        val visitData: VisitPlacement? = null
    ) {
        val durationMinutes: Int get() = java.time.Duration.between(start, end).toMinutes().toInt()
    }

    enum class BlockType { OFFICE, VISIT, TRAVEL_BUFFER }

    data class VisitPlacement(
        val patientId: Long,
        val patientName: String,
        val visitType: AppointmentType,
        val durationMinutes: Int,
        val notes: String?,
        val location: String?,
        val priority: VisitPriority,
        val requirementId: Long
    )

    /** A visit request to be scheduled, derived from PatientVisitRequirement */
    data class VisitRequest(
        val requirementId: Long,
        val patientId: Long,
        val patientName: String,
        val priority: VisitPriority,
        val durationMinutes: Int,
        val visitType: AppointmentType,
        val preferredTimeStart: LocalTime?,
        val preferredTimeEnd: LocalTime?,
        val location: String?,
        val notes: String?,
        val primaryStaffId: Long?
    )

    /** Tracks a staff member's schedule for the entire week */
    data class StaffWeekSchedule(
        val staffId: Long,
        val staffName: String,
        val role: UserRole,
        val dayOff: DayOfWeek, // the weekday off
        val dailySchedules: MutableMap<LocalDate, MutableList<TimeBlock>>
    ) {
        fun totalWorkMinutes(): Int =
            dailySchedules.values.sumOf { blocks ->
                blocks.filter { it.type != BlockType.TRAVEL_BUFFER }
                    .sumOf { it.durationMinutes }
            }

        fun totalVisits(): Int =
            dailySchedules.values.sumOf { blocks ->
                blocks.count { it.type == BlockType.VISIT }
            }

        fun totalOfficeMinutes(): Int =
            dailySchedules.values.sumOf { blocks ->
                blocks.filter { it.type == BlockType.OFFICE }
                    .sumOf { it.durationMinutes }
            }

        fun dayWorkMinutes(date: LocalDate): Int =
            dailySchedules[date]?.filter { it.type != BlockType.TRAVEL_BUFFER }
                ?.sumOf { it.durationMinutes } ?: 0
    }

    // ── Main entry point ──────────────────────────────────────────

    @Transactional
    fun generateWeeklyPlan(weekStartDate: LocalDate): Pair<SchedulePlan, List<String>> {
        val violations = mutableListOf<String>()

        // Validation
        require(weekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
            "weekStartDate must be a Monday, got ${weekStartDate.dayOfWeek}"
        }

        val currentWeekMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        require(weekStartDate.isAfter(currentWeekMonday)) {
            "Cannot generate plan for current or past week"
        }

        // Idempotency: don't overwrite confirmed plans
        val existingConfirmed = schedulePlanRepository.findByWeekStartDateAndStatus(weekStartDate, PlanStatus.CONFIRMED)
        require(existingConfirmed == null) {
            "A confirmed plan already exists for week starting $weekStartDate"
        }

        // Delete any existing DRAFT plan for this week (regeneration)
        val existingDraft = schedulePlanRepository.findByWeekStartDateAndStatus(weekStartDate, PlanStatus.DRAFT)
        if (existingDraft != null) {
            val oldAppointments = appointmentRepository.findByPlanId(existingDraft.id)
            appointmentRepository.deleteAll(oldAppointments)
            schedulePlanRepository.delete(existingDraft)
            log.info("Deleted existing DRAFT plan for week $weekStartDate (id=${existingDraft.id})")
        }

        // Create plan
        val plan = schedulePlanRepository.save(
            SchedulePlan(weekStartDate = weekStartDate)
        )

        // Get all staff (DOCTOR and NURSE)
        val allStaff = userService.findStaff()
        if (allStaff.isEmpty()) {
            violations.add("No staff members found")
            return Pair(plan, violations)
        }

        val weekEndDate = weekStartDate.plusDays(4) // Friday
        val weekDays = (0L..4L).map { weekStartDate.plusDays(it) }

        // ── Step 0: Build staff week schedules ───────────────────
        val staffSchedules = allStaff.map { staff ->
            val dayOff = determineDayOff(staff, allStaff)
            val schedule = StaffWeekSchedule(
                staffId = staff.id,
                staffName = "${staff.firstName} ${staff.lastName}",
                role = staff.role,
                dayOff = dayOff,
                dailySchedules = mutableMapOf()
            )
            // Initialize working days
            for (date in weekDays) {
                if (date.dayOfWeek == dayOff) continue
                if (timeOffService.isStaffOnTimeOff(staff.id, date)) continue
                schedule.dailySchedules[date] = mutableListOf()
            }
            schedule
        }

        // ── Step 1: Generate office work skeleton ────────────────
        for (schedule in staffSchedules) {
            for ((date, blocks) in schedule.dailySchedules) {
                blocks.add(
                    TimeBlock(
                        start = LocalTime.of(WORK_DAY_START_HOUR, 0),
                        end = LocalTime.of(WORK_DAY_END_HOUR, 0),
                        type = BlockType.OFFICE
                    )
                )
            }
        }
        log.info("Step 1 complete: Office skeleton generated for ${staffSchedules.size} staff")

        // ── Step 2: Order visits ─────────────────────────────────
        val visitRequirements = patientVisitRequirementRepository.findByIsActiveTrue()
        val visitRequests = mutableListOf<VisitRequest>()

        for (req in visitRequirements) {
            val patient = req.patient
            val patientName = "${patient.firstName} ${patient.lastName}"

            // Find primary staff for this patient
            val primaryAssignment = careAssignmentRepository.findByPatientId(patient.id)
                .firstOrNull { it.isPrimary }

            // Generate one VisitRequest per visitsPerWeek
            repeat(req.visitsPerWeek) {
                visitRequests.add(
                    VisitRequest(
                        requirementId = req.id,
                        patientId = patient.id,
                        patientName = patientName,
                        priority = req.priority,
                        durationMinutes = req.durationMinutes,
                        visitType = req.visitType,
                        preferredTimeStart = req.preferredTimeStart,
                        preferredTimeEnd = req.preferredTimeEnd,
                        location = req.location,
                        notes = req.notes,
                        primaryStaffId = primaryAssignment?.staff?.id
                    )
                )
            }
        }

        // Sort: URGENT first, then HIGH, then ROUTINE; narrower time windows first; then by patientId
        visitRequests.sortWith(
            compareBy<VisitRequest> { it.priority.ordinal } // URGENT=0, HIGH=1, ROUTINE=2
                .thenBy { timeWindowWidth(it) }
                .thenBy { it.patientId }
        )

        log.info("Step 2 complete: ${visitRequests.size} visits to schedule, sorted by priority")

        // ── Step 3 & 4: Staff assignment + time slot placement ───
        for (visit in visitRequests) {
            val placed = tryPlaceVisit(visit, staffSchedules, weekDays, violations)
            if (!placed) {
                violations.add(
                    "UNSCHEDULED: ${visit.patientName} (${visit.priority}, ${visit.visitType}) - no feasible slot found"
                )
            }
        }

        log.info("Steps 3-4 complete: visits placed, ${violations.size} violations so far")

        // ── Step 5: Workload validation & office block fill ──────
        for (schedule in staffSchedules) {
            validateAndFillWorkload(schedule, violations)
        }

        log.info("Step 5 complete: workload validated")

        // ── Persist appointments ─────────────────────────────────
        val createdAppointments = mutableListOf<Appointment>()

        for (schedule in staffSchedules) {
            val staff = userService.findById(schedule.staffId)!!
            for ((date, blocks) in schedule.dailySchedules) {
                for (block in blocks) {
                    if (block.type == BlockType.TRAVEL_BUFFER) continue
                    if (block.durationMinutes < OFFICE_BLOCK_MIN_MINUTES && block.type == BlockType.OFFICE) continue

                    val patientUser = if (block.type == BlockType.VISIT && block.visitData != null) {
                        userService.findById(block.visitData.patientId)!!
                    } else {
                        staff // OFFICE_WORK: staff references themselves
                    }

                    val appointmentType = if (block.type == BlockType.OFFICE) {
                        AppointmentType.OFFICE_WORK
                    } else {
                        block.visitData!!.visitType
                    }

                    val appointment = appointmentRepository.save(
                        Appointment(
                            patient = patientUser,
                            staff = staff,
                            scheduledAt = date.atTime(block.start),
                            estimatedDurationMinutes = block.durationMinutes,
                            type = appointmentType,
                            status = AppointmentStatus.SCHEDULED,
                            notes = if (block.type == BlockType.OFFICE) "Office work" else block.visitData?.notes,
                            location = if (block.type == BlockType.VISIT) block.visitData?.location else "Office",
                            plan = plan,
                            isGenerated = true,
                            isLocked = false
                        )
                    )
                    createdAppointments.add(appointment)
                }
            }
        }

        log.info("Plan generated: ${createdAppointments.size} appointments, ${violations.size} violations")
        return Pair(plan, violations)
    }

    // ── Confirm plan ─────────────────────────────────────────────

    @Transactional
    fun confirmPlan(planId: Long): SchedulePlan {
        val plan = schedulePlanRepository.findById(planId).orElseThrow {
            IllegalArgumentException("Plan not found: $planId")
        }
        require(plan.status == PlanStatus.DRAFT) {
            "Only DRAFT plans can be confirmed, current status: ${plan.status}"
        }

        // Lock all appointments
        val appointments = appointmentRepository.findByPlanId(planId)
        for (appt in appointments) {
            appointmentRepository.save(
                appt.copy(
                    status = AppointmentStatus.CONFIRMED,
                    isLocked = true,
                    updatedAt = LocalDateTime.now()
                )
            )
        }

        // Update plan status
        val confirmed = plan.copy(
            status = PlanStatus.CONFIRMED,
            confirmedAt = LocalDateTime.now()
        )
        return schedulePlanRepository.save(confirmed)
    }

    // ── Helper: determine day off ────────────────────────────────
    // Deterministic: assign day off based on staff's position in sorted list
    private fun determineDayOff(staff: User, allStaff: List<User>): DayOfWeek {
        val sorted = allStaff.sortedBy { it.id }
        val index = sorted.indexOfFirst { it.id == staff.id }
        // Cycle through Mon-Fri
        val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        return weekdays[index % weekdays.size]
    }

    // ── Helper: time window width (for sorting) ──────────────────
    private fun timeWindowWidth(visit: VisitRequest): Int {
        if (visit.preferredTimeStart == null || visit.preferredTimeEnd == null) {
            return MAX_DAILY_MINUTES // full day = widest window
        }
        return java.time.Duration.between(visit.preferredTimeStart, visit.preferredTimeEnd).toMinutes().toInt()
    }

    // ── Step 3+4: Try to place a visit ───────────────────────────

    private fun tryPlaceVisit(
        visit: VisitRequest,
        staffSchedules: List<StaffWeekSchedule>,
        weekDays: List<LocalDate>,
        violations: MutableList<String>
    ): Boolean {
        // Step 3: Prefer primary staff, fallback to least-loaded
        val candidateOrder = buildStaffOrder(visit, staffSchedules)

        for (schedule in candidateOrder) {
            val slot = findSlotForVisit(visit, schedule, weekDays)
            if (slot != null) {
                placeVisitInSchedule(visit, schedule, slot.first, slot.second)
                return true
            }
        }

        // Failure: try any staff member as fallback
        for (schedule in staffSchedules) {
            if (schedule in candidateOrder) continue
            val slot = findSlotForVisit(visit, schedule, weekDays)
            if (slot != null) {
                placeVisitInSchedule(visit, schedule, slot.first, slot.second)
                violations.add(
                    "REASSIGNED: ${visit.patientName} assigned to ${schedule.staffName} (not primary staff)"
                )
                return true
            }
        }

        return false
    }

    /** Build ordered list: primary staff first, then by lowest weekly workload */
    private fun buildStaffOrder(
        visit: VisitRequest,
        staffSchedules: List<StaffWeekSchedule>
    ): List<StaffWeekSchedule> {
        val primary = if (visit.primaryStaffId != null) {
            staffSchedules.firstOrNull { it.staffId == visit.primaryStaffId }
        } else null

        val others = staffSchedules
            .filter { it.staffId != visit.primaryStaffId }
            .sortedBy { it.totalWorkMinutes() }

        return if (primary != null) listOf(primary) + others else others
    }

    /** Find earliest feasible time slot on any working day */
    private fun findSlotForVisit(
        visit: VisitRequest,
        schedule: StaffWeekSchedule,
        weekDays: List<LocalDate>
    ): Pair<LocalDate, LocalTime>? {
        val totalNeeded = visit.durationMinutes + TRAVEL_BUFFER_MINUTES

        for (date in weekDays) {
            val blocks = schedule.dailySchedules[date] ?: continue // day off or time off

            // Check daily visit+travel load (office work is elastic and will be reduced)
            val currentVisitMinutes = blocks
                .filter { it.type == BlockType.VISIT || it.type == BlockType.TRAVEL_BUFFER }
                .sumOf { it.durationMinutes }
            if (currentVisitMinutes + totalNeeded > MAX_DAILY_MINUTES) continue

            // Check weekly visit+travel load
            val weeklyVisitMinutes = schedule.dailySchedules.values.sumOf { dayBlocks ->
                dayBlocks.filter { it.type == BlockType.VISIT || it.type == BlockType.TRAVEL_BUFFER }
                    .sumOf { it.durationMinutes }
            }
            if (weeklyVisitMinutes + totalNeeded > MAX_WEEKLY_MINUTES) continue

            // Find office blocks that can be split to accommodate the visit
            val slotTime = findTimeInBlocks(blocks, visit, totalNeeded)
            if (slotTime != null) return Pair(date, slotTime)
        }
        return null
    }

    /** Scan office blocks for a gap where the visit fits */
    private fun findTimeInBlocks(
        blocks: MutableList<TimeBlock>,
        visit: VisitRequest,
        totalNeeded: Int // visit duration + travel buffer
    ): LocalTime? {
        // Find office blocks, try to place visit inside them
        val officeBlocks = blocks.filter { it.type == BlockType.OFFICE }
            .sortedBy { it.start }

        for (office in officeBlocks) {
            // Determine search window
            var searchStart = office.start
            var searchEnd = office.end

            // Respect patient time preferences
            if (visit.preferredTimeStart != null && visit.preferredTimeStart.isAfter(searchStart)) {
                searchStart = visit.preferredTimeStart
            }
            if (visit.preferredTimeEnd != null && visit.preferredTimeEnd.isBefore(searchEnd)) {
                searchEnd = visit.preferredTimeEnd
            }

            // Check if enough room
            val availableMinutes = java.time.Duration.between(searchStart, searchEnd).toMinutes().toInt()
            if (availableMinutes < totalNeeded) continue

            // The visit fits at searchStart
            return searchStart
        }
        return null
    }

    /** Place a visit into the staff schedule, splitting office blocks */
    private fun placeVisitInSchedule(
        visit: VisitRequest,
        schedule: StaffWeekSchedule,
        date: LocalDate,
        startTime: LocalTime
    ) {
        val blocks = schedule.dailySchedules[date]!!
        val visitEnd = startTime.plusMinutes(visit.durationMinutes.toLong())
        val bufferEnd = visitEnd.plusMinutes(TRAVEL_BUFFER_MINUTES.toLong())

        val visitBlock = TimeBlock(
            start = startTime,
            end = visitEnd,
            type = BlockType.VISIT,
            visitData = VisitPlacement(
                patientId = visit.patientId,
                patientName = visit.patientName,
                visitType = visit.visitType,
                durationMinutes = visit.durationMinutes,
                notes = visit.notes,
                location = visit.location,
                priority = visit.priority,
                requirementId = visit.requirementId
            )
        )

        val travelBlock = TimeBlock(
            start = visitEnd,
            end = if (bufferEnd.isAfter(LocalTime.of(WORK_DAY_END_HOUR, 0))) LocalTime.of(WORK_DAY_END_HOUR, 0) else bufferEnd,
            type = BlockType.TRAVEL_BUFFER
        )

        // Find and split the office block containing this time
        val affectedOffice = blocks.firstOrNull {
            it.type == BlockType.OFFICE && !it.start.isAfter(startTime) && !it.end.isBefore(visitEnd)
        }

        if (affectedOffice != null) {
            blocks.remove(affectedOffice)

            // Office before the visit
            if (affectedOffice.start.isBefore(startTime)) {
                val beforeMinutes = java.time.Duration.between(affectedOffice.start, startTime).toMinutes().toInt()
                if (beforeMinutes >= OFFICE_BLOCK_MIN_MINUTES) {
                    blocks.add(TimeBlock(affectedOffice.start, startTime, BlockType.OFFICE))
                }
            }

            // The visit itself
            blocks.add(visitBlock)

            // Travel buffer
            if (travelBlock.durationMinutes > 0 && !travelBlock.end.isAfter(LocalTime.of(WORK_DAY_END_HOUR, 0))) {
                blocks.add(travelBlock)
            }

            // Office after the visit + buffer
            val afterStart = if (travelBlock.durationMinutes > 0 && !travelBlock.end.isAfter(LocalTime.of(WORK_DAY_END_HOUR, 0))) {
                travelBlock.end
            } else {
                visitEnd
            }
            if (afterStart.isBefore(affectedOffice.end)) {
                val afterMinutes = java.time.Duration.between(afterStart, affectedOffice.end).toMinutes().toInt()
                if (afterMinutes >= OFFICE_BLOCK_MIN_MINUTES) {
                    blocks.add(TimeBlock(afterStart, affectedOffice.end, BlockType.OFFICE))
                }
            }

            // Sort blocks by start time
            blocks.sortBy { it.start }
        }
    }

    // ── Step 5: Workload validation ──────────────────────────────

    private fun validateAndFillWorkload(
        schedule: StaffWeekSchedule,
        violations: MutableList<String>
    ) {
        val totalWork = schedule.totalWorkMinutes()

        if (totalWork > MAX_WEEKLY_MINUTES) {
            violations.add(
                "OVERLOADED: ${schedule.staffName} has $totalWork minutes (max $MAX_WEEKLY_MINUTES)"
            )
        }

        if (totalWork < MIN_WEEKLY_MINUTES) {
            // Try to fill with additional office blocks
            val deficit = MIN_WEEKLY_MINUTES - totalWork
            var filled = 0

            for ((date, blocks) in schedule.dailySchedules) {
                if (filled >= deficit) break
                val dayWork = schedule.dayWorkMinutes(date)
                val dayRemaining = MAX_DAILY_MINUTES - dayWork
                if (dayRemaining <= 0) continue

                // Find gaps in the schedule where office blocks can be inserted
                val gaps = findGaps(blocks)
                for (gap in gaps) {
                    if (filled >= deficit) break
                    val gapMinutes = java.time.Duration.between(gap.first, gap.second).toMinutes().toInt()
                    if (gapMinutes < OFFICE_BLOCK_MIN_MINUTES) continue

                    val fillMinutes = minOf(gapMinutes, dayRemaining, deficit - filled)
                    if (fillMinutes >= OFFICE_BLOCK_MIN_MINUTES) {
                        blocks.add(
                            TimeBlock(
                                gap.first,
                                gap.first.plusMinutes(fillMinutes.toLong()),
                                BlockType.OFFICE
                            )
                        )
                        filled += fillMinutes
                    }
                }
                blocks.sortBy { it.start }
            }

            val finalTotal = schedule.totalWorkMinutes()
            if (finalTotal < MIN_WEEKLY_MINUTES) {
                violations.add(
                    "UNDERLOADED: ${schedule.staffName} has $finalTotal minutes after fill (target $MIN_WEEKLY_MINUTES). " +
                    "Day off (${schedule.dayOff}) limits available hours."
                )
            }
        }
    }

    /** Find unoccupied time gaps within working hours */
    private fun findGaps(blocks: List<TimeBlock>): List<Pair<LocalTime, LocalTime>> {
        val sorted = blocks.sortedBy { it.start }
        val gaps = mutableListOf<Pair<LocalTime, LocalTime>>()
        val workStart = LocalTime.of(WORK_DAY_START_HOUR, 0)
        val workEnd = LocalTime.of(WORK_DAY_END_HOUR, 0)

        var cursor = workStart
        for (block in sorted) {
            if (cursor.isBefore(block.start)) {
                gaps.add(Pair(cursor, block.start))
            }
            if (block.end.isAfter(cursor)) {
                cursor = block.end
            }
        }
        if (cursor.isBefore(workEnd)) {
            gaps.add(Pair(cursor, workEnd))
        }
        return gaps
    }
}
