-- LearnOn - pagamentos Mercado Pago e liberacao automatica por webhook.
-- Seguro para rodar mais de uma vez.

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  course_id INT UNSIGNED NOT NULL,
  provider ENUM('mercado_pago') NOT NULL DEFAULT 'mercado_pago',
  status ENUM('pending', 'in_process', 'approved', 'rejected', 'cancelled', 'refunded', 'charged_back', 'expired') NOT NULL DEFAULT 'pending',
  amount DECIMAL(10,2) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'BRL',
  external_reference VARCHAR(120) NOT NULL UNIQUE,
  provider_preference_id VARCHAR(120) NULL,
  provider_payment_id VARCHAR(120) NULL,
  payment_method VARCHAR(80) NULL,
  payment_type VARCHAR(80) NULL,
  checkout_url TEXT NULL,
  sandbox_checkout_url TEXT NULL,
  raw_provider_response JSON NULL,
  paid_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_payments_student_course_status (student_id, course_id, status),
  INDEX idx_payments_provider_payment (provider, provider_payment_id),
  CONSTRAINT fk_payments_student
    FOREIGN KEY (student_id) REFERENCES students(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_payments_course
    FOREIGN KEY (course_id) REFERENCES courses(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payment_events (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  payment_id BIGINT UNSIGNED NOT NULL,
  provider ENUM('mercado_pago') NOT NULL DEFAULT 'mercado_pago',
  provider_event_id VARCHAR(120) NULL,
  provider_payment_id VARCHAR(120) NULL,
  event_type VARCHAR(80) NOT NULL,
  payload JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_payment_events_payment (payment_id),
  INDEX idx_payment_events_provider_payment (provider, provider_payment_id),
  CONSTRAINT fk_payment_events_payment
    FOREIGN KEY (payment_id) REFERENCES payments(id)
    ON DELETE CASCADE
);

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE UNIQUE INDEX uq_enrollments_student_course ON enrollments (student_id, course_id)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'enrollments'
    AND INDEX_NAME = 'uq_enrollments_student_course'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
