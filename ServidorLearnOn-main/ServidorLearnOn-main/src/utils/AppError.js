class AppError extends Error {
  constructor(message, status = 500, details = undefined) {
    super(message);
    this.name = 'AppError';
    this.status = status;
    this.details = details;
  }
}

module.exports = AppError;
