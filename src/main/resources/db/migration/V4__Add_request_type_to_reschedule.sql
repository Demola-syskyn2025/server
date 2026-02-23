-- Add request_type column to reschedule_requests table
ALTER TABLE reschedule_requests ADD COLUMN request_type VARCHAR(20) NOT NULL DEFAULT 'RESCHEDULE';
