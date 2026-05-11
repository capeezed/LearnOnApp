const db = require('../config/db');
const AppError = require('../utils/AppError');

const ACCEPT_WINDOW_HOURS = Number(process.env.MATCH_ACCEPT_WINDOW_HOURS || 24);

async function attemptMatch(requestId) {
  const [[request]] = await db.query('SELECT * FROM course_requests WHERE id = ?', [requestId]);
  if (!request || request.status !== 'aguardando_match') return null;

  const [previous] = await db.query('SELECT instructor_id FROM matches WHERE request_id = ?', [requestId]);
  const used = previous.map((item) => item.instructor_id);

  let sql = `
    SELECT DISTINCT i.id, i.name, i.rating_avg
    FROM instructors i
    JOIN instructor_expertise ie ON ie.instructor_id = i.id
    WHERE ie.topic_tag = ? AND i.is_active = TRUE
  `;
  const params = [request.topic_tag];

  if (used.length) {
    sql += ` AND i.id NOT IN (${used.map(() => '?').join(',')})`;
    params.push(...used);
  }

  sql += ' ORDER BY i.rating_avg DESC LIMIT 1';
  const [instructors] = await db.query(sql, params);

  if (!instructors.length) {
    await db.query(
      "UPDATE course_requests SET status = 'aguardando_instrutor' WHERE id = ?",
      [requestId]
    );
    return null;
  }

  const instructor = instructors[0];
  const expiresAt = new Date(Date.now() + ACCEPT_WINDOW_HOURS * 60 * 60 * 1000);
  const [result] = await db.query(
    `INSERT INTO matches (request_id, instructor_id, expires_at, match_attempt)
     VALUES (?, ?, ?, ?)`,
    [requestId, instructor.id, expiresAt, previous.length + 1]
  );

  await db.query(
    "UPDATE course_requests SET status = 'aguardando_instrutor' WHERE id = ?",
    [requestId]
  );

  return { match_id: result.insertId, instructor };
}

async function respondToMatch(matchId, instructorId, accepted) {
  const [[match]] = await db.query(
    'SELECT * FROM matches WHERE id = ? AND instructor_id = ?',
    [matchId, instructorId]
  );
  if (!match) throw new AppError('Match nao encontrado.', 404);
  if (match.status !== 'pending_acceptance') throw new AppError('Este match ja foi respondido.', 409);

  if (!accepted) {
    await db.query("UPDATE matches SET status = 'declined' WHERE id = ?", [matchId]);
    await db.query("UPDATE course_requests SET status = 'aguardando_match' WHERE id = ?", [match.request_id]);
    await attemptMatch(match.request_id);
    return { message: 'Match recusado. Buscando proximo instrutor.' };
  }

  await db.query("UPDATE matches SET status = 'accepted', accepted_at = NOW() WHERE id = ?", [matchId]);
  await db.query("UPDATE course_requests SET status = 'em_andamento' WHERE id = ?", [match.request_id]);

  return { message: 'Match aceito.', request_id: match.request_id };
}

async function claimRequest(requestId, instructorId) {
  const [[request]] = await db.query(
    `SELECT *
     FROM course_requests
     WHERE id = ? AND status IN ('aguardando_match', 'aguardando_instrutor')`,
    [requestId]
  );
  if (!request) throw new AppError('Pedido indisponivel para este instrutor.', 404);

  const [[instructor]] = await db.query(
    `SELECT i.id
     FROM instructors i
     JOIN instructor_expertise ie ON ie.instructor_id = i.id
     WHERE i.id = ? AND i.is_active = TRUE AND ie.topic_tag = ?
     LIMIT 1`,
    [instructorId, request.topic_tag]
  );
  if (!instructor) throw new AppError('Este pedido nao corresponde as suas areas de conhecimento.', 403);

  const [[existingAccepted]] = await db.query(
    "SELECT id FROM matches WHERE request_id = ? AND status = 'accepted' LIMIT 1",
    [requestId]
  );
  if (existingAccepted) throw new AppError('Pedido ja foi aceito por outro instrutor.', 409);

  const expiresAt = new Date(Date.now() + ACCEPT_WINDOW_HOURS * 60 * 60 * 1000);
  const [previous] = await db.query('SELECT id FROM matches WHERE request_id = ?', [requestId]);
  const [result] = await db.query(
    `INSERT INTO matches (request_id, instructor_id, expires_at, match_attempt)
     VALUES (?, ?, ?, ?)`,
    [requestId, instructorId, expiresAt, previous.length + 1]
  );

  await db.query("UPDATE matches SET status = 'accepted', accepted_at = NOW() WHERE id = ?", [result.insertId]);
  await db.query("UPDATE course_requests SET status = 'em_andamento' WHERE id = ?", [requestId]);
  await db.query('UPDATE instructors SET active_matches = COALESCE(active_matches, 0) + 1 WHERE id = ?', [instructorId]);

  return { message: 'Pedido assumido.', match_id: result.insertId, request_id: Number(requestId) };
}

module.exports = { attemptMatch, respondToMatch, claimRequest };
