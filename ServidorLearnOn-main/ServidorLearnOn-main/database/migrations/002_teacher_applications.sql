-- LearnOn - candidaturas de instrutor/professor.
-- Rode na VM conectado ao banco learnon antes de publicar o endpoint.

CREATE TABLE IF NOT EXISTS teacher_applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NULL,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL,
  phone VARCHAR(40) NOT NULL,
  profile_photo_url VARCHAR(500) NULL,
  bio TEXT NOT NULL,
  location VARCHAR(120) NOT NULL,
  subjects TEXT NOT NULL,
  experience_level ENUM('Junior', 'Pleno', 'Senior', 'Especialista') NOT NULL,
  years_experience INT NOT NULL DEFAULT 0,
  linkedin_url VARCHAR(500) NOT NULL,
  github_url VARCHAR(500) NOT NULL,
  portfolio_url VARCHAR(500) NULL,
  class_format ENUM('Ao vivo', 'Gravadas', 'Ambos') NOT NULL,
  suggested_price_range VARCHAR(40) NOT NULL,
  average_response_time VARCHAR(40) NOT NULL,
  document_url VARCHAR(500) NULL,
  certificate_url VARCHAR(500) NULL,
  accepted_terms BOOLEAN NOT NULL DEFAULT FALSE,
  status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
  review_notes TEXT NULL,
  reviewed_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_teacher_applications_email (email),
  INDEX idx_teacher_applications_status (status),
  INDEX idx_teacher_applications_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS teacher_application_areas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_id BIGINT NOT NULL,
  area VARCHAR(80) NOT NULL,
  UNIQUE KEY uniq_teacher_application_area (application_id, area),
  CONSTRAINT fk_teacher_application_areas_application
    FOREIGN KEY (application_id) REFERENCES teacher_applications(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS teacher_application_availability (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_id BIGINT NOT NULL,
  availability_label VARCHAR(80) NOT NULL,
  UNIQUE KEY uniq_teacher_application_availability (application_id, availability_label),
  CONSTRAINT fk_teacher_application_availability_application
    FOREIGN KEY (application_id) REFERENCES teacher_applications(id)
    ON DELETE CASCADE
);
