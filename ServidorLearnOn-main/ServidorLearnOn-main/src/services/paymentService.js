const crypto = require('crypto');
const db = require('../config/db');
const AppError = require('../utils/AppError');
const mercadoPago = require('./mercadoPagoClient');

function normalizeStatus(status) {
  if (status === 'paid' || status === 'credited') return 'approved';
  return status || 'pending';
}

function paymentToApi(payment) {
  if (!payment) return null;

  const sandbox = String(process.env.MERCADO_PAGO_SANDBOX || '').toLowerCase() === 'true';
  const checkoutUrl = sandbox && payment.sandbox_checkout_url
    ? payment.sandbox_checkout_url
    : payment.checkout_url;

  return {
    id: payment.id,
    course_id: payment.course_id,
    status: normalizeStatus(payment.status),
    provider: 'mercado_pago',
    provider_preference_id: payment.gateway_ref,
    provider_payment_id: null,
    amount: Number(payment.amount || 0),
    currency: 'BRL',
    checkout_url: checkoutUrl,
    paid_at: payment.paid_at,
    created_at: payment.created_at,
    updated_at: payment.updated_at || payment.created_at,
  };
}

function buildNotificationUrl() {
  if (process.env.MERCADO_PAGO_NOTIFICATION_URL) return process.env.MERCADO_PAGO_NOTIFICATION_URL;
  if (!process.env.PUBLIC_API_URL) return null;
  return `${process.env.PUBLIC_API_URL.replace(/\/+$/, '')}/api/payments/webhooks/mercado-pago`;
}

function buildBackUrls() {
  const base = process.env.PAYMENT_RETURN_URL || process.env.FRONTEND_URL;
  if (!base) return undefined;

  const clean = base.replace(/\/+$/, '');
  return {
    success: `${clean}/payments/success`,
    failure: `${clean}/payments/failure`,
    pending: `${clean}/payments/pending`,
  };
}

function parseSignature(header) {
  return String(header || '').split(',').reduce((acc, part) => {
    const [key, value] = part.split('=');
    if (key && value) acc[key.trim()] = value.trim();
    return acc;
  }, {});
}

function timingSafeEqualHex(a, b) {
  const left = Buffer.from(String(a || ''), 'hex');
  const right = Buffer.from(String(b || ''), 'hex');
  return left.length === right.length && crypto.timingSafeEqual(left, right);
}

function validateMercadoPagoSignature(req, paymentId) {
  const secret = process.env.MERCADO_PAGO_WEBHOOK_SECRET;
  if (!secret) return;

  const xSignature = req.get('x-signature');
  const xRequestId = req.get('x-request-id');
  const parts = parseSignature(xSignature);

  if (!xSignature || !xRequestId || !parts.ts || !parts.v1) {
    throw new AppError('Assinatura do webhook ausente.', 401);
  }

  const toleranceSeconds = Number(process.env.MERCADO_PAGO_WEBHOOK_TOLERANCE_SECONDS || 600);
  const timestamp = Number(parts.ts);
  if (!Number.isFinite(timestamp) || Math.abs(Math.floor(Date.now() / 1000) - timestamp) > toleranceSeconds) {
    throw new AppError('Webhook expirado.', 401);
  }

  const manifest = `id:${paymentId};request-id:${xRequestId};ts:${parts.ts};`;
  const expected = crypto.createHmac('sha256', secret).update(manifest).digest('hex');
  if (!timingSafeEqualHex(expected, parts.v1)) {
    throw new AppError('Assinatura do webhook invalida.', 401);
  }
}

async function unlockCourseAccess(conn, payment) {
  const [enrollmentResult] = await conn.query(
    'INSERT IGNORE INTO enrollments (student_id, course_id) VALUES (?, ?)',
    [payment.student_id, payment.course_id]
  );

  if (enrollmentResult.insertId) {
    await conn.query(
      `INSERT INTO notifications (user_type, user_id, type, title, body)
       VALUES ('student', ?, 'course_unlocked', 'Curso liberado', 'Seu pagamento foi aprovado e o curso ja esta disponivel em Meus cursos.')`,
      [payment.student_id]
    );

    await conn.query(
      `INSERT IGNORE INTO progress (enrollment_id, watched_seconds, total_seconds, percent_complete)
       VALUES (?, 0, 0, 0)`,
      [enrollmentResult.insertId]
    ).catch(() => {});
  }
}

async function createCheckout(student, courseId) {
  const conn = await db.getConnection();

  try {
    await conn.beginTransaction();

    const [[course]] = await conn.query(
      `SELECT c.id, c.request_id, c.title, c.description, c.price, c.status, c.thumbnail_url,
              i.name AS instructor_name
       FROM courses c
       JOIN instructors i ON i.id = c.instructor_id
       WHERE c.id = ?
       FOR UPDATE`,
      [courseId]
    );

    if (!course || course.status !== 'published') {
      throw new AppError('Curso nao encontrado ou indisponivel.', 404);
    }

    const [[approved]] = await conn.query(
      `SELECT *
       FROM payments
       WHERE student_id = ? AND course_id = ? AND status IN ('paid', 'credited')
       ORDER BY id DESC
       LIMIT 1`,
      [student.id, course.id]
    );

    if (approved) {
      await unlockCourseAccess(conn, approved);
      await conn.commit();
      return paymentToApi(approved);
    }

    const [[enrollment]] = await conn.query(
      'SELECT id FROM enrollments WHERE student_id = ? AND course_id = ? LIMIT 1',
      [student.id, course.id]
    );

    if (enrollment && Number(course.price || 0) <= 0) {
      throw new AppError('Voce ja possui acesso a este curso.', 409);
    }

    const [[pending]] = await conn.query(
      `SELECT *
       FROM payments
       WHERE student_id = ? AND course_id = ? AND status = 'pending'
       ORDER BY id DESC
       LIMIT 1`,
      [student.id, course.id]
    );

    if (pending) {
      await conn.commit();
      return paymentToApi(pending);
    }

    const amount = Number(course.price || 0);
    if (!Number.isFinite(amount) || amount <= 0) {
      throw new AppError('Curso sem preco valido para pagamento.', 422);
    }

    const reference = `learnon-course-${course.id}-student-${student.id}-${Date.now()}`;

    const [result] = await conn.query(
      `INSERT INTO payments
         (student_id, request_id, course_id, amount, type, status, gateway_ref)
       VALUES (?, ?, ?, ?, 'catalog_purchase', 'pending', ?)`,
      [student.id, course.request_id || null, course.id, amount.toFixed(2), reference]
    );

    const paymentId = result.insertId;
    const notificationUrl = buildNotificationUrl();
    const backUrls = buildBackUrls();

    const preferenceBody = {
      items: [{
        id: String(course.id),
        title: course.title,
        description: course.description || `Microcurso LearnOn com ${course.instructor_name}`,
        quantity: 1,
        currency_id: 'BRL',
        unit_price: Number(amount.toFixed(2)),
        picture_url: course.thumbnail_url || undefined,
      }],
      payer: {
        name: student.name,
        email: student.email,
      },
      external_reference: reference,
      metadata: {
        local_payment_id: paymentId,
        course_id: course.id,
        student_id: student.id,
      },
      notification_url: notificationUrl || undefined,
      back_urls: backUrls,
      auto_return: backUrls ? 'approved' : undefined,
      statement_descriptor: process.env.MERCADO_PAGO_STATEMENT_DESCRIPTOR || 'LEARNON',
    };

    const preference = await mercadoPago.createPreference(preferenceBody, `learnon-payment-${paymentId}`);

    await conn.query(
      `UPDATE payments
       SET checkout_url = ?, sandbox_checkout_url = ?, raw_provider_response = ?
       WHERE id = ?`,
      [
        preference.init_point,
        preference.sandbox_init_point,
        JSON.stringify(preference),
        paymentId,
      ]
    );

    const [[created]] = await conn.query('SELECT * FROM payments WHERE id = ?', [paymentId]);

    await conn.commit();
    return paymentToApi(created);
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

async function findMine(studentId, paymentId) {
  const [[payment]] = await db.query(
    'SELECT * FROM payments WHERE id = ? AND student_id = ? LIMIT 1',
    [paymentId, studentId]
  );

  if (!payment) throw new AppError('Pagamento nao encontrado.', 404);
  return paymentToApi(payment);
}

async function syncApprovedPayment(providerPaymentId, req = null) {
  if (!providerPaymentId) throw new AppError('ID do pagamento nao informado.', 400);
  if (req) validateMercadoPagoSignature(req, providerPaymentId);

  const providerPayment = await mercadoPago.getPayment(providerPaymentId);
  const externalReference = providerPayment.external_reference;
  if (!externalReference) throw new AppError('Pagamento sem referencia externa.', 422);

  const conn = await db.getConnection();

  try {
    await conn.beginTransaction();

    const [[payment]] = await conn.query(
      'SELECT * FROM payments WHERE gateway_ref = ? FOR UPDATE',
      [externalReference]
    );

    if (!payment) throw new AppError('Pagamento local nao encontrado.', 404);

    const providerStatus = String(providerPayment.status || 'pending');
    const providerAmount = Number(providerPayment.transaction_amount || 0);
    const expectedAmount = Number(payment.amount || 0);

    if (providerStatus === 'approved' && providerAmount + 0.001 < expectedAmount) {
      throw new AppError('Valor pago menor que o valor do curso.', 409);
    }

    const paidAt = providerStatus === 'approved'
      ? (providerPayment.date_approved ? new Date(providerPayment.date_approved) : new Date())
      : null;

    let nextStatus = 'pending';
    if (providerStatus === 'approved') nextStatus = 'paid';
    if (providerStatus === 'refunded' || providerStatus === 'charged_back') nextStatus = 'refunded';

    await conn.query(
      `UPDATE payments
       SET status = ?, paid_at = COALESCE(paid_at, ?), raw_provider_response = ?
       WHERE id = ?`,
      [
        nextStatus,
        paidAt,
        JSON.stringify(providerPayment),
        payment.id,
      ]
    );

    if (providerStatus === 'approved') {
      await unlockCourseAccess(conn, payment);
    }

    const [[updated]] = await conn.query('SELECT * FROM payments WHERE id = ?', [payment.id]);

    await conn.commit();
    return paymentToApi(updated);
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

module.exports = {
  createCheckout,
  findMine,
  syncApprovedPayment,

  createCoursePayment: createCheckout,
  getMyPayment: findMine,
  handleMercadoPagoWebhook: syncApprovedPayment,
};
