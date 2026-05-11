const courseRequestService = require('../services/courseRequestService');
const asyncHandler = require('../utils/asyncHandler');

const createRequest = asyncHandler(async (req, res) => {
  const result = await courseRequestService.create(req.user.id, req.validated);
  res.status(201).json(result);
});

const listMyRequests = asyncHandler(async (req, res) => {
  const result = await courseRequestService.listMine(req.user.id);
  res.json(result);
});

const getRequest = asyncHandler(async (req, res) => {
  const result = await courseRequestService.getMine(req.user.id, req.validated.id);
  res.json(result);
});

const updateStatus = asyncHandler(async (req, res) => {
  const result = await courseRequestService.updateStatus(req.validated.id, req.validated.status);
  res.json(result);
});

const recalculatePriority = asyncHandler(async (req, res) => {
  const result = await courseRequestService.recalculate(req.validated.id);
  res.json(result);
});

const listQueue = asyncHandler(async (req, res) => {
  const result = req.user.role === 'instructor'
    ? await courseRequestService.listQueueForInstructor(req.validated.queue, req.validated.limit, req.user.id)
    : await courseRequestService.listQueue(req.validated.queue, req.validated.limit);
  res.json(result);
});

module.exports = {
  createRequest,
  listMyRequests,
  getRequest,
  updateStatus,
  recalculatePriority,
  listQueue,
};
