-- LearnOn - preencher perfil/expertise de instrutores ja aprovados.
-- Seguro para rodar mais de uma vez.

INSERT IGNORE INTO instructor_expertise (instructor_id, topic_tag)
SELECT
  i.id,
  LOWER(TRIM(taa.area)) AS topic_tag
FROM instructors i
JOIN teacher_applications ta ON ta.email = i.email AND ta.status = 'approved'
JOIN teacher_application_areas taa ON taa.application_id = ta.id
WHERE TRIM(taa.area) <> '';

UPDATE instructors i
JOIN teacher_applications ta ON ta.email = i.email AND ta.status = 'approved'
SET
  i.bio = COALESCE(NULLIF(i.bio, ''), ta.bio),
  i.avatar_url = COALESCE(NULLIF(i.avatar_url, ''), ta.profile_photo_url)
WHERE ta.id = (
  SELECT MAX(ta2.id)
  FROM teacher_applications ta2
  WHERE ta2.email = i.email AND ta2.status = 'approved'
);
