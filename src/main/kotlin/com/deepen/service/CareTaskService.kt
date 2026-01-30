package com.deepen.service

import com.deepen.dto.CareTaskDto
import com.deepen.dto.CreateCareTaskRequest
import com.deepen.dto.UpdateCareTaskRequest
import com.deepen.model.CareTask
import com.deepen.model.TaskStatus
import com.deepen.repository.CareTaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class CareTaskService(
    private val careTaskRepository: CareTaskRepository,
    private val userService: UserService
) {
    
    fun findById(id: Long): CareTask? = careTaskRepository.findById(id).orElse(null)
    
    fun findByPatientId(patientId: Long): List<CareTask> = careTaskRepository.findByPatientId(patientId)
    
    fun findByPatientIdAndDate(patientId: Long, date: LocalDate): List<CareTask> =
        careTaskRepository.findByPatientIdAndDueDate(patientId, date)
    
    fun findByPatientIdAndDateRange(patientId: Long, startDate: LocalDate, endDate: LocalDate): List<CareTask> =
        careTaskRepository.findByPatientIdAndDueDateBetween(patientId, startDate, endDate)
    
    @Transactional
    fun createTask(request: CreateCareTaskRequest): CareTask {
        val patient = userService.findById(request.patientId)
            ?: throw IllegalArgumentException("Patient not found")
        
        val task = CareTask(
            patient = patient,
            title = request.title,
            description = request.description,
            dueDate = request.dueDate,
            dueTime = request.dueTime,
            frequency = request.frequency
        )
        
        return careTaskRepository.save(task)
    }
    
    @Transactional
    fun updateTask(id: Long, request: UpdateCareTaskRequest): CareTask {
        val task = findById(id) ?: throw IllegalArgumentException("Task not found")
        
        val updated = task.copy(
            title = request.title ?: task.title,
            description = request.description ?: task.description,
            dueDate = request.dueDate ?: task.dueDate,
            dueTime = request.dueTime ?: task.dueTime,
            status = request.status ?: task.status,
            completedAt = if (request.status == TaskStatus.COMPLETED) LocalDateTime.now() else task.completedAt
        )
        
        return careTaskRepository.save(updated)
    }
    
    @Transactional
    fun completeTask(id: Long): CareTask {
        val task = findById(id) ?: throw IllegalArgumentException("Task not found")
        val updated = task.copy(status = TaskStatus.COMPLETED, completedAt = LocalDateTime.now())
        return careTaskRepository.save(updated)
    }
    
    fun toDto(task: CareTask): CareTaskDto = CareTaskDto(
        id = task.id,
        patientId = task.patient.id,
        title = task.title,
        description = task.description,
        dueDate = task.dueDate,
        dueTime = task.dueTime,
        status = task.status,
        frequency = task.frequency,
        createdAt = task.createdAt,
        completedAt = task.completedAt
    )
}
