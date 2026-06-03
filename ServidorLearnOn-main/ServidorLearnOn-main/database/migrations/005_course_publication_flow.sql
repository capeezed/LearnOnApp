-- LearnOn - fluxo de rascunho/publicacao de cursos por pedido ou grupo.
-- Seguro para rodar mais de uma vez.

UPDATE courses
SET status = 'published'
WHERE status = 'public';

ALTER TABLE courses
  MODIFY COLUMN request_id INT UNSIGNED NULL;

ALTER TABLE courses
  MODIFY COLUMN status ENUM('draft', 'published', 'archived') NOT NULL DEFAULT 'draft';

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE courses ADD COLUMN origin_request_id INT UNSIGNED NULL AFTER request_id',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'courses'
    AND COLUMN_NAME = 'origin_request_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE courses ADD COLUMN request_group_id INT UNSIGNED NULL AFTER origin_request_id',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'courses'
    AND COLUMN_NAME = 'request_group_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE courses
SET origin_request_id = request_id
WHERE origin_request_id IS NULL;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_courses_origin_request ON courses (origin_request_id)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'courses'
    AND INDEX_NAME = 'idx_courses_origin_request'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_courses_request_group ON courses (request_group_id)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'courses'
    AND INDEX_NAME = 'idx_courses_request_group'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE courses ADD CONSTRAINT fk_courses_origin_request FOREIGN KEY (origin_request_id) REFERENCES course_requests(id) ON DELETE SET NULL',
    'SELECT 1'
  )
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'courses'
    AND CONSTRAINT_NAME = 'fk_courses_origin_request'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE courses ADD CONSTRAINT fk_courses_request_group FOREIGN KEY (request_group_id) REFERENCES request_groups(id) ON DELETE SET NULL',
    'SELECT 1'
  )
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'courses'
    AND CONSTRAINT_NAME = 'fk_courses_request_group'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
