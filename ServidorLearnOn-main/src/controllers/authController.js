const authService = require('../services/authService');
const asyncHandler = require('../utils/asyncHandler');

const registerStudent = asyncHandler(async (req, res) => {
  const result = await authService.register({ ...req.validated, role: 'aluno' });
  res.status(201).json(result);
});

const loginStudent = asyncHandler(async (req, res) => {
  const result = await authService.login({ ...req.validated, role: 'aluno' });
  res.json({ ...result, student: result.user });
});

const registerInstructor = asyncHandler(async (req, res) => {
  const result = await authService.register({ ...req.validated, role: 'instrutor' });
  res.status(201).json(result);
});

const loginInstructor = asyncHandler(async (req, res) => {
  const result = await authService.login({ ...req.validated, role: 'instrutor' });
  res.json({ ...result, instructor: result.user });
});

const registerAdmin = asyncHandler(async (req, res) => {
  const result = await authService.register({ ...req.validated, role: 'admin' });
  res.status(201).json(result);
});

const loginAdmin = asyncHandler(async (req, res) => {
  const result = await authService.login({ ...req.validated, role: 'admin' });
  res.json({ ...result, admin: result.user });
});

const refresh = asyncHandler(async (req, res) => {
  const result = await authService.refresh(req.validated.refreshToken);
  res.json(result);
});

const logout = asyncHandler(async (req, res) => {
  await authService.logout(req.body.refreshToken);
  res.status(204).send();
});

module.exports = {
  registerStudent,
  loginStudent,
  registerInstructor,
  loginInstructor,
  registerAdmin,
  loginAdmin,
  refresh,
  logout,
};
