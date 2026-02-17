package com.deepen.controller

import com.deepen.dto.*
import com.deepen.service.StaffAvailabilityService
import com.deepen.service.StaffProfileService
import com.deepen.service.TimeOffService
import com.deepen.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
class StaffController(
    private val staffProfileService: StaffProfileService,
    private val staffAvailabilityService: StaffAvailabilityService,
    private val timeOffService: TimeOffService,
    private val userService: UserService
) {

    private fun getUserId(authentication: Authentication): Long {
        val email = authentication.name
        return userService.findByEmail(email)?.id
            ?: throw IllegalArgumentException("User not found")
    }

    // ========== Staff Profile ==========

    @GetMapping("/profile/{userId}")
    fun getProfile(@PathVariable userId: Long): ResponseEntity<StaffProfileDto> {
        val profile = staffProfileService.findByUserId(userId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(staffProfileService.toDto(profile))
    }

    @PostMapping("/profile")
    fun createProfile(
        @RequestBody request: CreateStaffProfileRequest,
        authentication: Authentication
    ): ResponseEntity<StaffProfileDto> {
        val userId = getUserId(authentication)
        val profile = staffProfileService.createProfile(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(staffProfileService.toDto(profile))
    }

    @PatchMapping("/profile")
    fun updateProfile(
        @RequestBody request: UpdateStaffProfileRequest,
        authentication: Authentication
    ): ResponseEntity<StaffProfileDto> {
        val userId = getUserId(authentication)
        val profile = staffProfileService.updateProfile(userId, request)
        return ResponseEntity.ok(staffProfileService.toDto(profile))
    }

    // ========== Staff Availability ==========

    @GetMapping("/availability/{staffId}")
    fun getAvailability(@PathVariable staffId: Long): ResponseEntity<List<StaffAvailabilityDto>> {
        val availability = staffAvailabilityService.findByStaffId(staffId)
        return ResponseEntity.ok(availability.map { staffAvailabilityService.toDto(it) })
    }

    @PostMapping("/availability")
    fun setAvailability(
        @RequestBody request: SetAvailabilityRequest,
        authentication: Authentication
    ): ResponseEntity<StaffAvailabilityDto> {
        val staffId = getUserId(authentication)
        val availability = staffAvailabilityService.setAvailability(staffId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(staffAvailabilityService.toDto(availability))
    }

    @PostMapping("/availability/bulk")
    fun bulkSetAvailability(
        @RequestBody request: BulkSetAvailabilityRequest,
        authentication: Authentication
    ): ResponseEntity<List<StaffAvailabilityDto>> {
        val staffId = getUserId(authentication)
        val availability = staffAvailabilityService.bulkSetAvailability(staffId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(availability.map { staffAvailabilityService.toDto(it) })
    }

    @DeleteMapping("/availability/{id}")
    fun removeAvailability(@PathVariable id: Long): ResponseEntity<Void> {
        staffAvailabilityService.removeAvailability(id)
        return ResponseEntity.noContent().build()
    }

    // ========== Time Off ==========

    @GetMapping("/time-off")
    fun getMyTimeOff(authentication: Authentication): ResponseEntity<List<TimeOffRequestDto>> {
        val staffId = getUserId(authentication)
        val requests = timeOffService.findByStaffId(staffId)
        return ResponseEntity.ok(requests.map { timeOffService.toDto(it) })
    }

    @GetMapping("/time-off/pending")
    fun getPendingTimeOff(): ResponseEntity<List<TimeOffRequestDto>> {
        val requests = timeOffService.findPending()
        return ResponseEntity.ok(requests.map { timeOffService.toDto(it) })
    }

    @PostMapping("/time-off")
    fun requestTimeOff(
        @RequestBody request: CreateTimeOffRequest,
        authentication: Authentication
    ): ResponseEntity<TimeOffRequestDto> {
        val staffId = getUserId(authentication)
        val timeOff = timeOffService.createRequest(staffId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(timeOffService.toDto(timeOff))
    }

    @PatchMapping("/time-off/{id}/review")
    fun reviewTimeOff(
        @PathVariable id: Long,
        @RequestBody request: ReviewTimeOffRequest,
        authentication: Authentication
    ): ResponseEntity<TimeOffRequestDto> {
        val reviewerId = getUserId(authentication)
        val timeOff = timeOffService.reviewRequest(id, reviewerId, request)
        return ResponseEntity.ok(timeOffService.toDto(timeOff))
    }
}
