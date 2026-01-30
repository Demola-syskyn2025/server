package com.deepen.controller

import com.deepen.dto.CareTaskDto
import com.deepen.dto.CreateCareTaskRequest
import com.deepen.dto.UpdateCareTaskRequest
import com.deepen.service.CareTaskService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/tasks")
class CareTaskController(
    private val careTaskService: CareTaskService
) {
    
    @GetMapping("/{id}")
    fun getTask(@PathVariable id: Long): ResponseEntity<CareTaskDto> {
        val task = careTaskService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(careTaskService.toDto(task))
    }
    
    @GetMapping("/patient/{patientId}")
    fun getPatientTasks(@PathVariable patientId: Long): ResponseEntity<List<CareTaskDto>> {
        val tasks = careTaskService.findByPatientId(patientId)
        return ResponseEntity.ok(tasks.map { careTaskService.toDto(it) })
    }
    
    @GetMapping("/patient/{patientId}/date/{date}")
    fun getPatientTasksByDate(
        @PathVariable patientId: Long,
        @PathVariable date: LocalDate
    ): ResponseEntity<List<CareTaskDto>> {
        val tasks = careTaskService.findByPatientIdAndDate(patientId, date)
        return ResponseEntity.ok(tasks.map { careTaskService.toDto(it) })
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE')")
    fun createTask(@RequestBody request: CreateCareTaskRequest): ResponseEntity<CareTaskDto> {
        val task = careTaskService.createTask(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(careTaskService.toDto(task))
    }
    
    @PatchMapping("/{id}")
    fun updateTask(
        @PathVariable id: Long,
        @RequestBody request: UpdateCareTaskRequest
    ): ResponseEntity<CareTaskDto> {
        val task = careTaskService.updateTask(id, request)
        return ResponseEntity.ok(careTaskService.toDto(task))
    }
    
    @PostMapping("/{id}/complete")
    fun completeTask(@PathVariable id: Long): ResponseEntity<CareTaskDto> {
        val task = careTaskService.completeTask(id)
        return ResponseEntity.ok(careTaskService.toDto(task))
    }
}
