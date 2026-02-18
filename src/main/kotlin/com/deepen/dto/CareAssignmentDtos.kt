package com.deepen.dto

import java.time.LocalDateTime

data class CreateCareAssignmentRequest(
    val patientId: Long,
    val staffId: Long,
    val isPrimary: Boolean = true
)

data class CareAssignmentDto(
    val id: Long,
    val patient: UserDto,
    val staff: UserDto,
    val isPrimary: Boolean,
    val createdAt: LocalDateTime
)
