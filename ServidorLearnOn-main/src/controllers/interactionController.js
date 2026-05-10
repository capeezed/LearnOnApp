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

const notImplementedYet = asyncHandler(async (req, res) => {
  res.status(501).json({ error: 'Modulo de interacoes ainda nao foi alterado nesta etapa.' });
});

module.exports = {
  createSchedule,
  listSchedules,
  postQuestion: notImplementedYet,
  listQuestions: notImplementedYet,
  postAnswer: notImplementedYet,
  postReview: notImplementedYet,
};
