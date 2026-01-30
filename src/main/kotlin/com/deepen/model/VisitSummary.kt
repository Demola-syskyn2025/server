package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "visit_summaries")
data class VisitSummary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    val appointment: Appointment,
    
    @Column(nullable = false, length = 2000)
    val summary: String,
    
    @Column(length = 1000)
    val recommendations: String? = null,
    
    @Column(length = 1000)
    val medications: String? = null,
    
    val nextVisitRecommendation: LocalDateTime? = null,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User
)
