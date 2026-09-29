-- Database migration template (MySQL 8).
--
-- Usage:
-- 1. Copy this file and rename it to migration-YYYYMMDD-description.sql.
-- 2. Back up the target database before executing the migration.
-- 3. Write forward-only SQL below; do not modify an applied migration.
-- 4. Merge the final structure into schema.sql for fresh installations.

-- Add migration SQL below.


-- 清空表数据
set foreign_key_checks = 0;
TRUNCATE TABLE `student_topic_selection`;
TRUNCATE TABLE `teacher_group_quota`;
TRUNCATE TABLE `topic`;
TRUNCATE TABLE `major`;
TRUNCATE TABLE `user`;
TRUNCATE TABLE `topic_group`;
TRUNCATE TABLE `college`;
TRUNCATE TABLE `switch`;
SET FOREIGN_KEY_CHECKS = 1;