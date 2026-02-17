package com.deepen.dto

import java.time.LocalTime

data class SetAvailabilityRequest(
    val dayOfWeek: Int, // 0=Sunday, 1=Monday, ..., 6=Saturday
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isAvailable: Boolean = true
)

data class BulkSetAvailabilityRequest(
    val slots: List<SetAvailabilityRequest>
)

data class StaffAvailabilityDto(
    val id: Long,
    val staffId: Long,
    val staffName: String,
    val dayOfWeek: Int,
    val dayName: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isAvailable: Boolean
)
