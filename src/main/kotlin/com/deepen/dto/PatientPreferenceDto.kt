package com.deepen.dto

import com.deepen.model.AppointmentType
import java.time.DayOfWeek
import java.time.LocalTime
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotNull

data class PatientPreferenceDto(
    @field:NotNull
    val patientId: Long,
    
    val preferredDayOfWeek: DayOfWeek?,
    
    val preferredTimeStart: LocalTime?,
    
    val preferredTimeEnd: LocalTime?,
    
    val preferredVisitType: AppointmentType?,
    
    val avoidMornings: Boolean = false,
    
    val avoidEvenings: Boolean = false,
    
    val preferredLocation: String?,
    
    val notes: String?
)
