-- Phase 4: students can have a portal login (STUDENT role) linked to their profile.
-- Same pattern as guardians.user_id and staff.user_id — nullable, populated when
-- provisionPortalUser is set on the student create/update call.
ALTER TABLE students ADD COLUMN user_id UUID;
CREATE INDEX idx_students_user ON students (user_id);
