const db = require('../config/db');
const AppError = require('../utils/AppError');
const { normalizeFormat, normalizeUrgency, normalizeStatus, requestToApi } = require('../utils/mappers');
const priorityService = require('./priorityService');

function normalizeCategory(payload) {
  return (payload.category || payload.topic_tag || '').trim().toLowerCase();
}

async function create(studentId, payload) {
  const category = normalizeCategory(payload);
  if (!category) throw new AppError('Categoria obrigatoria.', 422);

  const formatPreference = normalizeFormat(payload.format_preference || payload.format);
  const urgency = normalizeUrgency(payload.urgency);
  if (!formatPreference) throw new AppError('Formato invalido.', 422);
  if (!urgency) throw new AppError('Urgencia invalida.', 422);

  const queueType = urgency === 'fast_track' ? 'fast_track' : 'normal';

  const [result] = await db.query(
    `INSERT INTO course_requests
       (student_id, title, description, topic_tag, format_preference, urgency, status, queue_type)
     VALUES (?, ?, ?, ?, ?, ?, 'aguardando_match', ?)`,
    [studentId, payload.title, payload.description, category, formatPreference, urgency, queueType]
  );

  const requestId = result.insertId;
  const score = await priorityService.calculateAndPersist(requestId);

  const [[request]] = await db.query('SELECT * FROM course_requests WHERE id = ?', [requestId]);

  return {
    ...requestToApi(request),
    score: score.totalScore,
    priority: score,
    message: 'Pedido criado. Estamos priorizando e procurando instrutores compativeis.',
  };
}

async function listMine(studentId) {
  const [rows] = await db.query(
    `SELECT cr.*, qs.total_score
     FROM course_requests cr
     LEFT JOIN queue_scores qs ON qs.request_id = cr.id
     WHERE cr.student_id = ?
     ORDER BY cr.created_at DESC`,
    [studentId]
  );
  return rows.map(requestToApi);
}

async function getMine(studentId, id) {
  const [[row]] = await db.query(
    `SELECT cr.*, qs.total_score, qs.explanation
     FROM course_requests cr
     LEFT JOIN queue_scores qs ON qs.request_id = cr.id
     WHERE cr.id = ? AND cr.student_id = ?`,
    [id, studentId]
  );
  if (!row) throw new AppError('Pedido nao encontrado.', 404);
  return requestToApi(row);
}

async function updateStatus(id, status) {
  const normalizedStatus = normalizeStatus(status);
  if (!normalizedStatus) throw new AppError('Status invalido.', 422);

  await db.query('UPDATE course_requests SET status = ? WHERE id = ?', [normalizedStatus, id]);
  const [[request]] = await db.query('SELECT * FROM course_requests WHERE id = ?', [id]);
  return requestToApi(request);
}

async function recalculate(id) {
  return priorityService.calculateAndPersist(id);
}

async function listQueue(queue, limit) {
  const queueType = String(queue || 'normal').toLowerCase().replace('-', '_') === 'fast_track'
    ? 'fast_track'
    : 'normal';
  const rows = await priorityService.listQueue(queueType, Number(limit || 20));
  return rows.map((row) => ({
    ...requestToApi(row),
    student: { name: row.student_name },
  }));
}

module.exports = {
  create,
  listMine,
  getMine,
  updateStatus,
  recalculate,
  listQueue,
};
