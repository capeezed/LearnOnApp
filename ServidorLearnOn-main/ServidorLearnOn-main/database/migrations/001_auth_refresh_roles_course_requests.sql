-- LearnOn - JWT refresh tokens, admin role e prioridade de pedidos
-- Banco esperado: MySQL/MariaDB, conforme backend atual com mysql2.
-- Rode na VM conectado ao banco learnon.

CREATE TABLE IF NOT EXISTS admins (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_type ENUM('student', 'instructor', 'admin') NOT NULL,
  user_id INT NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_refresh_tokens_user (user_type, user_id),
  INDEX idx_refresh_tokens_expires_at (expires_at)
);

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE course_requests ADD COLUMN priority_score DECIMAL(10,4) NOT NULL DEFAULT 0',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'course_requests'
    AND COLUMN_NAME = 'priority_score'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE course_requests ADD COLUMN queue_type ENUM(''normal'', ''fast_track'') NOT NULL DEFAULT ''normal''',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'course_requests'
    AND COLUMN_NAME = 'queue_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Status pode conter valores antigos nao mapeados na VM. Converter para VARCHAR
-- antes evita "Data truncated" ao expandir/restringir ENUM.
ALTER TABLE course_requests
  MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'aguardando_match';

UPDATE course_requests SET status = 'aguardando_match' WHERE status IN ('pending', 'queued', 'open', '');
UPDATE course_requests SET status = 'aguardando_instrutor' WHERE status IN ('matched', 'waiting_instructor');
UPDATE course_requests SET status = 'em_andamento' WHERE status IN ('in_production', 'processing', 'accepted');
UPDATE course_requests SET status = 'concluido' WHERE status IN ('delivered', 'done', 'completed');
UPDATE course_requests SET status = 'cancelado' WHERE status IN ('cancelled', 'canceled', 'expired', 'rejected');
UPDATE course_requests
SET status = 'aguardando_match'
WHERE status NOT IN (
  'aguardando_match',
  'aguardando_instrutor',
  'em_andamento',
  'concluido',
  'cancelado'
);

ALTER TABLE course_requests
  MODIFY COLUMN status ENUM(
    'aguardando_match',
    'aguardando_instrutor',
    'em_andamento',
    'concluido',
    'cancelado'
  ) NOT NULL DEFAULT 'aguardando_match';

ALTER TABLE course_requests
  MODIFY COLUMN format_preference ENUM('live', 'recorded', 'no_preference') NOT NULL DEFAULT 'no_preference',
  MODIFY COLUMN urgency ENUM('normal', 'fast_track') NOT NULL DEFAULT 'normal';

CREATE TABLE IF NOT EXISTS queue_scores (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id INT NOT NULL UNIQUE,
  demand_score DECIMAL(10,4) NOT NULL DEFAULT 0,
  age_score DECIMAL(10,4) NOT NULL DEFAULT 0,
  urgency_score DECIMAL(10,4) NOT NULL DEFAULT 0,
  availability_score DECIMAL(10,4) NOT NULL DEFAULT 0,
  total_score DECIMAL(10,4) NOT NULL DEFAULT 0,
  queue_type ENUM('normal', 'fast_track') NOT NULL DEFAULT 'normal',
  explanation JSON NULL,
  calculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_queue_scores_queue (queue_type, total_score),
  CONSTRAINT fk_queue_scores_request
    FOREIGN KEY (request_id) REFERENCES course_requests(id)
    ON DELETE CASCADE
);

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE queue_scores ADD COLUMN explanation JSON NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'queue_scores'
    AND COLUMN_NAME = 'explanation'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE instructors ADD COLUMN active_matches INT NOT NULL DEFAULT 0',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'instructors'
    AND COLUMN_NAME = 'active_matches'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE instructors ADD COLUMN max_active_matches INT NOT NULL DEFAULT 3',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'instructors'
    AND COLUMN_NAME = 'max_active_matches'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_course_requests_queue_priority ON course_requests (queue_type, priority_score)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'course_requests'
    AND INDEX_NAME = 'idx_course_requests_queue_priority'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_course_requests_topic_status ON course_requests (topic_tag, status)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'course_requests'
    AND INDEX_NAME = 'idx_course_requests_topic_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
