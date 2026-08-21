-- Technical bootstrap migration for TECH-004.
-- Intentionally contains no schema changes and creates no database object.
-- Its only purpose is to give Flyway a real migration to apply once and
-- to validate that it is not re-executed on subsequent application restarts.
SELECT 1;
