const db = require('../config/db');
const asyncHandler = require('../utils/asyncHandler');
const AppError = require('../utils/AppError');

const createSchedule = asyncHandler(async (req, res) => {
  const { course_id, scheduled_at, duration_min, meeting_url } = req.body;
  const [[course]] = await db.query(
    'SELECT * FROM courses WHERE id = ? AND instructor_id = ?',
    [course_id, req.user.id]
  );
  if (!course) throw new AppError('Curso nao encontrado.', 403);

  const [result] = await db.query(
    'INSERT INTO schedules (course_id, scheduled_at, duration_min, meeting_url) VALUES (?, ?, ?, ?)',
    [course_id, scheduled_at, duration_min || 60, meeting_url]
  );

  res.status(201).json({ schedule_id: result.insertId });
});

const listSchedules = asyncHandler(async (req, res) => {
  const params = [req.user.id];
  const where = req.user.role === 'student'
    ? 'WHERE e.student_id = ?'
    : 'WHERE c.instructor_id = ?';

  const [rows] = await db.query(
    `SELECT s.id, s.scheduled_at, s.duration_min, s.meeting_url, s.status,
            c.title AS course_title, i.name AS instructor_name
     FROM schedules s
     JOIN courses c ON c.id = s.course_id
     JOIN instructors i ON i.id = c.instructor_id
     LEFT JOIN enrollments e ON e.course_id = c.id
     ${where}
     ORDER BY s.scheduled_at ASC`,
    params
  );
  res.json(rows);
});

const listQuestions = asyncHandler(async (req, res) => {
  const { courseId } = req.params;
  const params = [courseId];
  let ownership = '';

  if (req.user.role === 'student') {
    ownership = 'AND q.student_id = ?';
    params.push(req.user.id);
  } else if (req.user.role === 'instructor') {
    ownership = 'AND c.instructor_id = ?';
    params.push(req.user.id);
  }

  const [rows] = await db.query(
    `SELECT q.id, q.question, q.is_resolved, q.created_at,
            s.name AS student_name, c.title AS course_title
     FROM qa_questions q
     JOIN courses c ON c.id = q.course_id
     JOIN students s ON s.id = q.student_id
     WHERE q.course_id = ? ${ownership}
     ORDER BY q.created_at DESC`,
    params
  );
  res.json(rows);
});

const postQuestion = asyncHandler(async (req, res) => {
  const { courseId } = req.params;
  const { question } = req.body;
  if (!question || question.trim().length < 3) throw new AppError('Pergunta obrigatoria.', 422);

  const [[enrollment]] = await db.query(
    'SELECT id FROM enrollments WHERE student_id = ? AND course_id = ? LIMIT 1',
    [req.user.id, courseId]
  );
  if (!enrollment) throw new AppError('Voce nao esta matriculado neste curso.', 403);

  const [result] = await db.query(
    'INSERT INTO qa_questions (course_id, student_id, question) VALUES (?, ?, ?)',
    [courseId, req.user.id, question.trim()]
  );

  res.status(201).json({ id: result.insertId, message: 'Pergunta enviada.' });
});

const postAnswer = asyncHandler(async (req, res) => {
  const { questionId } = req.params;
  const { answer } = req.body;
  if (!answer || answer.trim().length < 3) throw new AppError('Resposta obrigatoria.', 422);

  const [[question]] = await db.query(
    `SELECT q.id
     FROM qa_questions q
     JOIN courses c ON c.id = q.course_id
     WHERE q.id = ? AND c.instructor_id = ?`,
    [questionId, req.user.id]
  );
  if (!question) throw new AppError('Pergunta nao encontrada para este instrutor.', 404);

  await db.query(
    'INSERT INTO qa_answers (question_id, instructor_id, answer) VALUES (?, ?, ?)',
    [questionId, req.user.id, answer.trim()]
  );
  await db.query('UPDATE qa_questions SET is_resolved = TRUE WHERE id = ?', [questionId]);

  res.status(201).json({ message: 'Resposta enviada.' });
});

const postReview = asyncHandler(async (req, res) => {
  const { courseId } = req.params;
  const { rating, comment } = req.body;
  const numericRating = Number(rating);
  if (!numericRating || numericRating < 1 || numericRating > 5) throw new AppError('Nota deve estar entre 1 e 5.', 422);

  const [[enrollment]] = await db.query(
    'SELECT id FROM enrollments WHERE student_id = ? AND course_id = ? LIMIT 1',
    [req.user.id, courseId]
  );
  if (!enrollment) throw new AppError('Voce nao esta matriculado neste curso.', 403);

  await db.query(
    `INSERT INTO reviews (course_id, student_id, doubt_resolved, rating, comment)
     VALUES (?, ?, TRUE, ?, ?)
     ON DUPLICATE KEY UPDATE rating = VALUES(rating), comment = VALUES(comment), doubt_resolved = TRUE, created_at = CURRENT_TIMESTAMP`,
    [courseId, req.user.id, numericRating, comment || null]
  );

  const [[avg]] = await db.query(
    `SELECT AVG(r.rating) AS rating_avg
     FROM reviews r
     JOIN courses c ON c.id = r.course_id
     WHERE c.instructor_id = (SELECT instructor_id FROM courses WHERE id = ?)`,
    [courseId]
  );
  await db.query(
    'UPDATE instructors SET rating_avg = ? WHERE id = (SELECT instructor_id FROM courses WHERE id = ?)',
    [Number(avg.rating_avg || 0).toFixed(2), courseId]
  );

  res.status(201).json({ message: 'Avaliacao enviada.' });
});

module.exports = {
  createSchedule,
  listSchedules,
  postQuestion,
  listQuestions,
  postAnswer,
  postReview,
};
