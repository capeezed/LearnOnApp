const db = require('../config/db');
const asyncHandler = require('../utils/asyncHandler');

const money = (value) => `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;

const profile = asyncHandler(async (req, res) => {
  const [[instructor]] = await db.query(
    `SELECT id, name, email, bio, rating_avg, active_matches, max_active_matches
     FROM instructors
     WHERE id = ?`,
    [req.user.id]
  );

  const [expertise] = await db.query(
    'SELECT topic_tag FROM instructor_expertise WHERE instructor_id = ? ORDER BY topic_tag',
    [req.user.id]
  );
  const [[application]] = await db.query(
    `SELECT id, linkedin_url, github_url, portfolio_url, class_format
     FROM teacher_applications
     WHERE email = ? AND status = 'approved'
     ORDER BY reviewed_at DESC, id DESC
     LIMIT 1`,
    [instructor.email]
  );
  const [applicationAreas] = application
    ? await db.query(
      'SELECT LOWER(area) AS topic_tag FROM teacher_application_areas WHERE application_id = ? ORDER BY area',
      [application.id]
    )
    : [[]];

  const specialties = expertise.length
    ? expertise.map((item) => item.topic_tag)
    : applicationAreas.map((item) => item.topic_tag);

  const socialLinks = application
    ? [application.linkedin_url, application.github_url, application.portfolio_url].filter(Boolean)
    : [];

  const acceptedFormats = application?.class_format === 'Ao vivo'
    ? ['live']
    : application?.class_format === 'Gravadas'
      ? ['recorded']
      : ['live', 'recorded'];

  res.json({
    id: instructor.id,
    name: instructor.name,
    email: instructor.email,
    bio: instructor.bio || '',
    rating_avg: Number(instructor.rating_avg || 0),
    specialties,
    social_links: socialLinks,
    availability: `${instructor.active_matches || 0}/${instructor.max_active_matches || 3} matches ativos`,
    accepted_formats: acceptedFormats,
  });
});

const courses = asyncHandler(async (req, res) => {
  const [rows] = await db.query(
    `SELECT c.id, c.title, c.description, c.format, c.duration_minutes,
            c.price, c.status, c.published_at, cr.topic_tag,
            CASE
              WHEN c.request_group_id IS NOT NULL THEN (
                SELECT COUNT(DISTINCT crg.student_id)
                FROM request_group_members rgm
                JOIN course_requests crg ON crg.id = rgm.request_id
                WHERE rgm.group_id = c.request_group_id
              )
              WHEN COALESCE(c.origin_request_id, c.request_id) IS NOT NULL THEN 1
              ELSE 0
            END AS interested_students_count
     FROM courses c
     LEFT JOIN course_requests cr ON cr.id = COALESCE(c.origin_request_id, c.request_id)
     WHERE c.instructor_id = ?
     ORDER BY COALESCE(c.published_at, c.id) DESC`,
    [req.user.id]
  );
  res.json(rows);
});

const questions = asyncHandler(async (req, res) => {
  const [rows] = await db.query(
    `SELECT q.id, q.question, q.is_resolved, q.created_at,
            s.name AS student_name, c.title AS course_title
     FROM qa_questions q
     JOIN courses c ON c.id = q.course_id
     JOIN students s ON s.id = q.student_id
     WHERE c.instructor_id = ?
     ORDER BY q.is_resolved ASC, q.created_at DESC`,
    [req.user.id]
  );
  res.json(rows);
});

const reviews = asyncHandler(async (req, res) => {
  const [rows] = await db.query(
    `SELECT r.id, r.rating, r.comment, r.created_at,
            s.name AS student_name, c.title AS course_title
     FROM reviews r
     JOIN courses c ON c.id = r.course_id
     JOIN students s ON s.id = r.student_id
     WHERE c.instructor_id = ?
     ORDER BY r.created_at DESC`,
    [req.user.id]
  );
  res.json(rows);
});

const finance = asyncHandler(async (req, res) => {
  const [[summary]] = await db.query(
    `SELECT COALESCE(SUM(price), 0) AS total_revenue,
            COALESCE(SUM(CASE WHEN status IN ('published', 'public') THEN price ELSE 0 END), 0) AS available_revenue
     FROM courses
     WHERE instructor_id = ?`,
    [req.user.id]
  );

  const [topCourses] = await db.query(
    `SELECT title, COALESCE(price, 0) AS revenue
     FROM courses
     WHERE instructor_id = ?
     ORDER BY COALESCE(price, 0) DESC, id DESC
     LIMIT 5`,
    [req.user.id]
  );

  res.json({
    total_revenue: money(summary.total_revenue),
    pending_revenue: money(Number(summary.total_revenue || 0) - Number(summary.available_revenue || 0)),
    payments: [],
    top_courses: topCourses.map((item) => `${item.title} - ${money(item.revenue)}`),
  });
});

const notifications = asyncHandler(async (req, res) => {
  const [matches] = await db.query(
    `SELECT m.id, cr.title, m.expires_at
     FROM matches m
     JOIN course_requests cr ON cr.id = m.request_id
     WHERE m.instructor_id = ? AND m.status = 'pending_acceptance'
     ORDER BY m.created_at DESC
     LIMIT 5`,
    [req.user.id]
  );

  const [schedules] = await db.query(
    `SELECT s.id, c.title, s.scheduled_at
     FROM schedules s
     JOIN courses c ON c.id = s.course_id
     WHERE c.instructor_id = ? AND s.scheduled_at >= NOW()
     ORDER BY s.scheduled_at ASC
     LIMIT 5`,
    [req.user.id]
  );

  res.json([
    ...matches.map((item) => ({
      id: `match-${item.id}`,
      title: 'Novo match pendente',
      body: item.title,
      created_at: item.expires_at,
    })),
    ...schedules.map((item) => ({
      id: `schedule-${item.id}`,
      title: 'Aula agendada',
      body: item.title,
      created_at: item.scheduled_at,
    })),
  ]);
});

const analytics = asyncHandler(async (req, res) => {
  const [[courseStats]] = await db.query(
    `SELECT COUNT(*) AS total_courses,
            AVG(COALESCE(duration_minutes, 0)) AS avg_duration
     FROM courses
     WHERE instructor_id = ?`,
    [req.user.id]
  );

  const [[reviewStats]] = await db.query(
    `SELECT AVG(r.rating) AS rating_avg
     FROM reviews r
     JOIN courses c ON c.id = r.course_id
     WHERE c.instructor_id = ?`,
    [req.user.id]
  );

  const [categories] = await db.query(
    `SELECT cr.topic_tag, COUNT(*) AS total
     FROM courses c
     LEFT JOIN course_requests cr ON cr.id = c.request_id
     WHERE c.instructor_id = ? AND cr.topic_tag IS NOT NULL
     GROUP BY cr.topic_tag
     ORDER BY total DESC
     LIMIT 5`,
    [req.user.id]
  );

  const [viewed] = await db.query(
    `SELECT title
     FROM courses
     WHERE instructor_id = ?
     ORDER BY published_at DESC
     LIMIT 5`,
    [req.user.id]
  );

  res.json({
    top_viewed_courses: viewed.map((item) => item.title),
    completion_rate: `${Number(courseStats.total_courses || 0)} cursos`,
    average_response_time: reviewStats.rating_avg ? `${Number(reviewStats.rating_avg).toFixed(1)} avaliacao media` : 'Sem avaliacoes',
    top_categories: categories.map((item) => item.topic_tag),
    avg_duration: Number(courseStats.avg_duration || 0),
  });
});

const dashboard = asyncHandler(async (req, res) => {
  const [[pending]] = await db.query(
    "SELECT COUNT(*) AS total FROM matches WHERE instructor_id = ? AND status = 'pending_acceptance'",
    [req.user.id]
  );
  const [[delivered]] = await db.query(
    "SELECT COUNT(*) AS total, COALESCE(SUM(price), 0) AS revenue FROM courses WHERE instructor_id = ?",
    [req.user.id]
  );
  const [[reviewsSummary]] = await db.query(
    `SELECT AVG(r.rating) AS rating_avg
     FROM reviews r
     JOIN courses c ON c.id = r.course_id
     WHERE c.instructor_id = ?`,
    [req.user.id]
  );

  res.json({
    metrics: [
      { label: 'Pedidos pendentes', value: String(pending.total || 0), delta: 'matches abertos' },
      { label: 'Cursos entregues', value: String(delivered.total || 0), delta: 'microcursos publicados' },
      { label: 'Avaliacao media', value: reviewsSummary.rating_avg ? Number(reviewsSummary.rating_avg).toFixed(1) : '0.0', delta: 'reviews reais' },
      { label: 'Faturamento', value: money(delivered.revenue), delta: 'soma dos cursos' },
    ],
  });
});

module.exports = {
  profile,
  courses,
  questions,
  reviews,
  finance,
  notifications,
  analytics,
  dashboard,
};
