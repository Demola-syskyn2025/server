package com.deepen.model

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalTime

@Entity
@Table(name = "patient_preferences")
data class PatientPreference(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    val patient: User,

    @Column(name = "preferred_day_of_week")
    val preferredDayOfWeek: DayOfWeek?,

    @Column(name = "preferred_time_start")
    val preferredTimeStart: LocalTime?,

    @Column(name = "preferred_time_end")
    val preferredTimeEnd: LocalTime?,

    @Column(name = "preferred_visit_type")
    val preferredVisitType: AppointmentType?,

    @Column(name = "avoid_mornings")
    val avoidMornings: Boolean = false,

    @Column(name = "avoid_evenings")
    val avoidEvenings: Boolean = false,

    @Column(name = "preferred_location")
    val preferredLocation: String?,

    @Column(name = "notes")
    val notes: String?,

    @Column(name = "created_at")
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)
