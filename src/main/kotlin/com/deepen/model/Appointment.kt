package com.deepen.model



import jakarta.persistence.*

import java.time.LocalDateTime



enum class AppointmentStatus {

    SCHEDULED,

    CONFIRMED,

    IN_PROGRESS,

    COMPLETED,

    CANCELLED,

    RESCHEDULED

}



enum class AppointmentType {

    HOME_VISIT,

    HOSPITAL_VISIT,

    TELECONSULTATION

}



@Entity

@Table(name = "appointments")

data class Appointment(

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    val id: Long = 0,

    

    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "patient_id", nullable = false)

    val patient: User,

    

    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "staff_id", nullable = false)

    val staff: User,

    

    @Column(nullable = false)

    val scheduledAt: LocalDateTime,

    

    @Column(nullable = false)

    val estimatedDurationMinutes: Int = 30,

    

    @Enumerated(EnumType.STRING)

    @Column(nullable = false)

    val type: AppointmentType,

    

    @Enumerated(EnumType.STRING)

    @Column(nullable = false)

    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,

    

    @Column(length = 1000)

    val notes: String? = null,

    

    val location: String? = null,

    

    @Column(nullable = false)

    val createdAt: LocalDateTime = LocalDateTime.now(),

    

    val updatedAt: LocalDateTime = LocalDateTime.now()

)

