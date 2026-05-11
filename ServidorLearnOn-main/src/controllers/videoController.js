const streamifier = require('streamifier');
const cloudinary = require('cloudinary').v2;
const db = require('../config/db');
const asyncHandler = require('../utils/asyncHandler');
const AppError = require('../utils/AppError');

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

function videoToApi(video) {
  return {
    id: video.id,
    course_id: video.course_id,
    title: video.title,
    description: video.description,
    video_url: video.video_url,
    thumbnail_url: video.thumbnail_url,
    duration: video.duration,
    order_index: video.order_index,
    created_at: video.created_at,
    progress: video.progress ? {
      watched_seconds: Number(video.watched_seconds || 0),
      total_seconds: Number(video.total_seconds || video.duration || 0),
      percent_complete: Number(video.percent_complete || 0),
      last_position_sec: Number(video.last_position_sec || 0),
      completed_at: video.completed_at,
    } : undefined,
  };
}

function requireCloudinary() {
  if (!process.env.CLOUDINARY_CLOUD_NAME || !process.env.CLOUDINARY_API_KEY || !process.env.CLOUDINARY_API_SECRET) {
    throw new AppError('Cloudinary nao configurado. Defina CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY e CLOUDINARY_API_SECRET.', 500);
  }
}

function uploadBuffer(file, courseId) {
  requireCloudinary();
  return new Promise((resolve, reject) => {
    const upload = cloudinary.uploader.upload_stream(
      {
        resource_type: 'video',
        folder: `learnon/courses/${courseId}`,
        eager: [{ width: 640, crop: 'scale', format: 'jpg' }],
        eager_async: false,
      },
      (error, result) => (error ? reject(error) : resolve(result))
    );
    streamifier.createReadStream(file.buffer).pipe(upload);
  });
}

async function instructorOwnsCourse(courseId, instructorId) {
  const [[course]] = await db.query(
    'SELECT id, status, instructor_id FROM courses WHERE id = ? LIMIT 1',
    [courseId]
  );
  if (!course) throw new AppError('Curso nao encontrado.', 404);
  if (Number(course.instructor_id) !== Number(instructorId)) {
    throw new AppError('Apenas o instrutor dono do curso pode gerenciar videos.', 403);
  }
  return course;
}

async function resolveStudentEnrollment(courseId, studentId) {
  const [[enrollment]] = await db.query(
    `SELECT e.id, c.status
     FROM enrollments e
     JOIN courses c ON c.id = e.course_id
     WHERE e.course_id = ? AND e.student_id = ?
     LIMIT 1`,
    [courseId, studentId]
  );
  if (!enrollment) throw new AppError('Voce nao esta matriculado neste curso.', 403);
  if (enrollment.status !== 'published') throw new AppError('Videos disponiveis apenas em cursos publicados.', 403);
  return enrollment;
}

async function canReadCourseVideos(courseId, user) {
  if (user.role === 'instructor') {
    return instructorOwnsCourse(courseId, user.id);
  }
  if (user.role === 'student') {
    return resolveStudentEnrollment(courseId, user.id);
  }
  throw new AppError('Acesso nao autorizado.', 403);
}

async function canReadVideo(videoId, user) {
  const [[video]] = await db.query('SELECT * FROM course_videos WHERE id = ? LIMIT 1', [videoId]);
  if (!video) throw new AppError('Video nao encontrado.', 404);
  const access = await canReadCourseVideos(video.course_id, user);
  return { video, access };
}

const createVideo = asyncHandler(async (req, res) => {
  const courseId = Number(req.params.id);
  await instructorOwnsCourse(courseId, req.user.id);

  const upload = req.file ? await uploadBuffer(req.file, courseId) : null;
  const videoUrl = upload?.secure_url || req.body.video_url;
  if (!videoUrl) throw new AppError('Envie um arquivo de video ou informe video_url.', 422);

  const thumbnailUrl = req.body.thumbnail_url
    || upload?.eager?.[0]?.secure_url
    || (upload?.public_id ? cloudinary.url(upload.public_id, { resource_type: 'video', format: 'jpg', width: 640, crop: 'scale' }) : null);
  const duration = Number(req.body.duration || Math.round(upload?.duration || 0)) || null;

  const [result] = await db.query(
    `INSERT INTO course_videos
       (course_id, title, description, video_url, thumbnail_url, duration, order_index)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [
      courseId,
      req.body.title,
      req.body.description || null,
      videoUrl,
      thumbnailUrl,
      duration,
      Number(req.body.order_index || 0),
    ]
  );

  const [[video]] = await db.query('SELECT * FROM course_videos WHERE id = ?', [result.insertId]);
  res.status(201).json(videoToApi(video));
});

const listCourseVideos = asyncHandler(async (req, res) => {
  const courseId = Number(req.params.id);
  const access = await canReadCourseVideos(courseId, req.user);
  const enrollmentId = req.user.role === 'student' ? access.id : null;

  const [rows] = await db.query(
    `SELECT cv.*,
            vp.watched_seconds, vp.total_seconds, vp.percent_complete, vp.last_position_sec, vp.completed_at,
            CASE WHEN vp.id IS NULL THEN 0 ELSE 1 END AS progress
     FROM course_videos cv
     LEFT JOIN video_progress vp ON vp.video_id = cv.id AND vp.enrollment_id = ?
     WHERE cv.course_id = ?
     ORDER BY cv.order_index ASC, cv.id ASC`,
    [enrollmentId, courseId]
  );
  res.json(rows.map(videoToApi));
});

const getVideo = asyncHandler(async (req, res) => {
  const { video, access } = await canReadVideo(req.params.id, req.user);
  if (req.user.role !== 'student') return res.json(videoToApi(video));

  const [[progress]] = await db.query(
    'SELECT * FROM video_progress WHERE video_id = ? AND enrollment_id = ? LIMIT 1',
    [video.id, access.id]
  );
  res.json(videoToApi({ ...video, ...(progress || {}), progress: Boolean(progress) }));
});

const updateVideo = asyncHandler(async (req, res) => {
  const [[video]] = await db.query('SELECT * FROM course_videos WHERE id = ? LIMIT 1', [req.params.id]);
  if (!video) throw new AppError('Video nao encontrado.', 404);
  await instructorOwnsCourse(video.course_id, req.user.id);

  await db.query(
    `UPDATE course_videos
     SET title = COALESCE(?, title),
         description = COALESCE(?, description),
         thumbnail_url = COALESCE(?, thumbnail_url),
         duration = COALESCE(?, duration),
         order_index = COALESCE(?, order_index)
     WHERE id = ?`,
    [
      req.body.title || null,
      req.body.description || null,
      req.body.thumbnail_url || null,
      req.body.duration ? Number(req.body.duration) : null,
      req.body.order_index !== undefined ? Number(req.body.order_index) : null,
      req.params.id,
    ]
  );

  const [[updated]] = await db.query('SELECT * FROM course_videos WHERE id = ?', [req.params.id]);
  res.json(videoToApi(updated));
});

const deleteVideo = asyncHandler(async (req, res) => {
  const [[video]] = await db.query('SELECT * FROM course_videos WHERE id = ? LIMIT 1', [req.params.id]);
  if (!video) throw new AppError('Video nao encontrado.', 404);
  await instructorOwnsCourse(video.course_id, req.user.id);
  await db.query('DELETE FROM course_videos WHERE id = ?', [req.params.id]);
  res.status(204).send();
});

const saveVideoProgress = asyncHandler(async (req, res) => {
  const { video, access } = await canReadVideo(req.params.id, req.user);
  if (req.user.role !== 'student') throw new AppError('Apenas alunos salvam progresso de video.', 403);

  const watchedSeconds = Number(req.body.watched_seconds || 0);
  const totalSeconds = Number(req.body.total_seconds || video.duration || 0);
  const lastPositionSec = Number(req.body.last_position_sec || watchedSeconds || 0);
  const percent = totalSeconds > 0 ? Math.min(100, (watchedSeconds / totalSeconds) * 100) : 0;

  await db.query(
    `INSERT INTO video_progress
       (enrollment_id, video_id, watched_seconds, total_seconds, percent_complete, last_position_sec, completed_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)
     ON DUPLICATE KEY UPDATE
       watched_seconds = GREATEST(watched_seconds, VALUES(watched_seconds)),
       total_seconds = VALUES(total_seconds),
       percent_complete = GREATEST(percent_complete, VALUES(percent_complete)),
       last_position_sec = VALUES(last_position_sec),
       completed_at = IF(VALUES(percent_complete) >= 90, COALESCE(completed_at, NOW()), completed_at)`,
    [access.id, video.id, watchedSeconds, totalSeconds, percent.toFixed(2), lastPositionSec, percent >= 90 ? new Date() : null]
  );

  res.json({ percent_complete: percent.toFixed(2), last_position_sec: lastPositionSec });
});

module.exports = {
  createVideo,
  listCourseVideos,
  getVideo,
  updateVideo,
  deleteVideo,
  saveVideoProgress,
};
