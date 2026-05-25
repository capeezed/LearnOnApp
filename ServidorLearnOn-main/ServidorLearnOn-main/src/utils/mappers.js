const roleAliases = {
  aluno: 'student',
  student: 'student',
  instrutor: 'instructor',
  instructor: 'instructor',
  admin: 'admin',
};

const roleToApi = {
  student: 'aluno',
  instructor: 'instrutor',
  admin: 'admin',
};

const formatAliases = {
  live: 'live',
  ao_vivo: 'live',
  recorded: 'recorded',
  gravado: 'recorded',
  no_preference: 'no_preference',
  sem_preferencia: 'no_preference',
};

const urgencyAliases = {
  normal: 'normal',
  fast_track: 'fast_track',
  'fast-track': 'fast_track',
};

const statusAliases = {
  aguardando_match: 'aguardando_match',
  aguardando_instrutor: 'aguardando_instrutor',
  em_andamento: 'em_andamento',
  concluido: 'concluido',
  cancelado: 'cancelado',
};

function normalizeRole(role) {
  return roleAliases[String(role || '').toLowerCase()] || null;
}

function normalizeFormat(format) {
  return formatAliases[String(format || 'no_preference').toLowerCase()] || null;
}

function normalizeUrgency(urgency) {
  return urgencyAliases[String(urgency || 'normal').toLowerCase()] || null;
}

function normalizeStatus(status) {
  return statusAliases[String(status || '').toLowerCase()] || null;
}

function publicUser(user, role = user.role) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    role: roleToApi[role] || role,
  };
}

function requestToApi(request) {
  return {
    id: request.id,
    title: request.title,
    description: request.description,
    category: request.category || request.topic_tag,
    topic_tag: request.topic_tag || request.category,
    format_preference: request.format_preference,
    urgency: request.urgency,
    status: request.status,
    priority_score: Number(request.priority_score || request.total_score || 0),
    queue_type: request.queue_type || (request.urgency === 'fast_track' ? 'fast_track' : 'normal'),
    created_at: request.created_at,
  };
}

module.exports = {
  normalizeRole,
  normalizeFormat,
  normalizeUrgency,
  normalizeStatus,
  publicUser,
  requestToApi,
  roleToApi,
};
