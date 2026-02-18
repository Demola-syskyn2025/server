package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "care_assignments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["patient_id", "staff_id"])]
)
data class CareAssignment(
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
    val isPrimary: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
