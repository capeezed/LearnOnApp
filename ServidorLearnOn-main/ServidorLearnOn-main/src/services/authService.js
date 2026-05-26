const bcrypt = require('bcryptjs');
const { OAuth2Client } = require('google-auth-library');
const jwt = require('jsonwebtoken');
const db = require('../config/db');
const AppError = require('../utils/AppError');
const { randomToken, sha256 } = require('../utils/crypto');
const { normalizeRole, publicUser } = require('../utils/mappers');

const SALT_ROUNDS = 12;

const roleTable = {
  student: 'students',
  instructor: 'instructors',
  admin: 'admins',
};

function accessSecret() {
  return process.env.JWT_ACCESS_SECRET || process.env.JWT_SECRET;
}

function refreshSecret() {
  return process.env.JWT_REFRESH_SECRET || process.env.JWT_SECRET;
}

function refreshDays() {
  return Number(process.env.JWT_REFRESH_EXPIRES_IN_DAYS || 30);
}

function signAccessToken(user, role) {
  return jwt.sign(
    { sub: user.id, id: user.id, role, email: user.email },
    accessSecret(),
    { expiresIn: process.env.JWT_ACCESS_EXPIRES_IN || process.env.JWT_EXPIRES_IN || '15m' }
  );
}

function signRefreshToken(user, role) {
  return jwt.sign(
    { sub: user.id, role, nonce: randomToken() },
    refreshSecret(),
    { expiresIn: `${refreshDays()}d` }
  );
}

async function issueTokenPair(user, role, conn = db) {
  const accessToken = signAccessToken(user, role);
  const refreshToken = signRefreshToken(user, role);
  const expiresAt = new Date(Date.now() + refreshDays() * 24 * 60 * 60 * 1000);

  await conn.query(
    `INSERT INTO refresh_tokens (user_type, user_id, token_hash, expires_at)
     VALUES (?, ?, ?, ?)`,
    [role, user.id, sha256(refreshToken), expiresAt]
  );

  return { accessToken, refreshToken, expiresAt };
}

async function register({ name, email, password, role, bio, expertise = [] }) {
  const normalizedRole = normalizeRole(role);
  const table = roleTable[normalizedRole];
  if (!table) throw new AppError('Role invalida.', 422);

  const [exists] = await db.query(`SELECT id FROM ${table} WHERE email = ? LIMIT 1`, [email]);
  if (exists.length) throw new AppError('E-mail ja cadastrado.', 409);

  const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);
  const conn = await db.getConnection();

  try {
    await conn.beginTransaction();

    let result;
    if (normalizedRole === 'instructor') {
      [result] = await conn.query(
        'INSERT INTO instructors (name, email, password_hash, bio) VALUES (?, ?, ?, ?)',
        [name, email, passwordHash, bio || null]
      );

      if (Array.isArray(expertise) && expertise.length) {
        await conn.query(
          'INSERT INTO instructor_expertise (instructor_id, topic_tag) VALUES ?',
          [[...new Set(expertise)].map((tag) => [result.insertId, tag])]
        );
      }
    } else {
      [result] = await conn.query(
        `INSERT INTO ${table} (name, email, password_hash) VALUES (?, ?, ?)`,
        [name, email, passwordHash]
      );
    }

    const user = { id: result.insertId, name, email };
    const tokens = await issueTokenPair(user, normalizedRole, conn);

    await conn.commit();

    return {
      token: tokens.accessToken,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      user: publicUser(user, normalizedRole),
      name,
    };
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

async function login({ email, password, role }) {
  const normalizedRole = normalizeRole(role);
  const table = roleTable[normalizedRole];
  if (!table) throw new AppError('Role invalida.', 422);

  const [rows] = await db.query(`SELECT * FROM ${table} WHERE email = ? LIMIT 1`, [email]);
  const user = rows[0];
  if (!user || !(await bcrypt.compare(password, user.password_hash))) {
    throw new AppError('Credenciais invalidas.', 401);
  }

  const tokens = await issueTokenPair(user, normalizedRole);

  return {
    token: tokens.accessToken,
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    user: publicUser(user, normalizedRole),
    name: user.name,
  };
}

async function loginGoogleMobile(idToken) {
  if (!idToken) throw new AppError('Token do Google e obrigatorio.', 422);
  if (!process.env.GOOGLE_CLIENT_ID) throw new AppError('Google Client ID nao configurado.', 500);

  const client = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);
  let ticket;

  try {
    ticket = await client.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
  } catch {
    throw new AppError('Token do Google invalido.', 401);
  }

  const payload = ticket.getPayload();
  const email = payload?.email;
  const name = payload?.name || email;

  if (!email || payload.email_verified === false) {
    throw new AppError('Google nao retornou um e-mail verificado.', 401);
  }

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const [rows] = await conn.query('SELECT * FROM students WHERE email = ? LIMIT 1', [email]);
    let student = rows[0];

    if (!student) {
      const [result] = await conn.query(
        'INSERT INTO students (name, email, password_hash) VALUES (?, ?, ?)',
        [name, email, 'google-oauth']
      );
      student = { id: result.insertId, name, email };
    }

    const tokens = await issueTokenPair(student, 'student', conn);

    await conn.commit();

    return {
      token: tokens.accessToken,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      user: publicUser(student, 'student'),
      name: student.name,
      student: publicUser(student, 'student'),
    };
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

async function refresh(refreshToken) {
  let payload;
  try {
    payload = jwt.verify(refreshToken, refreshSecret());
  } catch {
    throw new AppError('Refresh token invalido ou expirado.', 401);
  }

  const role = normalizeRole(payload.role);
  const table = roleTable[role];
  const tokenHash = sha256(refreshToken);

  const [storedRows] = await db.query(
    `SELECT * FROM refresh_tokens
     WHERE token_hash = ? AND user_type = ? AND revoked_at IS NULL AND expires_at > NOW()
     LIMIT 1`,
    [tokenHash, role]
  );
  const stored = storedRows[0];
  if (!stored || !table) throw new AppError('Refresh token revogado ou expirado.', 401);

  const [userRows] = await db.query(`SELECT * FROM ${table} WHERE id = ? LIMIT 1`, [stored.user_id]);
  const user = userRows[0];
  if (!user) throw new AppError('Usuario nao encontrado.', 401);

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    await conn.query('UPDATE refresh_tokens SET revoked_at = NOW() WHERE id = ?', [stored.id]);
    const tokens = await issueTokenPair(user, role, conn);
    await conn.commit();

    return {
      token: tokens.accessToken,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      user: publicUser(user, role),
    };
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

async function logout(refreshToken) {
  if (!refreshToken) return;
  await db.query(
    'UPDATE refresh_tokens SET revoked_at = NOW() WHERE token_hash = ? AND revoked_at IS NULL',
    [sha256(refreshToken)]
  );
}

module.exports = { register, login, loginGoogleMobile, refresh, logout, issueTokenPair };
