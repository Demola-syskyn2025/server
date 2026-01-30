package com.deepen.repository

import com.deepen.model.User
import com.deepen.model.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun findByRole(role: UserRole): List<User>
    fun findByRoleIn(roles: List<UserRole>): List<User>
}
