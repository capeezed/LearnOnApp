const cron = require('node-cron');
const db = require('../config/db');
const priorityService = require('../services/priorityService');

cron.schedule('*/30 * * * *', async () => {
  console.log('[CRON] Recalculando prioridade das filas...');
  const limit = Number(process.env.PRIORITY_RECALC_LIMIT || 250);
  const [requests] = await db.query(
    `SELECT id FROM course_requests
     WHERE status IN (?, ?)
     ORDER BY created_at ASC
     LIMIT ?`,
    [...priorityService.OPEN_STATUSES, limit]
  );

  for (const request of requests) {
    await priorityService.calculateAndPersist(request.id);
  }
});

cron.schedule('*/15 * * * *', async () => {
  console.log('[CRON] Expirando matches pendentes...');
  const [expired] = await db.query(
    `SELECT id, request_id FROM matches
     WHERE status = 'pending_acceptance' AND expires_at <= NOW()`
  );

  for (const match of expired) {
    await db.query("UPDATE matches SET status = 'expired' WHERE id = ?", [match.id]);
    await db.query("UPDATE course_requests SET status = 'aguardando_match' WHERE id = ?", [match.request_id]);
  }
});

console.log('[CRON] Jobs agendados.');
