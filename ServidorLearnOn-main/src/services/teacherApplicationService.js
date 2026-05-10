const db = require('../config/db');
const AppError = require('../utils/AppError');

function unique(values = []) {
  return [...new Set(values.map((value) => String(value).trim()).filter(Boolean))];
}

function applicationToApi(row, areas = [], availability = []) {
  return {
    id: row.id,
    full_name: row.full_name,
    email: row.email,
    phone: row.phone,
    profile_photo_url: row.profile_photo_url,
    bio: row.bio,
    location: row.location,
    knowledge_areas: areas,
    subjects: row.subjects,
    experience_level: row.experience_level,
    years_experience: row.years_experience,
    linkedin_url: row.linkedin_url,
    github_url: row.github_url,
    portfolio_url: row.portfolio_url,
    class_format: row.class_format,
    weekly_availability: availability,
    suggested_price_range: row.suggested_price_range,
    average_response_time: row.average_response_time,
    document_url: row.document_url,
    certificate_url: row.certificate_url,
    accepted_terms: Boolean(row.accepted_terms),
    status: row.status,
    review_notes: row.review_notes,
    reviewed_at: row.reviewed_at,
    created_at: row.created_at,
  };
}

async function create(payload) {
  const [pending] = await db.query(
    "SELECT id FROM teacher_applications WHERE email = ? AND status = 'pending' LIMIT 1",
    [payload.email]
  );
  if (pending.length) throw new AppError('Ja existe uma candidatura pendente para este e-mail.', 409);

  const areas = unique(payload.knowledge_areas);
  const availability = unique(payload.weekly_availability);
  const conn = await db.getConnection();

  try {
    await conn.beginTransaction();

    const [result] = await conn.query(
      `INSERT INTO teacher_applications
        (full_name, email, phone, profile_photo_url, bio, location, subjects,
         experience_level, years_experience, linkedin_url, github_url, portfolio_url,
         class_format, suggested_price_range, average_response_time, document_url,
         certificate_url, accepted_terms)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        payload.full_name,
        payload.email,
        payload.phone,
        payload.profile_photo_uri || null,
        payload.bio,
        payload.location,
        payload.subjects,
        payload.experience_level,
        Number(payload.years_experience || 0),
        payload.linkedin_url,
        payload.github_url,
        payload.portfolio_url || null,
        payload.class_format,
        payload.suggested_price_range,
        payload.average_response_time,
        payload.document_uri,
        payload.certificate_uri || null,
        Boolean(payload.accepted_terms),
      ]
    );

    if (areas.length) {
      await conn.query(
        'INSERT INTO teacher_application_areas (application_id, area) VALUES ?',
        [areas.map((area) => [result.insertId, area])]
      );
    }

    if (availability.length) {
      await conn.query(
        'INSERT INTO teacher_application_availability (application_id, availability_label) VALUES ?',
        [availability.map((item) => [result.insertId, item])]
      );
    }

    await conn.commit();

    return {
      id: result.insertId,
      status: 'pending',
      message: 'Candidatura enviada para analise.',
    };
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

async function list({ status, limit = 50 } = {}) {
  const params = [];
  let where = '';
  if (status) {
    where = 'WHERE status = ?';
    params.push(status);
  }
  params.push(Number(limit));

  const [applications] = await db.query(
    `SELECT * FROM teacher_applications
     ${where}
     ORDER BY created_at DESC
     LIMIT ?`,
    params
  );

  if (!applications.length) return [];

  const ids = applications.map((item) => item.id);
  const [areas] = await db.query(
    'SELECT application_id, area FROM teacher_application_areas WHERE application_id IN (?)',
    [ids]
  );
  const [availability] = await db.query(
    'SELECT application_id, availability_label FROM teacher_application_availability WHERE application_id IN (?)',
    [ids]
  );

  return applications.map((item) => applicationToApi(
    item,
    areas.filter((area) => area.application_id === item.id).map((area) => area.area),
    availability.filter((row) => row.application_id === item.id).map((row) => row.availability_label)
  ));
}

async function updateStatus(id, status, reviewNotes = null) {
  const [result] = await db.query(
    `UPDATE teacher_applications
     SET status = ?, review_notes = ?, reviewed_at = NOW()
     WHERE id = ?`,
    [status, reviewNotes || null, id]
  );
  if (!result.affectedRows) throw new AppError('Candidatura nao encontrada.', 404);

  return {
    id: Number(id),
    status,
    message: status === 'approved' ? 'Candidatura aprovada.' : 'Candidatura rejeitada.',
  };
}

module.exports = { create, list, updateStatus };
