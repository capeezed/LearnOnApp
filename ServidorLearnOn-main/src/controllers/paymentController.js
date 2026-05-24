const asyncHandler = require('../utils/asyncHandler');
const paymentService = require('../services/paymentService');

const createCoursePayment = asyncHandler(async (req, res) => {
  const payment = await paymentService.createCheckout(req.user, Number(req.params.courseId));
  res.status(201).json(payment);
});

const getMyPayment = asyncHandler(async (req, res) => {
  const payment = await paymentService.findMine(req.user.id, Number(req.params.id));
  res.json(payment);
});

const mercadoPagoWebhook = asyncHandler(async (req, res) => {
  const paymentId = req.query['data.id'] || req.query.id || req.body?.data?.id;
  if (!paymentId) return res.status(200).json({ received: true, ignored: true });

  await paymentService.syncApprovedPayment(String(paymentId), req);
  res.status(200).json({ received: true });
});

module.exports = {
  createCoursePayment,
  getMyPayment,
  mercadoPagoWebhook,
};
