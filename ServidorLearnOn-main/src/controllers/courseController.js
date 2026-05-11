const db = require('../config/db');
const asyncHandler = require('../utils/asyncHandler');
const AppError = require('../utils/AppError');

function courseToApi(course, interestedCount = 0, enrolledCount = 0) {
  return {
    ...course,
    course_id: course.id,
    status: String(course.status || '').toUpperCase(),
    interested_students_count: Number(interestedCount || 0),
    enrolled_students_count: Number(enrolledCount || 0),
  };
}

async function resolveInterestedStudents(conn, course) {
  if (course.request_group_id) {
    const [rows] = await conn.query(
      `SELECT DISTINCT cr.student_id
       FROM request_group_members rgm
       JOIN course_requests cr ON cr.id = rgm.request_id
       WHERE rgm.group_id = ?`,
      [course.request_group_id]
    );
    return rows.map((row) => row.student_id);
  }

  const requestId = course.origin_request_id || course.request_id;
  if (!requestId) return [];

  const [rows] = await conn.query(
    'SELECT DISTINCT student_id FROM course_requests WHERE id = ?',
    [requestId]
  );
  return rows.map((row) => row.student_id);
}

async function validateInstructorCanUseRequest(conn, requestId, instructorId) {
  const [[match]] = await conn.query(
    `SELECT m.*
     FROM matches m
     WHERE m.request_id = ? AND m.instructor_id = ? AND m.status = 'accepted'
     LIMIT 1`,
    [requestId, instructorId]
  );
  if (!match) throw new AppError('Voce nao tem permissao para criar curso para este pedido.', 403);
  return match;
}

async function validateInstructorCanUseGroup(conn, groupId, instructorId) {
  const [[group]] = await conn.query(
    'SELECT id, topic_tag FROM request_groups WHERE id = ? LIMIT 1',
    [groupId]
  );
  if (!group) throw new AppError('Grupo de pedidos nao encontrado.', 404);

  const [[expertise]] = await conn.query(
    'SELECT id FROM instructor_expertise WHERE instructor_id = ? AND topic_tag = ? LIMIT 1',
    [instructorId, group.topic_tag]
  );
  if (!expertise) throw new AppError('Este grupo nao corresponde as suas areas de conhecimento.', 403);

  return group;
}

const createCourse = asyncHandler(async (req, res) => {
  const {
    request_id,
    origin_request_id,
    request_group_id,
    title,
    description,
    video_url,
    thumbnail_url,
    duration_minutes,
    price,
    format,
  } = req.body;
  const instructorId = req.user.id;
  const originRequestId = origin_request_id || request_id || null;
  const conn = await db.getConnection();

  try {
    await conn.beginTransaction();

    let match = null;
    if (originRequestId) {
      match = await validateInstructorCanUseRequest(conn, originRequestId, instructorId);
    }
    if (request_group_id) {
      await validateInstructorCanUseGroup(conn, request_group_id, instructorId);
    }
    if (!originRequestId && !request_group_id) {
      throw new AppError('Informe origin_request_id/request_id ou request_group_id.', 422);
    }

    const effectiveFormat = format || match?.final_format || 'recorded';
    const [result] = await conn.query(
      `INSERT INTO courses
         (request_id, origin_request_id, request_group_id, instructor_id, title, description,
          format, video_url, thumbnail_url, duration_minutes, price, status)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'draft')`,
      [
        originRequestId,
        originRequestId,
        request_group_id || null,
        instructorId,
        title,
        description,
        effectiveFormat === 'live' ? 'live' : 'recorded',
        video_url || null,
        thumbnail_url || null,
        duration_minutes || null,
        price || 0,
      ]
    );

    const [[course]] = await conn.query('SELECT * FROM courses WHERE id = ?', [result.insertId]);
    const interested = await resolveInterestedStudents(conn, course);
    await conn.commit();

    res.status(201).json(courseToApi(course, interested.length, 0));
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
});

const publishCourse = asyncHandler(async (req, res) => {
  const courseId = req.params.id;
  const instructorId = req.user.id;
  const conn = await db.getConnection();

  try {
    await conn.beginTransaction();

    const [[course]] = await conn.query(
      'SELECT * FROM courses WHERE id = ? AND instructor_id = ? FOR UPDATE',
      [courseId, instructorId]
    );
    if (!course) throw new AppError('Curso nao encontrado para este instrutor.', 404);
    if (course.status === 'archived') throw new AppError('Curso arquivado nao pode ser publicado.', 409);

    const studentIds = await resolveInterestedStudents(conn, course);
    if (studentIds.length) {
      await conn.query(
        'INSERT IGNORE INTO enrollments (student_id, course_id) VALUES ?',
        [studentIds.map((studentId) => [studentId, course.id])]
      );
      await conn.query(
        `INSERT INTO notifications (user_type, user_id, type, title, body)
         VALUES ?`,
        [studentIds.map((studentId) => [
          'student',
          studentId,
          'course_published',
          'Seu microcurso foi publicado',
          `O curso "${course.title}" ja esta disponivel em Meus cursos.`,
        ])]
      );
    }

    if (course.request_group_id) {
      await conn.query(
        `UPDATE course_requests cr
         JOIN request_group_members rgm ON rgm.request_id = cr.id
         SET cr.status = 'concluido'
         WHERE rgm.group_id = ?`,
        [course.request_group_id]
      );
    } else if (course.origin_request_id || course.request_id) {
      await conn.query(
        "UPDATE course_requests SET status = 'concluido' WHERE id = ?",
        [course.origin_request_id || course.request_id]
      );
    }

    await conn.query(
      "UPDATE courses SET status = 'published', published_at = COALESCE(published_at, NOW()) WHERE id = ?",
      [course.id]
    );

    const [[published]] = await conn.query('SELECT * FROM courses WHERE id = ?', [course.id]);
    const [[enrolled]] = await conn.query(
      'SELECT COUNT(*) AS total FROM enrollments WHERE course_id = ?',
      [course.id]
    );

    await conn.commit();
    res.json(courseToApi(published, studentIds.length, enrolled.total));
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
});

const listPublic = asyncHandler(async (req, res) => {
  const { topic, page = 1, limit = 12 } = req.query;
  const offset = (Number(page) - 1) * Number(limit);
  const params = [];
  let where = "WHERE c.status = 'published'";

  if (topic) {
    where += ' AND (c.title LIKE ? OR c.description LIKE ?)';
    params.push(`%${topic}%`, `%${topic}%`);
  }

  const [rows] = await db.query(
    `SELECT c.id, c.title, c.description, c.format, c.duration_minutes,
            c.price, c.thumbnail_url, c.published_at, c.status,
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
            cr.topic_tag, c.status,
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

module.exports = { createCourse, publishCourse, listPublic, getCourse, myCourses, saveProgress };
