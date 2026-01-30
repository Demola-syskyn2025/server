package com.deepen.dto

import com.deepen.model.TaskFrequency
import com.deepen.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateCareTaskRequest(
    val patientId: Long,
    val title: String,
    val description: String? = null,
    val dueDate: LocalDate,
    val dueTime: String? = null,
    val frequency: TaskFrequency = TaskFrequency.ONCE
)

data class UpdateCareTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val dueDate: LocalDate? = null,
    val dueTime: String? = null,
    val status: TaskStatus? = null
)

data class CareTaskDto(
    val id: Long,
    val patientId: Long,
    val title: String,
    val description: String?,
    val dueDate: LocalDate,
    val dueTime: String?,
    val status: TaskStatus,
    val frequency: TaskFrequency,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?
)
