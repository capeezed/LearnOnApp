const { body, param, query } = require('express-validator');
const validate = require('../middlewares/validate');

const formats = ['live', 'ao_vivo', 'recorded', 'gravado', 'no_preference', 'sem_preferencia'];
const urgencies = ['normal', 'fast_track', 'fast-track'];
const statuses = ['aguardando_match', 'aguardando_instrutor', 'em_andamento', 'concluido', 'cancelado'];

const createRequest = [
  body('title').trim().isLength({ min: 4, max: 160 }).withMessage('Titulo deve ter entre 4 e 160 caracteres.'),
  body('description').trim().isLength({ min: 20, max: 4000 }).withMessage('Descricao deve ter entre 20 e 4000 caracteres.'),
  body('category').optional().trim().isLength({ min: 2, max: 80 }).withMessage('Categoria invalida.'),
  body('topic_tag').optional().trim().isLength({ min: 2, max: 80 }).withMessage('Categoria invalida.'),
  body('format_preference').optional().isIn(formats).withMessage('Formato invalido.'),
  body('format').optional().isIn(formats).withMessage('Formato invalido.'),
  body('urgency').optional().isIn(urgencies).withMessage('Urgencia invalida.'),
  validate,
];

const requestId = [
  param('id').isInt({ min: 1 }).withMessage('ID invalido.'),
  validate,
];

const updateStatus = [
  param('id').isInt({ min: 1 }).withMessage('ID invalido.'),
  body('status').isIn(statuses).withMessage('Status invalido.'),
  validate,
];

const listQueue = [
  query('queue').optional().isIn(['normal', 'fast_track', 'fast-track']).withMessage('Fila invalida.'),
  query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('Limite invalido.'),
  validate,
];

module.exports = { createRequest, requestId, updateStatus, listQueue };
