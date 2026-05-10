const jwt = require('jsonwebtoken');
const db = require('../config/db');
const AppError = require('../utils/AppError');
const { normalizeRole } = require('../utils/mappers');

const roleTable = {
  student: 'students',
  instructor: 'instructors',
  admin: 'admins',
};

async function auth(req, res, next) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return next(new AppError('Token nao fornecido.', 401));
  }

  try {
    const token = header.slice('Bearer '.length);
    const payload = jwt.verify(token, process.env.JWT_ACCESS_SECRET || process.env.JWT_SECRET);
    const role = normalizeRole(payload.role);
    const table = roleTable[role];
    if (!table) return next(new AppError('Role invalida no token.', 401));

    const [rows] = await db.query(
      `SELECT id, name, email, is_active FROM ${table} WHERE id = ? LIMIT 1`,
      [payload.sub || payload.id]
    );
    const user = rows[0];
    if (!user || user.is_active === 0) return next(new AppError('Usuario nao encontrado ou inativo.', 401));

    req.user = { id: user.id, name: user.name, email: user.email, role };
    next();
  } catch {
    next(new AppError('Token invalido ou expirado.', 401));
  }
}

function role(...roles) {
  const allowed = roles.map(normalizeRole).filter(Boolean);
  return (req, res, next) => {
    if (!req.user || !allowed.includes(req.user.role)) {
      return next(new AppError('Acesso nao autorizado.', 403));
    }
    next();
  };
}

module.exports = { auth, role };
