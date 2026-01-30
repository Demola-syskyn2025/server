package com.deepen.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class UserRole {
    PATIENT,
    FAMILY_MEMBER,
    DOCTOR,
    NURSE
}

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val email: String,
    
    @Column(nullable = false)
    val password: String,
    
    @Column(nullable = false)
    val firstName: String,
    
    @Column(nullable = false)
    val lastName: String,
    
    @Column(nullable = false)
    val phoneNumber: String = "",
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
