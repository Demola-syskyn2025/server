package com.deepen.repository

import com.deepen.model.CareTask
import com.deepen.model.TaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface CareTaskRepository : JpaRepository<CareTask, Long> {
    fun findByPatientId(patientId: Long): List<CareTask>
    fun findByPatientIdAndStatus(patientId: Long, status: TaskStatus): List<CareTask>
    fun findByPatientIdAndDueDate(patientId: Long, dueDate: LocalDate): List<CareTask>
    fun findByPatientIdAndDueDateBetween(patientId: Long, startDate: LocalDate, endDate: LocalDate): List<CareTask>
}
