package com.deepen.repository

import com.deepen.model.StaffProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StaffProfileRepository : JpaRepository<StaffProfile, Long> {
    fun findByUserId(userId: Long): StaffProfile?
}
