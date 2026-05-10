const db = require('../config/db');
const asyncHandler = require('../utils/asyncHandler');
const AppError = require('../utils/AppError');

const publishCourse = asyncHandler(async (req, res) => {
  const { request_id, title, description, video_url, thumbnail_url, duration_minutes, price } = req.body;
  const instructorId = req.user.id;

  const [[match]] = await db.query(
    `SELECT m.* FROM matches m
     JOIN course_requests cr ON cr.id = m.request_id
     WHERE cr.id = ? AND m.instructor_id = ? AND m.status = 'accepted'`,
    [request_id, instructorId]
  );
  if (!match) throw new AppError('Voce nao tem permissao para entregar este curso.', 403);

  const [result] = await db.query(
    `INSERT INTO courses
       (request_id, instructor_id, title, description, format, video_url,
        thumbnail_url, duration_minutes, price, status, published_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'published', NOW())`,
    [
      request_id,
      instructorId,
      title,
      description,
      match.final_format || 'recorded',
      video_url,
      thumbnail_url,
      duration_minutes,
      price,
    ]
  );

  await db.query("UPDATE course_requests SET status = 'concluido' WHERE id = ?", [request_id]);

  const [[request]] = await db.query('SELECT student_id FROM course_requests WHERE id = ?', [request_id]);
  await db.query(
    'INSERT IGNORE INTO enrollments (student_id, course_id) VALUES (?, ?)',
    [request.student_id, result.insertId]
  );

  res.status(201).json({ course_id: result.insertId });
});

const listPublic = asyncHandler(async (req, res) => {
  const { topic, page = 1, limit = 12 } = req.query;
  const offset = (Number(page) - 1) * Number(limit);
  const params = [];
  let where = "WHERE c.status IN ('published', 'public')";

  if (topic) {
    where += ' AND (c.title LIKE ? OR c.description LIKE ?)';
    params.push(`%${topic}%`, `%${topic}%`);
  }

  const [rows] = await db.query(
    `SELECT c.id, c.title, c.description, c.format, c.duration_minutes,
            c.price, c.thumbnail_url, c.published_at,
            i.name AS instructor_name, i.rating_avg AS instructor_rating
     FROM courses c
     JOIN instructors i ON i.id = c.instructor_id
     ${where}
     ORDER BY c.published_at DESC
     LIMIT ? OFFSET ?`,
    [...params, Number(limit), offset]
  );
  res.json(rows);
});

const getCourse = asyncHandler(async (req, res) => {
  const [[course]] = await db.query(
    `SELECT c.*, i.name AS instructor_name, i.bio AS instructor_bio, i.rating_avg
     FROM courses c
     JOIN instructors i ON i.id = c.instructor_id
     WHERE c.id = ?`,
    [req.params.id]
  );
  if (!course) throw new AppError('Curso nao encontrado.', 404);
  res.json(course);
});

const myCourses = asyncHandler(async (req, res) => {
  const [rows] = await db.query(
    `SELECT c.id, c.title, c.format, c.duration_minutes, c.thumbnail_url,
            cr.topic_tag,
            COALESCE(p.percent_complete, 0) AS progress,
            p.last_position_sec, e.enrolled_at
     FROM enrollments e
     JOIN courses c ON c.id = e.course_id
     LEFT JOIN course_requests cr ON cr.id = c.request_id
     LEFT JOIN progress p ON p.enrollment_id = e.id
     WHERE e.student_id = ?
     ORDER BY e.enrolled_at DESC`,
    [req.user.id]
  );
  res.json(rows);
});

const saveProgress = asyncHandler(async (req, res) => {
  const { watched_seconds = 0, total_seconds = 0, last_position_sec = 0 } = req.body;
  const percent = total_seconds > 0 ? Math.min(100, (watched_seconds / total_seconds) * 100) : 0;

  const [[enrollment]] = await db.query(
    'SELECT id FROM enrollments WHERE student_id = ? AND course_id = ?',
    [req.user.id, req.params.id]
  );
  if (!enrollment) throw new AppError('Voce nao esta matriculado neste curso.', 403);

  await db.query(
    `UPDATE progress
     SET watched_seconds = ?, total_seconds = ?, percent_complete = ?, last_position_sec = ?
     WHERE enrollment_id = ?`,
    [watched_seconds, total_seconds, percent.toFixed(2), last_position_sec, enrollment.id]
  );

  if (percent >= 90) {
    await db.query('UPDATE enrollments SET completed_at = NOW() WHERE id = ? AND completed_at IS NULL', [enrollment.id]);
  }

  res.json({ percent_complete: percent.toFixed(2) });
});

module.exports = { publishCourse, listPublic, getCourse, myCourses, saveProgress };
