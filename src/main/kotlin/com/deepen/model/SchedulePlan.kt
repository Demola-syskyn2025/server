package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

enum class PlanStatus {
    DRAFT,
    CONFIRMED
}

@Entity
@Table(
    name = "schedule_plans",
    uniqueConstraints = [UniqueConstraint(columnNames = ["week_start_date", "status"])]
)
data class SchedulePlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "week_start_date", nullable = false)
    val weekStartDate: LocalDate, // Always a Monday

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: PlanStatus = PlanStatus.DRAFT,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    val createdBy: User? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    val confirmedAt: LocalDateTime? = null
)
