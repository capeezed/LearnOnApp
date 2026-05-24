const https = require('https');
const { URL } = require('url');
const AppError = require('../utils/AppError');

const BASE_URL = 'https://api.mercadopago.com';

function request(method, path, body = null, headers = {}) {
  const accessToken = process.env.MERCADO_PAGO_ACCESS_TOKEN;
  if (!accessToken) {
    throw new AppError('Mercado Pago nao configurado. Defina MERCADO_PAGO_ACCESS_TOKEN.', 500);
  }

  const url = new URL(path, BASE_URL);
  const payload = body ? JSON.stringify(body) : null;

  return new Promise((resolve, reject) => {
    const req = https.request(
      {
        method,
        hostname: url.hostname,
        path: `${url.pathname}${url.search}`,
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Accept: 'application/json',
          ...(payload ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) } : {}),
          ...headers,
        },
      },
      (res) => {
        let data = '';
        res.setEncoding('utf8');
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          let parsed = null;
          try {
            parsed = data ? JSON.parse(data) : null;
          } catch {
            parsed = { raw: data };
          }

          if (res.statusCode >= 400) {
            const message = parsed?.message || parsed?.error || 'Erro ao comunicar com Mercado Pago.';
            reject(new AppError(message, res.statusCode, parsed));
            return;
          }

          resolve(parsed);
        });
      }
    );

    req.on('error', (err) => reject(new AppError('Falha de conexao com Mercado Pago.', 502, { cause: err.message })));
    if (payload) req.write(payload);
    req.end();
  });
}

function createPreference(preference, idempotencyKey) {
  return request('POST', '/checkout/preferences', preference, {
    'X-Idempotency-Key': idempotencyKey,
  });
}

function getPayment(paymentId) {
  return request('GET', `/v1/payments/${encodeURIComponent(paymentId)}`);
}

module.exports = { createPreference, getPayment };
