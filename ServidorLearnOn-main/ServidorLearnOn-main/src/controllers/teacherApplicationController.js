const teacherApplicationService = require('../services/teacherApplicationService');
const asyncHandler = require('../utils/asyncHandler');

const create = asyncHandler(async (req, res) => {
  const result = await teacherApplicationService.create(req.validated);
  res.status(201).json(result);
});

const list = asyncHandler(async (req, res) => {
  const result = await teacherApplicationService.list(req.validated);
  res.json(result);
});

const updateStatus = asyncHandler(async (req, res) => {
  const result = await teacherApplicationService.updateStatus(
    req.validated.id,
    req.validated.status,
    req.validated.review_notes,
    req.validated.temporary_password
  );
  res.json(result);
});

module.exports = { create, list, updateStatus };
