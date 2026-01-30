package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

enum class TaskStatus {
    PENDING,
    COMPLETED,
    SKIPPED
}

enum class TaskFrequency {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY
}

@Entity
@Table(name = "care_tasks")
data class CareTask(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    val patient: User,
    
    @Column(nullable = false)
    val title: String,
    
    @Column(length = 500)
    val description: String? = null,
    
    @Column(nullable = false)
    val dueDate: LocalDate,
    
    val dueTime: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TaskStatus = TaskStatus.PENDING,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val frequency: TaskFrequency = TaskFrequency.ONCE,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val completedAt: LocalDateTime? = null
)
