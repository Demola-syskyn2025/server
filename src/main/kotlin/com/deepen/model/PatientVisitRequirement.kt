package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.LocalTime

enum class VisitPriority {
    URGENT,
    HIGH,
    ROUTINE
}

@Entity
@Table(name = "patient_visit_requirements")
data class PatientVisitRequirement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    val patient: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val priority: VisitPriority = VisitPriority.ROUTINE,

    @Column(nullable = false)
    val visitsPerWeek: Int = 1,

    @Column(nullable = false)
    val durationMinutes: Int = 30,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val visitType: AppointmentType = AppointmentType.HOME_VISIT,

    val preferredTimeStart: LocalTime? = null,

    val preferredTimeEnd: LocalTime? = null,

    val location: String? = null,

    @Column(length = 1000)
    val notes: String? = null,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
