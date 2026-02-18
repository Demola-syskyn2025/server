package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

enum class TimeOffStatus {
    PENDING,
    APPROVED,
    REJECTED
}

@Entity
@Table(name = "time_off_requests")
data class TimeOffRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    val staff: User,

    @Column(nullable = false)
    val startDate: LocalDate,

    @Column(nullable = false)
    val endDate: LocalDate,

    @Column(length = 500)
    val reason: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TimeOffStatus = TimeOffStatus.PENDING,

    @Column(nullable = false)
    val requestedAt: LocalDateTime = LocalDateTime.now(),

    val reviewedAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    val reviewedBy: User? = null
)
