# Database Schema Diagram

## Entity Relationship Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                    USERS                                         │
│  ┌─────────────┐                                                                 │
│  │   users     │                                                                 │
│  ├─────────────┤                                                                 │
│  │ id (PK)     │                                                                 │
│  │ email       │                                                                 │
│  │ password    │                                                                 │
│  │ first_name  │                                                                 │
│  │ last_name   │                                                                 │
│  │ phone_number│                                                                 │
│  │ role        │──► PATIENT | FAMILY_MEMBER | DOCTOR | NURSE                     │
│  │ is_active   │                                                                 │
│  │ created_at  │                                                                 │
│  └──────┬──────┘                                                                 │
│         │                                                                        │
│         ├─────────────────┬─────────────────┐                                    │
│         ▼                 ▼                 ▼                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐                      │
│  │staff_profiles│  │patient_      │  │notification_       │                      │
│  │              │  │profiles      │  │settings            │                      │
│  ├──────────────┤  ├──────────────┤  ├────────────────────┤                      │
│  │ user_id (FK) │  │ user_id (FK) │  │ user_id (FK)       │                      │
│  │ department   │  │ date_of_birth│  │ appointment_remind │                      │
│  │ specialization│ │ address      │  │ task_reminders     │                      │
│  │ license_number│ │ emergency_   │  │ quiet_hours        │                      │
│  └──────────────┘  │ contact      │  └────────────────────┘                      │
│                    └──────────────┘                                              │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              PATIENT-FAMILY LINKING                              │
│                                                                                  │
│  ┌─────────────┐         ┌────────────────────┐         ┌─────────────┐         │
│  │   users     │         │ patient_family_    │         │   users     │         │
│  │  (PATIENT)  │◄────────│ links              │────────►│  (FAMILY)   │         │
│  └─────────────┘         ├────────────────────┤         └─────────────┘         │
│                          │ patient_id (FK)    │                                  │
│                          │ family_member_id   │                                  │
│                          │ relationship       │                                  │
│                          │ access_level       │                                  │
│                          │ status             │                                  │
│                          └────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              STAFF SCHEDULING                                    │
│                                                                                  │
│  ┌─────────────┐         ┌────────────────────┐                                  │
│  │   users     │         │ staff_availability │                                  │
│  │  (STAFF)    │◄────────┤                    │                                  │
│  └──────┬──────┘         │ staff_id (FK)      │                                  │
│         │                │ day_of_week        │                                  │
│         │                │ start_time         │                                  │
│         │                │ end_time           │                                  │
│         │                └────────────────────┘                                  │
│         │                                                                        │
│         │                ┌────────────────────┐                                  │
│         └───────────────►│ time_off_requests  │                                  │
│                          │                    │                                  │
│                          │ staff_id (FK)      │                                  │
│                          │ start_date         │                                  │
│                          │ end_date           │                                  │
│                          │ status             │                                  │
│                          └────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                                APPOINTMENTS                                      │
│                                                                                  │
│  ┌─────────────┐                                         ┌─────────────┐        │
│  │   users     │         ┌────────────────────┐          │   users     │        │
│  │  (PATIENT)  │◄────────┤   appointments     ├─────────►│  (STAFF)    │        │
│  └─────────────┘         ├────────────────────┤          └─────────────┘        │
│                          │ id (PK)            │                                  │
│                          │ patient_id (FK)    │                                  │
│                          │ staff_id (FK)      │                                  │
│                          │ scheduled_at       │                                  │
│                          │ duration_minutes   │                                  │
│                          │ type               │──► HOME_VISIT | HOSPITAL | TELE  │
│                          │ status             │──► SCHEDULED | CONFIRMED | ...   │
│                          │ is_emergency       │                                  │
│                          │ notes              │                                  │
│                          │ location           │                                  │
│                          └─────────┬──────────┘                                  │
│                                    │                                             │
│                    ┌───────────────┼───────────────┐                             │
│                    ▼               ▼               ▼                             │
│         ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                     │
│         │visit_        │  │reschedule_   │  │              │                     │
│         │summaries     │  │requests      │  │              │                     │
│         ├──────────────┤  ├──────────────┤  │              │                     │
│         │appointment_id│  │appointment_id│  │              │                     │
│         │summary       │  │requested_by  │  │              │                     │
│         │recommendations│ │preferred_date│  │              │                     │
│         │medications   │  │status        │  │              │                     │
│         │vital_signs   │  └──────────────┘  │              │                     │
│         └──────────────┘                    │              │                     │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                                 CARE TASKS                                       │
│                                                                                  │
│  ┌─────────────┐         ┌────────────────────┐          ┌─────────────┐        │
│  │   users     │         │   care_tasks       │          │   users     │        │
│  │  (PATIENT)  │◄────────┤                    ├─────────►│  (STAFF)    │        │
│  └─────────────┘         ├────────────────────┤          │ assigned_by │        │
│                          │ id (PK)            │          └─────────────┘        │
│                          │ patient_id (FK)    │                                  │
│                          │ title              │                                  │
│                          │ description        │                                  │
│                          │ due_date           │                                  │
│                          │ due_time           │                                  │
│                          │ status             │──► PENDING | COMPLETED | SKIPPED │
│                          │ frequency          │──► ONCE | DAILY | WEEKLY | ...   │
│                          │ priority           │──► LOW | NORMAL | HIGH           │
│                          │ assigned_by (FK)   │                                  │
│                          └────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                                  MESSAGING                                       │
│                                                                                  │
│  ┌─────────────┐         ┌────────────────────┐          ┌─────────────┐        │
│  │   users     │         │   conversations    │          │   users     │        │
│  │  (PATIENT)  │◄────────┤                    ├─────────►│  (STAFF)    │        │
│  └─────────────┘         ├────────────────────┤          └─────────────┘        │
│                          │ id (PK)            │                                  │
│                          │ patient_id (FK)    │                                  │
│                          │ staff_id (FK)      │                                  │
│                          │ subject            │                                  │
│                          │ status             │                                  │
│                          └─────────┬──────────┘                                  │
│                                    │                                             │
│                                    ▼                                             │
│                          ┌────────────────────┐                                  │
│                          │   messages         │                                  │
│                          ├────────────────────┤                                  │
│                          │ id (PK)            │                                  │
│                          │ conversation_id(FK)│                                  │
│                          │ sender_id (FK)     │                                  │
│                          │ content            │                                  │
│                          │ is_read            │                                  │
│                          │ sent_at            │                                  │
│                          └────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                            NOTIFICATIONS & AUDIT                                 │
│                                                                                  │
│  ┌─────────────┐         ┌────────────────────┐                                  │
│  │   users     │◄────────┤   notifications    │                                  │
│  └─────────────┘         ├────────────────────┤                                  │
│                          │ user_id (FK)       │                                  │
│                          │ title              │                                  │
│                          │ message            │                                  │
│                          │ type               │                                  │
│                          │ is_read            │                                  │
│                          └────────────────────┘                                  │
│                                                                                  │
│  ┌─────────────┐         ┌────────────────────┐                                  │
│  │   users     │◄────────┤   audit_log        │                                  │
│  └─────────────┘         ├────────────────────┤                                  │
│                          │ user_id (FK)       │                                  │
│                          │ action             │                                  │
│                          │ entity_type        │                                  │
│                          │ entity_id          │                                  │
│                          │ old_values (JSON)  │                                  │
│                          │ new_values (JSON)  │                                  │
│                          └────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Tables Summary

| Table | Description | Key Relationships |
|-------|-------------|-------------------|
| `users` | All system users | Base table for all user types |
| `staff_profiles` | Extended staff info | 1:1 with users (DOCTOR/NURSE) |
| `patient_profiles` | Extended patient info | 1:1 with users (PATIENT) |
| `patient_family_links` | Family-patient access | Many:Many users |
| `staff_availability` | Weekly schedule | Many:1 with staff |
| `time_off_requests` | Leave requests | Many:1 with staff |
| `appointments` | Visit scheduling | Links patient + staff |
| `reschedule_requests` | Change requests | Many:1 with appointments |
| `visit_summaries` | Post-visit notes | 1:1 with appointments |
| `care_tasks` | Daily care checklist | Many:1 with patient |
| `conversations` | Chat threads | Links patient + staff |
| `messages` | Chat messages | Many:1 with conversations |
| `notifications` | User alerts | Many:1 with users |
| `notification_settings` | Preferences | 1:1 with users |
| `audit_log` | Change tracking | References all entities |

## User Roles & Access

| Role | Can Access |
|------|------------|
| **PATIENT** | Own appointments, tasks, messages, visit summaries |
| **FAMILY_MEMBER** | Linked patient's data (based on access level) |
| **DOCTOR** | All patients, appointments, create summaries/tasks |
| **NURSE** | All patients, appointments, create summaries/tasks |
