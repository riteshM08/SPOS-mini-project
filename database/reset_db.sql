-- ============================================================================
-- AIRLINE RESERVATION SYSTEM (ARS)
-- Full Database Reset Script
-- Usage: psql -U postgres -d airline_db -f database/reset_db.sql
-- ============================================================================

\i database/schema.sql
\i database/seed_data.sql

SELECT 'Database reset and sample data populated successfully!' AS status;
