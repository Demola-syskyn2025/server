package com.deepen.controller

import com.deepen.dto.CreateRescheduleRequest
import com.deepen.dto.RescheduleRequestDto
import com.deepen.dto.ReviewRescheduleRequest
import com.deepen.service.RescheduleService
import com.deepen.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reschedule")
class RescheduleController(
    private val rescheduleService: RescheduleService,
    private val userService: UserService
) {

    private fun getUserId(authentication: Authentication): Long {
        val email = authentication.name
        return userService.findByEmail(email)?.id
            ?: throw IllegalArgumentException("User not found")
    }

    @GetMapping("/appointment/{appointmentId}")
    fun getByAppointment(@PathVariable appointmentId: Long): ResponseEntity<List<RescheduleRequestDto>> {
        val requests = rescheduleService.findByAppointmentId(appointmentId)
        return ResponseEntity.ok(requests.map { rescheduleService.toDto(it) })
    }

    @GetMapping("/my-requests")
    fun getMyRequests(authentication: Authentication): ResponseEntity<List<RescheduleRequestDto>> {
        val userId = getUserId(authentication)
        val requests = rescheduleService.findByRequestedById(userId)
        return ResponseEntity.ok(requests.map { rescheduleService.toDto(it) })
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun getPendingRequests(authentication: Authentication): ResponseEntity<List<RescheduleRequestDto>> {
        val staffId = getUserId(authentication)
        val requests = rescheduleService.findPendingByStaffId(staffId)
        return ResponseEntity.ok(requests.map { rescheduleService.toDto(it) })
    }

    @PostMapping
    fun createRequest(
        @RequestBody request: CreateRescheduleRequest,
        authentication: Authentication
    ): ResponseEntity<RescheduleRequestDto> {
        val userId = getUserId(authentication)
        val reschedule = rescheduleService.createRequest(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(rescheduleService.toDto(reschedule))
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun reviewRequest(
        @PathVariable id: Long,
        @RequestBody request: ReviewRescheduleRequest
    ): ResponseEntity<RescheduleRequestDto> {
        val reschedule = rescheduleService.reviewRequest(id, request)
        return ResponseEntity.ok(rescheduleService.toDto(reschedule))
    }
}
