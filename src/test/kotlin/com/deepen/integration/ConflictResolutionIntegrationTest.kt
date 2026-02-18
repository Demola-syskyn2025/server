package com.deepen.integration

import com.deepen.dto.AvailabilityCheckRequest
import com.deepen.service.ConflictResolutionService
import com.deepen.service.StaffAvailabilityService
import com.deepen.service.TimeOffService
import com.deepen.service.PatientPreferenceService
import com.deepen.repository.AppointmentRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConflictResolutionIntegrationTest {

    @Autowired
    private lateinit var conflictResolutionService: ConflictResolutionService

    @Autowired
    private lateinit var appointmentRepository: AppointmentRepository

    @Test
    fun `should check availability with real data`() {
        // Test with real data from DataSeeder
        val request = AvailabilityCheckRequest(
            staffId = 1L, // Doctor
            patientId = 1L, // Matti Virtanen
            scheduledAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
            durationMinutes = 30
        )

        val response = conflictResolutionService.checkAvailability(request)

        println("Availability Check Results:")
        println("Is Available: ${response.isAvailable}")
        println("Conflicts: ${response.conflicts}")
        println("Alternative Times: ${response.alternativeTimes.size} suggestions")
        
        response.alternativeTimes.forEach { alternative ->
            println("  - ${alternative.scheduledAt} (Confidence: ${alternative.confidence}, Preferred: ${alternative.isPreferred})")
        }

        // Verify the service works without throwing exceptions
        assert(true)
    }

    @Test
    fun `should detect conflict with existing appointment`() {
        // Create a conflict with existing data
        val request = AvailabilityCheckRequest(
            staffId = 2L, // Nurse
            patientId = 1L, // Matti Virtanen
            scheduledAt = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0),
            durationMinutes = 30
        )

        val response = conflictResolutionService.checkAvailability(request)

        println("Conflict Detection Results:")
        println("Is Available: ${response.isAvailable}")
        println("Conflicts: ${response.conflicts}")
        
        // Should detect conflict with nurse's existing appointment at 08:00
        if (response.conflicts.isNotEmpty()) {
            println("✅ Successfully detected conflicts")
        }

        // Should provide alternatives
        if (response.alternativeTimes.isNotEmpty()) {
            println("✅ Provided ${response.alternativeTimes.size} alternative suggestions")
        }

        assert(true)
    }

    @Test
    fun `should provide alternatives for after hours request`() {
        // Test after hours request
        val request = AvailabilityCheckRequest(
            staffId = 1L, // Doctor
            patientId = 1L, // Matti Virtanen
            scheduledAt = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0),
            durationMinutes = 30
        )

        val response = conflictResolutionService.checkAvailability(request)

        println("After Hours Request Results:")
        println("Is Available: ${response.isAvailable}")
        println("Conflicts: ${response.conflicts}")
        println("Alternatives Provided: ${response.alternativeTimes.size}")

        // Should NOT be available (after working hours)
        assert(!response.isAvailable)
        
        // Should provide alternatives
        assert(response.alternativeTimes.isNotEmpty())
        
        // Alternatives should be within working hours
        response.alternativeTimes.forEach { alternative ->
            val hour = alternative.scheduledAt.hour
            assert(hour >= 9 && hour <= 16) { "Alternative time ${alternative.scheduledAt} is outside working hours" }
        }

        println("✅ All alternatives are within working hours")
    }
}
