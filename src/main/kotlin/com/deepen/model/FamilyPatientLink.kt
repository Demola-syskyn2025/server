package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "family_patient_links",
    uniqueConstraints = [UniqueConstraint(columnNames = ["family_member_id", "patient_id"])]
)
data class FamilyPatientLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id", nullable = false)
    val familyMember: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    val patient: User,

    @Column(nullable = false)
    val relationship: String = "Family Member",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
