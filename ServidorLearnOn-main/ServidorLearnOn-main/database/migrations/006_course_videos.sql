-- LearnOn - videos por curso e progresso por aula.
-- Seguro para rodar mais de uma vez.

CREATE TABLE IF NOT EXISTS course_videos (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  course_id INT UNSIGNED NOT NULL,
  title VARCHAR(180) NOT NULL,
  description TEXT NULL,
  video_url TEXT NOT NULL,
  thumbnail_url TEXT NULL,
  duration INT UNSIGNED NULL,
  order_index INT UNSIGNED NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_course_videos_course
    FOREIGN KEY (course_id) REFERENCES courses(id)
    ON DELETE CASCADE
);

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_course_videos_course_order ON course_videos (course_id, order_index, id)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'course_videos'
    AND INDEX_NAME = 'idx_course_videos_course_order'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS video_progress (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  enrollment_id INT UNSIGNED NOT NULL,
  video_id INT UNSIGNED NOT NULL,
  watched_seconds INT UNSIGNED NOT NULL DEFAULT 0,
  total_seconds INT UNSIGNED NOT NULL DEFAULT 0,
  percent_complete DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  last_position_sec INT UNSIGNED NOT NULL DEFAULT 0,
  completed_at TIMESTAMP NULL DEFAULT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_video_progress_enrollment_video (enrollment_id, video_id),
  CONSTRAINT fk_video_progress_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_video_progress_video
    FOREIGN KEY (video_id) REFERENCES course_videos(id)
    ON DELETE CASCADE
);
