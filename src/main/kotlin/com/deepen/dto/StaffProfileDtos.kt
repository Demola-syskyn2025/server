package com.deepen.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class CreateStaffProfileRequest(
    val department: String? = null,
    val specialization: String? = null,
    val licenseNumber: String? = null,
    val hireDate: LocalDate? = null
)

data class UpdateStaffProfileRequest(
    val department: String? = null,
    val specialization: String? = null,
    val licenseNumber: String? = null,
    val hireDate: LocalDate? = null
)

data class StaffProfileDto(
    val id: Long,
    val user: UserDto,
    val department: String?,
    val specialization: String?,
    val licenseNumber: String?,
    val hireDate: LocalDate?,
    val createdAt: LocalDateTime
)
