function errorHandler(err, req, res, next) {
  const status = err.status || 500;

  if (status >= 500) {
    console.error('[ERROR]', err);
  }

  res.status(status).json({
    error: err.message || 'Erro interno do servidor.',
    details: err.details,
  });
}

module.exports = errorHandler;
