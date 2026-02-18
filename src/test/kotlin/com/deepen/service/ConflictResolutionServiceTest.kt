package com.deepen.service

import com.deepen.dto.AvailabilityCheckRequest
import com.deepen.model.*
import com.deepen.repository.AppointmentRepository
import com.deepen.repository.StaffAvailabilityRepository
import com.deepen.repository.TimeOffRequestRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.time.*

class ConflictResolutionServiceTest {

    @Mock
    private lateinit var appointmentRepository: AppointmentRepository

    @Mock
    private lateinit var staffAvailabilityService: StaffAvailabilityService

    @Mock
    private lateinit var timeOffService: TimeOffService

    @Mock
    private lateinit var patientPreferenceService: PatientPreferenceService

    @InjectMocks
    private lateinit var conflictResolutionService: ConflictResolutionService

    private lateinit var doctor: User
    private lateinit var nurse: User
    private lateinit var patient: User

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Setup test users
        doctor = User(
            id = 1L,
            email = "dr.test@hospital.fi",
            password = "password",
            firstName = "Test",
            lastName = "Doctor",
            role = UserRole.DOCTOR
        )
        
        nurse = User(
            id = 2L,
            email = "nurse.test@hospital.fi",
            password = "password",
            firstName = "Test",
            lastName = "Nurse",
            role = UserRole.NURSE
        )
        
        patient = User(
            id = 1L,
            email = "patient@test.fi",
            password = "password",
            firstName = "Test",
            lastName = "Patient",
            role = UserRole.PATIENT
        )
    }

    @Test
    fun `should return available when no conflicts exist`() {
        // Given
        val request = AvailabilityCheckRequest(
            staffId = doctor.id,
            patientId = patient.id,
            scheduledAt = LocalDateTime.of(2024, 1, 15, 10, 0),
            durationMinutes = 30
        )

        // Mock staff availability (Doctor works 9-16)
        val availability = StaffAvailability(
            staff = doctor,
            dayOfWeek = 1, // Monday
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(16, 0),
            isAvailable = true
        )
        `when`(staffAvailabilityService.findByStaffId(doctor.id))
            .thenReturn(listOf(availability))

        // Mock no time-off
        `when`(timeOffService.isStaffOnTimeOff(doctor.id, LocalDate.of(2024, 1, 15)))
            .thenReturn(false)

        // Mock no existing appointments
        `when`(appointmentRepository.findByStaffIdAndDateRange(
            eq(doctor.id),
            any<LocalDateTime>(),
            any<LocalDateTime>()
        )).thenReturn(emptyList())

        // Mock no patient preferences
        `when`(patientPreferenceService.getPatientPreference(patient.id))
            .thenReturn(null)

        // When
        val response = conflictResolutionService.checkAvailability(request)

        // Then
        assertTrue(response.isAvailable)
        assertTrue(response.conflicts.isEmpty())
        assertTrue(response.alternativeTimes.isEmpty())
    }

    @Test
    fun `should detect conflict when staff not available`() {
        // Given
        val request = AvailabilityCheckRequest(
            staffId = doctor.id,
            patientId = patient.id,
            scheduledAt = LocalDateTime.of(2024, 1, 15, 17, 0), // After working hours
            durationMinutes = 30
        )

        // Mock staff availability (Doctor works 9-16)
        val availability = StaffAvailability(
            staff = doctor,
            dayOfWeek = 1, // Monday
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(16, 0),
            isAvailable = true
        )
        `when`(staffAvailabilityService.findByStaffId(doctor.id))
            .thenReturn(listOf(availability))

        `when`(timeOffService.isStaffOnTimeOff(doctor.id, LocalDate.of(2024, 1, 15)))
            .thenReturn(false)

        `when`(appointmentRepository.findByStaffIdAndDateRange(
            eq(doctor.id),
            any<LocalDateTime>(),
            any<LocalDateTime>()
        )).thenReturn(emptyList())

        `when`(patientPreferenceService.getPatientPreference(patient.id))
            .thenReturn(null)

        // When
        val response = conflictResolutionService.checkAvailability(request)

        // Then
        assertFalse(response.isAvailable)
        assertTrue(response.conflicts.any { it.contains("outside staff working hours") })
        assertTrue(response.alternativeTimes.isNotEmpty())
    }

    @Test
    fun `should detect conflict with existing appointment`() {
        // Given
        val request = AvailabilityCheckRequest(
            staffId = doctor.id,
            patientId = patient.id,
            scheduledAt = LocalDateTime.of(2024, 1, 15, 10, 0),
            durationMinutes = 30
        )

        // Mock staff availability
        val availability = StaffAvailability(
            staff = doctor,
            dayOfWeek = 1,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(16, 0),
            isAvailable = true
        )
        `when`(staffAvailabilityService.findByStaffId(doctor.id))
            .thenReturn(listOf(availability))

        `when`(timeOffService.isStaffOnTimeOff(doctor.id, LocalDate.of(2024, 1, 15)))
            .thenReturn(false)

        // Mock existing appointment that conflicts
        val existingAppointment = Appointment(
            id = 1L,
            patient = patient,
            staff = doctor,
            scheduledAt = LocalDateTime.of(2024, 1, 15, 10, 15),
            estimatedDurationMinutes = 30,
            type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.SCHEDULED
        )
        `when`(appointmentRepository.findByStaffIdAndDateRange(
            eq(doctor.id),
            any<LocalDateTime>(),
            any<LocalDateTime>()
        )).thenReturn(listOf(existingAppointment))

        `when`(patientPreferenceService.getPatientPreference(patient.id))
            .thenReturn(null)

        // When
        val response = conflictResolutionService.checkAvailability(request)

        // Then
        assertFalse(response.isAvailable)
        assertTrue(response.conflicts.any { it.contains("Conflict with existing appointment") })
        assertTrue(response.alternativeTimes.isNotEmpty())
    }

    @Test
    fun `should detect conflict with patient preferences`() {
        // Given
        val request = AvailabilityCheckRequest(
            staffId = doctor.id,
            patientId = patient.id,
            scheduledAt = LocalDateTime.of(2024, 1, 15, 8, 0), // Morning
            durationMinutes = 30
        )

        // Mock staff availability
        val availability = StaffAvailability(
            staff = doctor,
            dayOfWeek = 1,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(16, 0),
            isAvailable = true
        )
        `when`(staffAvailabilityService.findByStaffId(doctor.id))
            .thenReturn(listOf(availability))

        `when`(timeOffService.isStaffOnTimeOff(doctor.id, LocalDate.of(2024, 1, 15)))
            .thenReturn(false)

        `when`(appointmentRepository.findByStaffIdAndDateRange(
            eq(doctor.id),
            any<LocalDateTime>(),
            any<LocalDateTime>()
        )).thenReturn(emptyList())

        // Mock patient preference to avoid mornings
        val patientPreference = PatientPreference(
            patient = patient,
            preferredDayOfWeek = null,
            preferredTimeStart = null,
            preferredTimeEnd = null,
            preferredVisitType = null,
            avoidMornings = true,
            avoidEvenings = false,
            preferredLocation = null,
            notes = null
        )
        `when`(patientPreferenceService.getPatientPreference(patient.id))
            .thenReturn(patientPreference)

        // When
        val response = conflictResolutionService.checkAvailability(request)

        // Then
        assertFalse(response.isAvailable)
        assertTrue(response.conflicts.any { it.contains("Patient prefers to avoid mornings") })
        assertTrue(response.alternativeTimes.isNotEmpty())
    }

    @Test
    fun `should provide alternative time suggestions`() {
        // Given
        val request = AvailabilityCheckRequest(
            staffId = doctor.id,
            patientId = patient.id,
            scheduledAt = LocalDateTime.of(2024, 1, 15, 17, 0), // After hours
            durationMinutes = 30
        )

        // Mock staff availability for the week
        val availabilities = (1..5).map { day ->
            StaffAvailability(
                staff = doctor,
                dayOfWeek = day,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(16, 0),
                isAvailable = true
            )
        }
        `when`(staffAvailabilityService.findByStaffId(doctor.id))
            .thenReturn(availabilities)

        `when`(timeOffService.isStaffOnTimeOff(doctor.id, any()))
            .thenReturn(false)

        `when`(appointmentRepository.findByStaffIdAndDateRange(
            eq(doctor.id),
            any<LocalDateTime>(),
            any<LocalDateTime>()
        )).thenReturn(emptyList())

        // Mock patient prefers afternoons
        val patientPreference = PatientPreference(
            patient = patient,
            preferredDayOfWeek = null,
            preferredTimeStart = LocalTime.of(13, 0),
            preferredTimeEnd = LocalTime.of(16, 0),
            preferredVisitType = null,
            avoidMornings = false,
            avoidEvenings = false,
            preferredLocation = null,
            notes = null
        )
        `when`(patientPreferenceService.getPatientPreference(patient.id))
            .thenReturn(patientPreference)

        // When
        val response = conflictResolutionService.checkAvailability(request)

        // Then
        assertFalse(response.isAvailable)
        assertTrue(response.alternativeTimes.isNotEmpty())
        
        // Verify alternatives are within preferred time range
        val preferredAlternatives = response.alternativeTimes.filter { it.isPreferred }
        assertTrue(preferredAlternatives.isNotEmpty())
        
        // Verify alternatives are sorted by confidence
        val confidences = response.alternativeTimes.map { it.confidence }
        assertEquals(confidences.sortedDescending(), confidences)
    }

    @Test
    fun `should detect time-off conflicts`() {
        // Given
        val request = AvailabilityCheckRequest(
            staffId = doctor.id,
            patientId = patient.id,
            scheduledAt = LocalDateTime.of(2024, 1, 15, 10, 0),
            durationMinutes = 30
        )

        // Mock staff availability
        val availability = StaffAvailability(
            staff = doctor,
            dayOfWeek = 1,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(16, 0),
            isAvailable = true
        )
        `when`(staffAvailabilityService.findByStaffId(doctor.id))
            .thenReturn(listOf(availability))

        // Mock staff on time-off
        `when`(timeOffService.isStaffOnTimeOff(doctor.id, LocalDate.of(2024, 1, 15)))
            .thenReturn(true)

        `when`(appointmentRepository.findByStaffIdAndDateRange(
            eq(doctor.id),
            any<LocalDateTime>(),
            any<LocalDateTime>()
        )).thenReturn(emptyList())

        `when`(patientPreferenceService.getPatientPreference(patient.id))
            .thenReturn(null)

        // When
        val response = conflictResolutionService.checkAvailability(request)

        // Then
        assertFalse(response.isAvailable)
        assertTrue(response.conflicts.any { it.contains("Staff on time-off") })
        assertTrue(response.alternativeTimes.isNotEmpty())
    }
}
