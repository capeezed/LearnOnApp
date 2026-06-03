const { body, param, query } = require('express-validator');
const validate = require('../middlewares/validate');

const allowedExperienceLevels = ['Junior', 'Pleno', 'Senior', 'Especialista'];
const allowedClassFormats = ['Ao vivo', 'Gravadas', 'Ambos'];
const allowedStatuses = ['pending', 'approved', 'rejected'];

const create = [
  body('full_name').trim().isLength({ min: 3, max: 120 }).withMessage('Nome completo deve ter entre 3 e 120 caracteres.'),
  body('email').trim().isEmail().normalizeEmail().withMessage('E-mail invalido.'),
  body('phone').trim().isLength({ min: 10, max: 40 }).withMessage('Telefone deve conter DDD.'),
  body('profile_photo_uri').optional({ nullable: true, checkFalsy: true }).trim().isLength({ max: 500 }).withMessage('URL da foto invalida.'),
  body('bio').trim().isLength({ min: 40, max: 2000 }).withMessage('Bio deve ter entre 40 e 2000 caracteres.'),
  body('location').trim().isLength({ min: 4, max: 120 }).withMessage('Cidade/Estado invalido.'),
  body('knowledge_areas').isArray({ min: 1, max: 12 }).withMessage('Selecione pelo menos uma area de conhecimento.'),
  body('knowledge_areas.*').trim().isLength({ min: 2, max: 80 }).withMessage('Area de conhecimento invalida.'),
  body('subjects').trim().isLength({ min: 3, max: 2000 }).withMessage('Informe tecnologias ou materias dominadas.'),
  body('experience_level').isIn(allowedExperienceLevels).withMessage('Nivel de experiencia invalido.'),
  body('years_experience').isInt({ min: 0, max: 80 }).withMessage('Anos de experiencia invalido.'),
  body('linkedin_url').trim().isURL({ require_protocol: true }).withMessage('LinkedIn deve ser uma URL completa.'),
  body('github_url').trim().isURL({ require_protocol: true }).withMessage('GitHub deve ser uma URL completa.'),
  body('portfolio_url').optional({ nullable: true, checkFalsy: true }).trim().isURL({ require_protocol: true }).withMessage('Portfolio deve ser uma URL completa.'),
  body('class_format').isIn(allowedClassFormats).withMessage('Formato de aula invalido.'),
  body('weekly_availability').isArray({ min: 1, max: 14 }).withMessage('Selecione disponibilidade semanal.'),
  body('weekly_availability.*').trim().isLength({ min: 2, max: 80 }).withMessage('Disponibilidade invalida.'),
  body('suggested_price_range').trim().isLength({ min: 3, max: 40 }).withMessage('Faixa de preco invalida.'),
  body('average_response_time').trim().isLength({ min: 2, max: 40 }).withMessage('Tempo medio de resposta invalido.'),
  body('document_uri').trim().isLength({ min: 3, max: 500 }).withMessage('Documento de verificacao obrigatorio.'),
  body('certificate_uri').optional({ nullable: true, checkFalsy: true }).trim().isLength({ max: 500 }).withMessage('Certificado invalido.'),
  body('accepted_terms').isBoolean().custom((value) => value === true).withMessage('Termos devem ser aceitos.'),
  validate,
];

const list = [
  query('status').optional().isIn(allowedStatuses).withMessage('Status invalido.'),
  query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('Limite invalido.'),
  validate,
];

const updateStatus = [
  param('id').isInt({ min: 1 }).withMessage('ID invalido.'),
  body('status').isIn(['approved', 'rejected']).withMessage('Status deve ser approved ou rejected.'),
  body('review_notes').optional({ nullable: true, checkFalsy: true }).trim().isLength({ max: 2000 }).withMessage('Observacao muito longa.'),
  body('temporary_password').optional({ nullable: true, checkFalsy: true }).isLength({ min: 8, max: 120 }).withMessage('Senha temporaria deve ter pelo menos 8 caracteres.'),
  validate,
];

module.exports = { create, list, updateStatus };
