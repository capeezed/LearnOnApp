const db = require('../config/db');
const matchService = require('../services/matchService');
const asyncHandler = require('../utils/asyncHandler');

const listPendingMatches = asyncHandler(async (req, res) => {
  const [rows] = await db.query(
    `SELECT m.id, m.request_id, m.expires_at, m.match_attempt,
            cr.title, cr.description, cr.topic_tag, cr.format_preference, cr.urgency,
            s.name AS student_name
     FROM matches m
     JOIN course_requests cr ON cr.id = m.request_id
     JOIN students s ON s.id = cr.student_id
     WHERE m.instructor_id = ? AND m.status = 'pending_acceptance'
     ORDER BY m.created_at DESC`,
    [req.user.id]
  );
  res.json(rows);
});

const respond = asyncHandler(async (req, res) => {
  const result = await matchService.respondToMatch(req.params.id, req.user.id, Boolean(req.body.accepted));
  res.json(result);
});

const claimRequest = asyncHandler(async (req, res) => {
  const result = await matchService.claimRequest(req.params.id, req.user.id);
  res.status(201).json(result);
});

const confirmFormat = asyncHandler(async (req, res) => {
  const { format } = req.body;
  await db.query('UPDATE matches SET final_format = ? WHERE id = ?', [format, req.params.id]);
  res.json({ message: 'Formato confirmado.', final_format: format });
});

module.exports = { listPendingMatches, respond, claimRequest, confirmFormat };
