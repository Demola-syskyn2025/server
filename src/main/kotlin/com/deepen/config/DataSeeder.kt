package com.deepen.config

import com.deepen.model.*
import com.deepen.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Configuration
@Profile("dev", "default")
class DataSeeder {

    // Helper to create a clean datetime at a specific hour:minute
    private fun dateAt(daysOffset: Long, hour: Int, minute: Int = 0): LocalDateTime =
        LocalDateTime.now().plusDays(daysOffset).withHour(hour).withMinute(minute).withSecond(0).withNano(0)

    // Helper to create a recurring series
    private fun seedRecurringSeries(
        repo: AppointmentRepository,
        patient: User,
        staff: User,
        firstDate: LocalDateTime,
        durationMin: Int,
        type: AppointmentType,
        frequency: RecurringFrequency,
        endDate: LocalDate,
        notes: String,
        location: String? = null
    ): List<Appointment> {
        val groupId = UUID.randomUUID().toString()
        val list = mutableListOf<Appointment>()
        var current = firstDate
        while (!current.toLocalDate().isAfter(endDate)) {
            list.add(repo.save(Appointment(
                patient = patient, staff = staff, scheduledAt = current,
                estimatedDurationMinutes = durationMin, type = type,
                status = AppointmentStatus.SCHEDULED, notes = notes, location = location,
                recurringGroupId = groupId, recurringFrequency = frequency, recurringEndDate = endDate
            )))
            current = when (frequency) {
                RecurringFrequency.WEEKLY -> current.plusWeeks(1)
                RecurringFrequency.BIWEEKLY -> current.plusWeeks(2)
                RecurringFrequency.MONTHLY -> current.plusMonths(1)
            }
        }
        return list
    }

    @Bean
    fun seedData(
        userRepository: UserRepository,
        appointmentRepository: AppointmentRepository,
        careTaskRepository: CareTaskRepository,
        visitSummaryRepository: VisitSummaryRepository,
        careAssignmentRepository: CareAssignmentRepository,
        staffProfileRepository: StaffProfileRepository,
        staffAvailabilityRepository: StaffAvailabilityRepository,
        timeOffRequestRepository: TimeOffRequestRepository,
        rescheduleRequestRepository: RescheduleRequestRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner = CommandLineRunner {

        if (userRepository.count() > 0) return@CommandLineRunner
        val pw = passwordEncoder.encode("password123")

        // ================================================================
        //  STAFF (1 Doctor, 1 Nurse)
        // ================================================================
        val doctor = userRepository.save(User(
            email = "dr.smith@hospital.fi", password = pw,
            firstName = "John", lastName = "Smith",
            phoneNumber = "+358401234567", role = UserRole.DOCTOR
        ))
        val nurse = userRepository.save(User(
            email = "nurse.jones@hospital.fi", password = pw,
            firstName = "Sarah", lastName = "Jones",
            phoneNumber = "+358401234568", role = UserRole.NURSE
        ))

        // ================================================================
        //  PATIENTS (8) — realistic home-care caseload
        // ================================================================
        val p1 = userRepository.save(User(
            email = "matti.virtanen@email.com", password = pw,
            firstName = "Matti", lastName = "Virtanen",
            phoneNumber = "+358401234569", role = UserRole.PATIENT
        )) // Post-surgery recovery

        val p2 = userRepository.save(User(
            email = "liisa.korhonen@email.com", password = pw,
            firstName = "Liisa", lastName = "Korhonen",
            phoneNumber = "+358401234570", role = UserRole.PATIENT
        )) // Diabetes management

        val p3 = userRepository.save(User(
            email = "antti.makinen@email.com", password = pw,
            firstName = "Antti", lastName = "Mäkinen",
            phoneNumber = "+358401234572", role = UserRole.PATIENT
        )) // Wound care (leg ulcer)

        val p4 = userRepository.save(User(
            email = "sanna.laine@email.com", password = pw,
            firstName = "Sanna", lastName = "Laine",
            phoneNumber = "+358401234573", role = UserRole.PATIENT
        )) // Palliative care

        val p5 = userRepository.save(User(
            email = "jukka.nieminen@email.com", password = pw,
            firstName = "Jukka", lastName = "Nieminen",
            phoneNumber = "+358401234574", role = UserRole.PATIENT
        )) // Chronic heart failure

        val p6 = userRepository.save(User(
            email = "heli.jarvinen@email.com", password = pw,
            firstName = "Heli", lastName = "Järvinen",
            phoneNumber = "+358401234575", role = UserRole.PATIENT
        )) // Post-hip replacement

        val p7 = userRepository.save(User(
            email = "mikko.salonen@email.com", password = pw,
            firstName = "Mikko", lastName = "Salonen",
            phoneNumber = "+358401234576", role = UserRole.PATIENT
        )) // COPD management

        val p8 = userRepository.save(User(
            email = "tuula.hakala@email.com", password = pw,
            firstName = "Tuula", lastName = "Hakala",
            phoneNumber = "+358401234577", role = UserRole.PATIENT
        )) // Elderly routine monitoring

        // ================================================================
        //  FAMILY MEMBERS (2)
        // ================================================================
        userRepository.save(User(
            email = "pekka.virtanen@email.com", password = pw,
            firstName = "Pekka", lastName = "Virtanen",
            phoneNumber = "+358401234571", role = UserRole.FAMILY_MEMBER
        )) // Son of Matti

        userRepository.save(User(
            email = "anna.korhonen@email.com", password = pw,
            firstName = "Anna", lastName = "Korhonen",
            phoneNumber = "+358401234578", role = UserRole.FAMILY_MEMBER
        )) // Daughter of Liisa

        // ================================================================
        //  STAFF PROFILES
        // ================================================================
        staffProfileRepository.save(StaffProfile(
            user = doctor, department = "Home Care",
            specialization = "General Practice / Internal Medicine",
            licenseNumber = "FI-DOC-2020-1234", hireDate = LocalDate.of(2020, 3, 15)
        ))
        staffProfileRepository.save(StaffProfile(
            user = nurse, department = "Home Care",
            specialization = "Wound Care & Chronic Disease Monitoring",
            licenseNumber = "FI-NUR-2019-5678", hireDate = LocalDate.of(2019, 8, 1)
        ))

        // ================================================================
        //  CARE ASSIGNMENTS
        //  Doctor → all 8 patients (primary physician)
        //  Nurse  → 6 patients (those needing frequent hands-on care)
        // ================================================================
        val allPatients = listOf(p1, p2, p3, p4, p5, p6, p7, p8)
        val nursePatients = listOf(p1, p2, p3, p4, p5, p6) // nurse handles 6

        allPatients.forEach { p ->
            careAssignmentRepository.save(CareAssignment(patient = p, staff = doctor, isPrimary = true))
        }
        nursePatients.forEach { p ->
            careAssignmentRepository.save(CareAssignment(patient = p, staff = nurse, isPrimary = true))
        }

        // ================================================================
        //  DOCTOR AVAILABILITY - Real Home Care Schedule
        //  Mon-Fri: 8:00-10:00 Hospital Rounds, 10:00-12:00 Clinic, 1:00-2:00 Admin, 2:00-5:00 Home Visits
        //  Sat: Emergency on-call only
        //  Sun: Off
        // ================================================================
        for (day in 1..5) { // Monday-Friday
            // Morning: Hospital Rounds (8:00-10:00) - NOT available for appointments
            staffAvailabilityRepository.save(StaffAvailability(
                staff = doctor, dayOfWeek = day,
                startTime = LocalTime.of(8, 0), endTime = LocalTime.of(10, 0), isAvailable = false
            ))
            
            // Mid-morning: Clinic Appointments (10:00-12:00) - Available for clinic visits
            staffAvailabilityRepository.save(StaffAvailability(
                staff = doctor, dayOfWeek = day,
                startTime = LocalTime.of(10, 0), endTime = LocalTime.of(12, 0), isAvailable = true
            ))
            
            // Lunch: 12:00-13:00 - NOT available
            staffAvailabilityRepository.save(StaffAvailability(
                staff = doctor, dayOfWeek = day,
                startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0), isAvailable = false
            ))
            
            // Afternoon: Admin work (13:00-14:00) - NOT available for appointments
            staffAvailabilityRepository.save(StaffAvailability(
                staff = doctor, dayOfWeek = day,
                startTime = LocalTime.of(13, 0), endTime = LocalTime.of(14, 0), isAvailable = false
            ))
            
            // Late afternoon: Home Visits (14:00-17:00) - Available for home visits
            staffAvailabilityRepository.save(StaffAvailability(
                staff = doctor, dayOfWeek = day,
                startTime = LocalTime.of(14, 0), endTime = LocalTime.of(17, 0), isAvailable = true
            ))
        }
        
        // Saturday: Emergency on-call (8:00-18:00) - Limited availability
        staffAvailabilityRepository.save(StaffAvailability(
            staff = doctor, dayOfWeek = 6,
            startTime = LocalTime.of(8, 0), endTime = LocalTime.of(18, 0), isAvailable = true
        ))
        
        // Sunday: Off
        staffAvailabilityRepository.save(StaffAvailability(
            staff = doctor, dayOfWeek = 0,
            startTime = LocalTime.of(0, 0), endTime = LocalTime.of(0, 0), isAvailable = false
        ))

        // ================================================================
        //  NURSE AVAILABILITY - More flexible for home visits
        //  Mon-Fri: 7:30-15:00 (7.5h/day) - Mostly home visits with some admin
        //  Sat: 8:00-14:00 (6h) - Home visits only
        //  Sun: Off
        // ================================================================
        for (day in 1..5) { // Monday-Friday
            staffAvailabilityRepository.save(StaffAvailability(
                staff = nurse, dayOfWeek = day,
                startTime = LocalTime.of(7, 30), endTime = LocalTime.of(15, 0), isAvailable = true
            ))
        }
        
        // Saturday: Limited home visits
        staffAvailabilityRepository.save(StaffAvailability(
            staff = nurse, dayOfWeek = 6,
            startTime = LocalTime.of(8, 0), endTime = LocalTime.of(14, 0), isAvailable = true
        ))
        
        // Sunday: Off
        staffAvailabilityRepository.save(StaffAvailability(
            staff = nurse, dayOfWeek = 0,
            startTime = LocalTime.of(0, 0), endTime = LocalTime.of(0, 0), isAvailable = false
        ))

        // ================================================================
        //  COMPLETED APPOINTMENTS (recent history — past 2 weeks)
        // ================================================================
        val completed1 = appointmentRepository.save(Appointment(
            patient = p1, staff = doctor, scheduledAt = dateAt(-14, 10),
            estimatedDurationMinutes = 45, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.COMPLETED, notes = "Post-surgery initial assessment",
            location = "Matti's home - Tuira, Oulu"
        ))
        val completed2 = appointmentRepository.save(Appointment(
            patient = p1, staff = nurse, scheduledAt = dateAt(-12, 8),
            estimatedDurationMinutes = 30, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.COMPLETED, notes = "Wound dressing change, incision healing well",
            location = "Matti's home - Tuira, Oulu"
        ))
        val completed3 = appointmentRepository.save(Appointment(
            patient = p2, staff = doctor, scheduledAt = dateAt(-10, 11),
            estimatedDurationMinutes = 30, type = AppointmentType.TELECONSULTATION,
            status = AppointmentStatus.COMPLETED, notes = "Diabetes review — A1C results discussion"
        ))
        appointmentRepository.save(Appointment(
            patient = p3, staff = nurse, scheduledAt = dateAt(-7, 9),
            estimatedDurationMinutes = 40, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.COMPLETED, notes = "Leg ulcer dressing change",
            location = "Antti's home - Kaukovainio, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p4, staff = doctor, scheduledAt = dateAt(-5, 14),
            estimatedDurationMinutes = 45, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.COMPLETED, notes = "Pain management review, adjusted medication",
            location = "Sanna's home - Linnanmaa, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p5, staff = doctor, scheduledAt = dateAt(-3, 10),
            estimatedDurationMinutes = 30, type = AppointmentType.TELECONSULTATION,
            status = AppointmentStatus.COMPLETED, notes = "Heart failure medication review"
        ))
        appointmentRepository.save(Appointment(
            patient = p6, staff = nurse, scheduledAt = dateAt(-2, 8, 30),
            estimatedDurationMinutes = 30, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.COMPLETED, notes = "Post-op hip mobility check, exercises reviewed",
            location = "Heli's home - Rajakylä, Oulu"
        ))

        // ================================================================
        //  VISIT SUMMARIES for completed appointments
        // ================================================================
        visitSummaryRepository.save(VisitSummary(
            appointment = completed1,
            summary = "Patient recovering well 2 weeks post knee surgery. Vital signs stable. BP 128/82. Wound clean, no signs of infection.",
            recommendations = "Continue Paracetamol. Start gentle range-of-motion exercises. Elevate leg when resting.",
            medications = "Paracetamol 1g x3/day, Enoxaparin 40mg daily (DVT prophylaxis), Vitamin D 20µg",
            nextVisitRecommendation = dateAt(-7, 10), createdBy = doctor
        ))
        visitSummaryRepository.save(VisitSummary(
            appointment = completed2,
            summary = "Surgical wound healing well. Removed old dressing, cleaned with saline, applied new sterile dressing. No redness or discharge.",
            recommendations = "Keep wound dry. Next dressing change in 3 days. Contact if redness or fever.",
            medications = null, nextVisitRecommendation = dateAt(-9, 8), createdBy = nurse
        ))
        visitSummaryRepository.save(VisitSummary(
            appointment = completed3,
            summary = "A1C result: 7.2% (target <7%). Fasting glucose logs show spikes after dinner. Discussed carbohydrate counting.",
            recommendations = "Adjust evening insulin dose from 12 to 14 units. Reduce carb intake at dinner. Recheck A1C in 3 months.",
            medications = "Metformin 1000mg x2/day, Insulin Glargine 14 units (was 12), Atorvastatin 20mg",
            nextVisitRecommendation = dateAt(14, 11), createdBy = doctor
        ))

        // ================================================================
        //  ONE-OFF SCHEDULED APPOINTMENTS (next 7 days)
        // ================================================================
        // Doctor — 4 home visits + 2 teleconsultations this week
        appointmentRepository.save(Appointment(
            patient = p4, staff = doctor, scheduledAt = dateAt(1, 10),
            estimatedDurationMinutes = 45, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.CONFIRMED, notes = "Palliative care — pain reassessment",
            location = "Sanna's home - Linnanmaa, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p7, staff = doctor, scheduledAt = dateAt(1, 14),
            estimatedDurationMinutes = 20, type = AppointmentType.TELECONSULTATION,
            status = AppointmentStatus.SCHEDULED, notes = "COPD — review spirometry results"
        ))
        appointmentRepository.save(Appointment(
            patient = p8, staff = doctor, scheduledAt = dateAt(2, 10),
            estimatedDurationMinutes = 30, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.SCHEDULED, notes = "Quarterly elderly wellness check",
            location = "Tuula's home - Pateniemi, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p5, staff = doctor, scheduledAt = dateAt(3, 11),
            estimatedDurationMinutes = 30, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.SCHEDULED, notes = "Heart failure — weight & edema check",
            location = "Jukka's home - Toppila, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p1, staff = doctor, scheduledAt = dateAt(4, 9, 30),
            estimatedDurationMinutes = 30, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.SCHEDULED, notes = "Post-surgery follow-up — 4 week check",
            location = "Matti's home - Tuira, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p8, staff = doctor, scheduledAt = dateAt(5, 15),
            estimatedDurationMinutes = 15, type = AppointmentType.TELECONSULTATION,
            status = AppointmentStatus.SCHEDULED, notes = "Lab results review — blood panel"
        ))

        // Nurse — 6 home visits this week (on top of recurring)
        appointmentRepository.save(Appointment(
            patient = p4, staff = nurse, scheduledAt = dateAt(1, 8),
            estimatedDurationMinutes = 45, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.CONFIRMED,
            notes = "Palliative — vitals, medication admin, comfort care",
            location = "Sanna's home - Linnanmaa, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p5, staff = nurse, scheduledAt = dateAt(1, 10),
            estimatedDurationMinutes = 30, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.SCHEDULED, notes = "Heart failure — daily weight, BP, edema check",
            location = "Jukka's home - Toppila, Oulu"
        ))
        val nurseApptForReschedule = appointmentRepository.save(Appointment(
            patient = p6, staff = nurse, scheduledAt = dateAt(2, 9),
            estimatedDurationMinutes = 40, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.SCHEDULED, notes = "Hip rehab exercises + wound check",
            location = "Heli's home - Rajakylä, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p4, staff = nurse, scheduledAt = dateAt(3, 8),
            estimatedDurationMinutes = 45, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.SCHEDULED, notes = "Palliative — vitals, subcutaneous meds",
            location = "Sanna's home - Linnanmaa, Oulu"
        ))
        appointmentRepository.save(Appointment(
            patient = p5, staff = nurse, scheduledAt = dateAt(4, 10),
            estimatedDurationMinutes = 30, type = AppointmentType.HOME_VISIT,
            status = AppointmentStatus.SCHEDULED, notes = "Heart failure — daily monitoring",
            location = "Jukka's home - Toppila, Oulu"
        ))

        // ================================================================
        //  RECURRING APPOINTMENT SERIES
        // ================================================================

        // Nurse → p1: Weekly wound care (post-surgery), 4 weeks
        seedRecurringSeries(
            appointmentRepository, p1, nurse,
            dateAt(1, 9, 0), 30, AppointmentType.HOME_VISIT,
            RecurringFrequency.WEEKLY, LocalDate.now().plusWeeks(4),
            "Weekly wound care — dressing change & healing check",
            "Matti's home - Tuira, Oulu"
        )

        // Nurse → p2: Weekly diabetes check, 4 weeks
        seedRecurringSeries(
            appointmentRepository, p2, nurse,
            dateAt(2, 10, 0), 25, AppointmentType.HOME_VISIT,
            RecurringFrequency.WEEKLY, LocalDate.now().plusWeeks(4),
            "Weekly glucose log review, injection site check",
            "Liisa's home - Keskusta, Oulu"
        )

        // Nurse → p3: Biweekly leg ulcer care, 8 weeks
        seedRecurringSeries(
            appointmentRepository, p3, nurse,
            dateAt(1, 11), 40, AppointmentType.HOME_VISIT,
            RecurringFrequency.BIWEEKLY, LocalDate.now().plusWeeks(8),
            "Leg ulcer — clean, measure, apply compression dressing",
            "Antti's home - Kaukovainio, Oulu"
        )

        // Doctor → p2: Monthly diabetes management, 3 months
        seedRecurringSeries(
            appointmentRepository, p2, doctor,
            dateAt(7, 11), 30, AppointmentType.TELECONSULTATION,
            RecurringFrequency.MONTHLY, LocalDate.now().plusMonths(3),
            "Monthly diabetes review — labs, medication adjustment"
        )

        // Doctor → p4: Weekly palliative check, 4 weeks
        seedRecurringSeries(
            appointmentRepository, p4, doctor,
            dateAt(7, 14), 45, AppointmentType.HOME_VISIT,
            RecurringFrequency.WEEKLY, LocalDate.now().plusWeeks(4),
            "Palliative care — pain management, symptom control",
            "Sanna's home - Linnanmaa, Oulu"
        )

        // Doctor → p5: Biweekly heart failure follow-up, 2 months
        seedRecurringSeries(
            appointmentRepository, p5, doctor,
            dateAt(10, 10, 30), 30, AppointmentType.HOME_VISIT,
            RecurringFrequency.BIWEEKLY, LocalDate.now().plusMonths(2),
            "Heart failure — exam, medication review, fluid status",
            "Jukka's home - Toppila, Oulu"
        )

        // Doctor → p6: Monthly post-op follow-up, 3 months
        seedRecurringSeries(
            appointmentRepository, p6, doctor,
            dateAt(14, 10), 30, AppointmentType.HOME_VISIT,
            RecurringFrequency.MONTHLY, LocalDate.now().plusMonths(3),
            "Hip replacement follow-up — mobility, X-ray review",
            "Heli's home - Rajakylä, Oulu"
        )

        // Doctor → p7: Monthly COPD management, 3 months
        seedRecurringSeries(
            appointmentRepository, p7, doctor,
            dateAt(14, 14), 20, AppointmentType.TELECONSULTATION,
            RecurringFrequency.MONTHLY, LocalDate.now().plusMonths(3),
            "COPD — inhaler technique, spirometry review"
        )

        // Doctor → p8: Monthly elderly check, 3 months
        seedRecurringSeries(
            appointmentRepository, p8, doctor,
            dateAt(21, 10), 30, AppointmentType.HOME_VISIT,
            RecurringFrequency.MONTHLY, LocalDate.now().plusMonths(3),
            "Elderly wellness — falls risk, cognition, medication review",
            "Tuula's home - Pateniemi, Oulu"
        )

        // Nurse → p6: Weekly hip rehab visits, 6 weeks
        seedRecurringSeries(
            appointmentRepository, p6, nurse,
            dateAt(3, 9), 40, AppointmentType.HOME_VISIT,
            RecurringFrequency.WEEKLY, LocalDate.now().plusWeeks(6),
            "Hip rehab — exercises, wound check, mobility assessment",
            "Heli's home - Rajakylä, Oulu"
        )

        // ================================================================
        //  CARE TASKS (per patient condition)
        // ================================================================

        // p1 — Post-surgery recovery
        listOf(
            Triple("Take pain medication", "Paracetamol 1g with breakfast", "08:00"),
            Triple("Blood pressure check", "Measure BP, report if above 140/90", "09:00"),
            Triple("Leg elevation exercises", "Elevate operated leg for 20 min", "11:00"),
            Triple("Light walking", "10–15 minute walk around the house", "15:00"),
            Triple("Evening medication", "Enoxaparin injection (DVT prevention)", "20:00"),
        ).forEach { (title, desc, time) ->
            careTaskRepository.save(CareTask(
                patient = p1, title = title, description = desc,
                dueDate = LocalDate.now().plusDays(1), dueTime = time,
                status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
            ))
        }

        // p2 — Diabetes management
        listOf(
            Triple("Fasting blood sugar", "Check blood sugar before breakfast, log result", "07:00"),
            Triple("Morning medication", "Metformin 1000mg with breakfast", "08:00"),
            Triple("Post-lunch glucose", "Check blood sugar 2h after lunch", "14:00"),
            Triple("Evening medication", "Metformin 1000mg + Atorvastatin 20mg", "18:00"),
            Triple("Insulin injection", "Insulin Glargine 14 units before bed", "21:00"),
        ).forEach { (title, desc, time) ->
            careTaskRepository.save(CareTask(
                patient = p2, title = title, description = desc,
                dueDate = LocalDate.now().plusDays(1), dueTime = time,
                status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
            ))
        }

        // p3 — Wound care
        listOf(
            Triple("Wound inspection", "Check leg ulcer for redness, swelling, or discharge", "08:00"),
            Triple("Compression stocking", "Apply compression stocking after morning shower", "09:00"),
            Triple("Leg elevation", "Elevate legs above heart level for 30 min", "14:00"),
        ).forEach { (title, desc, time) ->
            careTaskRepository.save(CareTask(
                patient = p3, title = title, description = desc,
                dueDate = LocalDate.now().plusDays(1), dueTime = time,
                status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
            ))
        }

        // p4 — Palliative care
        listOf(
            Triple("Pain assessment", "Rate pain on 0-10 scale, note any changes", "08:00"),
            Triple("Morning medications", "Oxycodone 10mg + Paracetamol 1g", "08:30"),
            Triple("Comfort check", "Assess comfort, nausea, appetite", "12:00"),
            Triple("Afternoon medication", "Oxycodone 10mg + anti-nausea if needed", "16:00"),
            Triple("Evening care", "Skin care, position change, night medication", "20:00"),
        ).forEach { (title, desc, time) ->
            careTaskRepository.save(CareTask(
                patient = p4, title = title, description = desc,
                dueDate = LocalDate.now().plusDays(1), dueTime = time,
                status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
            ))
        }

        // p5 — Heart failure
        listOf(
            Triple("Daily weight", "Weigh before breakfast. Report if >1kg gain in 24h", "07:00"),
            Triple("Blood pressure & pulse", "Measure BP and heart rate, log results", "08:00"),
            Triple("Morning medication", "Bisoprolol 5mg + Ramipril 5mg + Furosemide 40mg", "08:30"),
            Triple("Fluid intake tracking", "Track all fluids — target max 1.5L/day", "12:00"),
            Triple("Ankle swelling check", "Check ankles for edema, report if worsening", "18:00"),
        ).forEach { (title, desc, time) ->
            careTaskRepository.save(CareTask(
                patient = p5, title = title, description = desc,
                dueDate = LocalDate.now().plusDays(1), dueTime = time,
                status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
            ))
        }

        // p6 — Post-hip replacement
        listOf(
            Triple("Hip exercises", "Perform prescribed exercises (sheet on fridge)", "09:00"),
            Triple("Walking practice", "Walk 10 min with walker, increase gradually", "11:00"),
            Triple("Ice pack", "Apply ice pack to hip for 15 min", "15:00"),
        ).forEach { (title, desc, time) ->
            careTaskRepository.save(CareTask(
                patient = p6, title = title, description = desc,
                dueDate = LocalDate.now().plusDays(1), dueTime = time,
                status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
            ))
        }

        // p7 — COPD (weekly tasks)
        careTaskRepository.save(CareTask(
            patient = p7, title = "Inhaler technique practice",
            description = "Practice inhaler technique with spacer. Rinse mouth after steroid inhaler.",
            dueDate = LocalDate.now().plusDays(1), dueTime = "09:00",
            status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
        ))
        careTaskRepository.save(CareTask(
            patient = p7, title = "Peak flow measurement",
            description = "Measure peak flow, record in diary. Call if below 200 L/min.",
            dueDate = LocalDate.now().plusDays(1), dueTime = "08:00",
            status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
        ))

        // p8 — Elderly monitoring
        listOf(
            Triple("Morning medication", "Take all morning pills with water", "08:00"),
            Triple("Blood pressure", "Measure BP and log", "09:00"),
            Triple("Light exercise", "Chair exercises or short walk (15 min)", "11:00"),
        ).forEach { (title, desc, time) ->
            careTaskRepository.save(CareTask(
                patient = p8, title = title, description = desc,
                dueDate = LocalDate.now().plusDays(1), dueTime = time,
                status = TaskStatus.PENDING, frequency = TaskFrequency.DAILY
            ))
        }

        // ================================================================
        //  TIME OFF REQUESTS
        // ================================================================
        val nextFriday = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.FRIDAY)
        timeOffRequestRepository.save(TimeOffRequest(
            staff = nurse, startDate = nextFriday, endDate = nextFriday,
            reason = "Personal appointment", status = TimeOffStatus.PENDING
        ))
        timeOffRequestRepository.save(TimeOffRequest(
            staff = doctor,
            startDate = LocalDate.now().plusWeeks(3).with(java.time.DayOfWeek.MONDAY),
            endDate = LocalDate.now().plusWeeks(3).with(java.time.DayOfWeek.FRIDAY),
            reason = "Medical conference in Helsinki", status = TimeOffStatus.APPROVED
        ))

        // ================================================================
        //  RESCHEDULE REQUEST
        // ================================================================
        rescheduleRequestRepository.save(RescheduleRequest(
            appointment = nurseApptForReschedule, requestedBy = p6,
            reason = "I have a physiotherapy session at the hospital that morning",
            preferredDate1 = dateAt(2, 14), preferredDate2 = dateAt(3, 9),
            status = RescheduleStatus.PENDING
        ))

        // ================================================================
        //  PRINT SUMMARY
        // ================================================================
        println("=== Development data seeded successfully ===")
        println("Users:              ${userRepository.count()} (2 staff, 8 patients, 2 family)")
        println("Appointments:       ${appointmentRepository.count()}")
        println("Care Tasks:         ${careTaskRepository.count()}")
        println("Visit Summaries:    ${visitSummaryRepository.count()}")
        println("Care Assignments:   ${careAssignmentRepository.count()} (Doctor→8, Nurse→6)")
        println("Staff Availability: ${staffAvailabilityRepository.count()} slots")
        println("Time Off Requests:  ${timeOffRequestRepository.count()}")
        println("Reschedule Reqs:    ${rescheduleRequestRepository.count()}")
        println("============================================")
        println("Test accounts (all passwords: password123):")
        println("  Doctor:  dr.smith@hospital.fi")
        println("  Nurse:   nurse.jones@hospital.fi")
        println("  Patients:")
        println("    matti.virtanen@email.com  (post-surgery)")
        println("    liisa.korhonen@email.com  (diabetes)")
        println("    antti.makinen@email.com   (wound care)")
        println("    sanna.laine@email.com     (palliative)")
        println("    jukka.nieminen@email.com  (heart failure)")
        println("    heli.jarvinen@email.com   (hip replacement)")
        println("    mikko.salonen@email.com   (COPD)")
        println("    tuula.hakala@email.com    (elderly monitoring)")
        println("  Family:")
        println("    pekka.virtanen@email.com  (son of Matti)")
        println("    anna.korhonen@email.com   (daughter of Liisa)")
        println("============================================")
    }
}
