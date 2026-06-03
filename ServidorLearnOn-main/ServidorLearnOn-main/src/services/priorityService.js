const db = require('../config/db');

const OPEN_STATUSES = ['aguardando_match', 'aguardando_instrutor'];

const WEIGHTS = {
  similarDemand: 8,
  waitingDay: 1.6,
  fastTrackUrgency: 45,
  normalUrgency: 0,
  instructorAvailability: 6,
};

function daysBetween(start, end) {
  return Math.max(0, (end.getTime() - new Date(start).getTime()) / (1000 * 60 * 60 * 24));
}

function calculatePriorityScore(input) {
  const now = input.now || new Date();
  const waitingDays = daysBetween(input.createdAt, now);

  // Pedidos similares aumentam a prioridade porque um curso pode atender varios alunos.
  // A raiz quadrada reduz distorcao quando ha muitos pedidos repetidos.
  const similarDemandScore = Math.sqrt(Math.max(0, input.similarRequestsCount)) * WEIGHTS.similarDemand;

  // Tempo na fila cresce de forma continua para evitar que pedidos normais fiquem esquecidos.
  const waitingTimeScore = waitingDays * WEIGHTS.waitingDay;

  // Fast-track tem fila separada e tambem ganha boost dentro da propria fila.
  const isFastTrack = input.urgency === 'fast_track';
  const urgencyScore = isFastTrack ? WEIGHTS.fastTrackUrgency : WEIGHTS.normalUrgency;

  // Disponibilidade mede a proporcao de instrutores compativeis com capacidade livre.
  const availabilityRatio = input.availableInstructors <= 0
    ? 0
    : input.availableInstructors / Math.max(1, input.totalCompatibleInstructors);
  const availabilityScore = availabilityRatio * WEIGHTS.instructorAvailability;

  const totalScore = similarDemandScore + waitingTimeScore + urgencyScore + availabilityScore;

  return {
    similarDemandScore: Number(similarDemandScore.toFixed(4)),
    waitingTimeScore: Number(waitingTimeScore.toFixed(4)),
    urgencyScore: Number(urgencyScore.toFixed(4)),
    availabilityScore: Number(availabilityScore.toFixed(4)),
    totalScore: Number(totalScore.toFixed(4)),
    queueType: isFastTrack ? 'fast_track' : 'normal',
    explanation: {
      similarRequestsCount: input.similarRequestsCount,
      waitingDays: Number(waitingDays.toFixed(4)),
      urgency: input.urgency,
      availableInstructors: input.availableInstructors,
      totalCompatibleInstructors: input.totalCompatibleInstructors,
      weights: WEIGHTS,
    },
  };
}

async function getPriorityInputs(requestId) {
  const [[request]] = await db.query('SELECT * FROM course_requests WHERE id = ?', [requestId]);
  if (!request) throw new Error('Pedido nao encontrado para calculo de prioridade.');

  const [[similar]] = await db.query(
    `SELECT COUNT(*) AS total
     FROM course_requests
     WHERE id <> ? AND topic_tag = ? AND status IN (?, ?)`,
    [request.id, request.topic_tag, ...OPEN_STATUSES]
  );

  const [[availability]] = await db.query(
    `SELECT
       COUNT(DISTINCT i.id) AS total_compatible,
       COUNT(DISTINCT CASE
         WHEN i.is_active = TRUE
          AND COALESCE(i.active_matches, 0) < COALESCE(i.max_active_matches, 3)
         THEN i.id END) AS available
     FROM instructors i
     JOIN instructor_expertise ie ON ie.instructor_id = i.id
     WHERE ie.topic_tag = ?`,
    [request.topic_tag]
  );

  return {
    request,
    similarRequestsCount: Number(similar.total || 0),
    availableInstructors: Number(availability.available || 0),
    totalCompatibleInstructors: Number(availability.total_compatible || 0),
  };
}

async function calculateAndPersist(requestId) {
  const input = await getPriorityInputs(requestId);
  const score = calculatePriorityScore({
    createdAt: input.request.created_at,
    urgency: input.request.urgency,
    similarRequestsCount: input.similarRequestsCount,
    availableInstructors: input.availableInstructors,
    totalCompatibleInstructors: input.totalCompatibleInstructors,
  });

  await db.query(
    `INSERT INTO queue_scores
      (request_id, demand_score, age_score, urgency_score, availability_score,
       total_score, queue_type, calculated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
     ON DUPLICATE KEY UPDATE
       demand_score = VALUES(demand_score),
       age_score = VALUES(age_score),
       urgency_score = VALUES(urgency_score),
       availability_score = VALUES(availability_score),
       total_score = VALUES(total_score),
       queue_type = VALUES(queue_type),
       calculated_at = NOW()`,
    [
      requestId,
      score.similarDemandScore,
      score.waitingTimeScore,
      score.urgencyScore,
      score.availabilityScore,
      score.totalScore,
      score.queueType,
    ]
  );

  await db.query(
    'UPDATE course_requests SET priority_score = ?, queue_type = ? WHERE id = ?',
    [score.totalScore, score.queueType, requestId]
  );

  return score;
}

async function listQueue(queueType = 'normal', limit = 20) {
  const [rows] = await db.query(
    `SELECT cr.*, qs.total_score, qs.queue_type, s.name AS student_name
     FROM course_requests cr
     LEFT JOIN queue_scores qs ON qs.request_id = cr.id
     JOIN students s ON s.id = cr.student_id
     WHERE cr.queue_type = ? AND cr.status IN (?, ?)
     ORDER BY cr.priority_score DESC, cr.created_at ASC
     LIMIT ?`,
    [queueType, ...OPEN_STATUSES, Number(limit)]
  );
  return rows;
}

module.exports = {
  calculatePriorityScore,
  calculateAndPersist,
  listQueue,
  OPEN_STATUSES,
  WEIGHTS,
};
