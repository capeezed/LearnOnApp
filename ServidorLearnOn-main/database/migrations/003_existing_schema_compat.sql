-- LearnOn - compatibilidade com o schema atual da VM.
-- O dump enviado ja possui:
--   qa_questions, qa_answers, reviews, notifications, instructor_payouts.
-- Portanto esta migration NAO cria tabelas duplicadas de Q&A/reviews.
--
-- Rode apenas se sua VM ainda nao recebeu a migration 001, pois o dump atual
-- de instructors nao possui estas colunas usadas pelo match/agenda.

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE instructors ADD COLUMN active_matches INT UNSIGNED NOT NULL DEFAULT 0',
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
    'ALTER TABLE instructors ADD COLUMN max_active_matches INT UNSIGNED NOT NULL DEFAULT 3',
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
