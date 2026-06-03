const { validationResult, matchedData } = require('express-validator');

function validate(req, res, next) {
  const result = validationResult(req);
  if (!result.isEmpty()) {
    return res.status(422).json({
      error: 'Dados invalidos.',
      details: result.array().map((item) => ({
        field: item.path,
        message: item.msg,
      })),
    });
  }

  req.validated = {
    ...req.validated,
    ...matchedData(req, { locations: ['body', 'params', 'query'] }),
  };
  next();
}

module.exports = validate;
