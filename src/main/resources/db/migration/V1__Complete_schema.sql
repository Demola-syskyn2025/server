-- ============================================
-- Deepen Database Schema - V1
-- Transparency for Patients at Home
-- Complete schema with all features
-- ============================================

-- ============================================
-- USERS & AUTHENTICATION
-- ============================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) DEFAULT '',
    role VARCHAR(20) NOT NULL CHECK (role IN ('PATIENT', 'FAMILY_MEMBER', 'DOCTOR', 'NURSE')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- ============================================
-- STAFF PROFILES
-- ============================================

CREATE TABLE staff_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department VARCHAR(100),
    specialization VARCHAR(200),
    license_number VARCHAR(50),
    hire_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_staff_profiles_user_id ON staff_profiles(user_id);

-- ============================================
-- STAFF AVAILABILITY
-- ============================================

CREATE TABLE staff_availability (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_staff_availability_staff ON staff_availability(staff_id);
CREATE INDEX idx_staff_availability_day ON staff_availability(day_of_week);

-- ============================================
-- TIME OFF REQUESTS
-- ============================================

CREATE TABLE time_off_requests (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_time_off_staff ON time_off_requests(staff_id);
CREATE INDEX idx_time_off_dates ON time_off_requests(start_date, end_date);

-- ============================================
-- APPOINTMENTS
-- ============================================

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheduled_at TIMESTAMP NOT NULL,
    estimated_duration_minutes INT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('HOME_VISIT', 'HOSPITAL_VISIT', 'TELECONSULTATION')),
    status VARCHAR(20) DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'RESCHEDULED')),
    notes VARCHAR(1000),
    location VARCHAR(255),
    recurring_group_id VARCHAR(255),
    recurring_frequency VARCHAR(255) CHECK (recurring_frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY')),
    recurring_end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_staff ON appointments(staff_id);
CREATE INDEX idx_appointments_scheduled ON appointments(scheduled_at);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_recurring ON appointments(recurring_group_id);

-- ============================================
-- RESCHEDULE REQUESTS
-- ============================================

CREATE TABLE reschedule_requests (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    requested_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(500),
    preferred_date1 TIMESTAMP,
    preferred_date2 TIMESTAMP,
    preferred_date3 TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'ALTERNATIVE_OFFERED')),
    staff_response VARCHAR(500),
    new_scheduled_at TIMESTAMP,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP
);

CREATE INDEX idx_reschedule_appointment ON reschedule_requests(appointment_id);
CREATE INDEX idx_reschedule_status ON reschedule_requests(status);

-- ============================================
-- VISIT SUMMARIES
-- ============================================

CREATE TABLE visit_summaries (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
    summary VARCHAR(2000) NOT NULL,
    recommendations VARCHAR(1000),
    medications VARCHAR(1000),
    next_visit_recommendation TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_visit_summaries_appointment ON visit_summaries(appointment_id);

-- ============================================
-- CARE TASKS
-- ============================================

CREATE TABLE care_tasks (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    due_date DATE NOT NULL,
    due_time VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
    frequency VARCHAR(20) DEFAULT 'ONCE' CHECK (frequency IN ('ONCE', 'DAILY', 'WEEKLY', 'MONTHLY')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_care_tasks_patient ON care_tasks(patient_id);
CREATE INDEX idx_care_tasks_due_date ON care_tasks(due_date);
CREATE INDEX idx_care_tasks_status ON care_tasks(status);

-- ============================================
-- CARE ASSIGNMENTS
-- ============================================

CREATE TABLE care_assignments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(patient_id, staff_id)
);

CREATE INDEX idx_care_assignments_patient ON care_assignments(patient_id);
CREATE INDEX idx_care_assignments_staff ON care_assignments(staff_id);

-- ============================================
-- PATIENT PREFERENCES
-- ============================================

CREATE TABLE patient_preferences (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preferred_location VARCHAR(255),
    preferred_visit_type SMALLINT CHECK (preferred_visit_type BETWEEN 0 AND 2),
    preferred_day_of_week SMALLINT CHECK (preferred_day_of_week BETWEEN 0 AND 6),
    preferred_time_start TIME,
    preferred_time_end TIME,
    avoid_mornings BOOLEAN DEFAULT FALSE,
    avoid_evenings BOOLEAN DEFAULT FALSE,
    notes VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_patient_preferences_patient ON patient_preferences(patient_id);

-- ============================================
-- CONVERSATIONS (for Chat)
-- ============================================

CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CLOSED', 'ARCHIVED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(patient_id, staff_id)
);

CREATE INDEX idx_conversations_patient ON conversations(patient_id);
CREATE INDEX idx_conversations_staff ON conversations(staff_id);

-- ============================================
-- MESSAGES
-- ============================================

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_sent_at ON messages(sent_at);

-- ============================================
-- NOTIFICATIONS
-- ============================================

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at);

-- ============================================
-- NOTIFICATION SETTINGS
-- ============================================

CREATE TABLE notification_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    appointment_reminders BOOLEAN DEFAULT TRUE,
    task_reminders BOOLEAN DEFAULT TRUE,
    message_notifications BOOLEAN DEFAULT TRUE,
    reminder_hours_before INT DEFAULT 24,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_settings_user ON notification_settings(user_id);
