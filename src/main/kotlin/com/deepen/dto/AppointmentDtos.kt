package com.deepen.dto



import com.deepen.model.AppointmentStatus

import com.deepen.model.AppointmentType

import java.time.LocalDateTime



data class CreateAppointmentRequest(

    val patientId: Long,

    val staffId: Long,

    val scheduledAt: LocalDateTime,

    val estimatedDurationMinutes: Int = 30,

    val type: AppointmentType,

    val notes: String? = null,

    val location: String? = null

)



data class UpdateAppointmentRequest(

    val scheduledAt: LocalDateTime? = null,

    val status: AppointmentStatus? = null,

    val notes: String? = null,

    val location: String? = null

)



data class AppointmentDto(

    val id: Long,

    val patient: UserDto,

    val staff: UserDto,

    val scheduledAt: LocalDateTime,

    val estimatedDurationMinutes: Int,

    val type: AppointmentType,

    val status: AppointmentStatus,

    val notes: String?,

    val location: String?,

    val createdAt: LocalDateTime

)

