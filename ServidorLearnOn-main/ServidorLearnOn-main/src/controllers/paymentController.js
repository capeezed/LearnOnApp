const asyncHandler = require('../utils/asyncHandler');
const paymentService = require('../services/paymentService');

const createCoursePayment = asyncHandler(async (req, res) => {
  const courseId = Number(req.params.courseId);

  const payment = await paymentService.createCoursePayment(req.user, courseId);

  res.status(201).json(payment);
});

const getMyPayment = asyncHandler(async (req, res) => {
  const paymentId = Number(req.params.id);

  const payment = await paymentService.getMyPayment(req.user.id, paymentId);

  res.json(payment);
});

const mercadoPagoWebhook = asyncHandler(async (req, res) => {
  const paymentId =
    req.query['data.id'] ||
    req.query.id ||
    req.body?.data?.id;

  if (!paymentId) {
    return res.status(200).json({
      received: true,
      ignored: true,
    });
  }

  await paymentService.handleMercadoPagoWebhook(String(paymentId));

  res.status(200).json({
    received: true,
  });
});

module.exports = {
  createCoursePayment,
  getMyPayment,
  mercadoPagoWebhook,
};