const { body } = require('express-validator');
const validate = require('../middlewares/validate');

const register = [
  body('name').trim().isLength({ min: 2, max: 120 }).withMessage('Nome deve ter entre 2 e 120 caracteres.'),
  body('email').trim().isEmail().normalizeEmail().withMessage('E-mail invalido.'),
  body('password').isLength({ min: 8 }).withMessage('Senha deve ter pelo menos 8 caracteres.'),
  body('bio').optional().trim().isLength({ max: 1000 }).withMessage('Bio deve ter no maximo 1000 caracteres.'),
  body('expertise').optional().isArray().withMessage('Expertise deve ser uma lista.'),
  body('expertise.*').optional().trim().isLength({ min: 2, max: 80 }).withMessage('Categoria de expertise invalida.'),
  validate,
];

const login = [
  body('email').trim().isEmail().normalizeEmail().withMessage('E-mail invalido.'),
  body('password').notEmpty().withMessage('Senha obrigatoria.'),
  validate,
];

const refresh = [
  body('refreshToken').notEmpty().withMessage('Refresh token obrigatorio.'),
  validate,
];

module.exports = { register, login, refresh };
