package com.deepen.dto

import java.time.LocalDateTime

data class FamilyPatientLinkDto(
    val id: Long,
    val familyMember: UserDto,
    val patient: UserDto,
    val relationship: String,
    val createdAt: LocalDateTime
)

data class CreateFamilyPatientLinkRequest(
    val patientId: Long,
    val relationship: String = "Family Member"
)
