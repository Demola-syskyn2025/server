package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class RescheduleStatus {
    PENDING,
    APPROVED,
    REJECTED,
    ALTERNATIVE_OFFERED
}

enum class RequestType {
    RESCHEDULE,
    CANCEL
}

@Entity
@Table(name = "reschedule_requests")
data class RescheduleRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    val appointment: Appointment,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    val requestedBy: User,

    @Column(length = 500)
    val reason: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val requestType: RequestType = RequestType.RESCHEDULE,

    val preferredDate1: LocalDateTime? = null,

    val preferredDate2: LocalDateTime? = null,

    val preferredDate3: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: RescheduleStatus = RescheduleStatus.PENDING,

    @Column(length = 500)
    val staffResponse: String? = null,

    val newScheduledAt: LocalDateTime? = null,

    @Column(nullable = false)
    val requestedAt: LocalDateTime = LocalDateTime.now(),

    val reviewedAt: LocalDateTime? = null
)
