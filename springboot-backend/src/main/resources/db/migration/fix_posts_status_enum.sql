-- Fix posts.status column to support REPORTED_REMOVED value
-- This script converts ENUM to VARCHAR(20) to support all status values
-- Run this script manually if you encounter "Data truncated for column 'status'" error

-- Step 1: Convert ENUM to VARCHAR(20) if it's currently an ENUM
-- This handles the case where the column was created as ENUM without REPORTED_REMOVED
ALTER TABLE posts 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW';

-- Step 2: Verify the change
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'posts' AND COLUMN_NAME = 'status';

-- Note: After running this script, restart your Spring Boot application.
-- The JPA entity already specifies length=20, so Hibernate will maintain this structure.

