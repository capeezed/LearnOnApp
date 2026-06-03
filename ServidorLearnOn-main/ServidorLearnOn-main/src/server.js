require('dotenv').config();
const express = require('express');
const cors = require('cors');
const routes = require('./routes');
const errorHandler = require('./middlewares/errorHandler');
const db = require('./config/db');
const passport = require('./config/passport');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use(passport.initialize());

app.get('/health', async (req, res, next) => {
  try {
    const conn = await db.getConnection();
    conn.release();
    res.json({ status: 'ok', project: 'LearnOn API' });
  } catch (err) {
    next(err);
  }
});

app.use('/api', routes);
app.use(errorHandler);

if (require.main === module) {
  app.listen(PORT, async () => {
    console.log(`LearnOn API rodando em http://localhost:${PORT}`);
    try {
      const conn = await db.getConnection();
      console.log('Banco de dados conectado com sucesso!');
      conn.release();
      require('./jobs/cronJobs');
    } catch (err) {
      console.error('Erro ao conectar no banco de dados:');
      console.error(`   ${err.message}`);
    }
  });
}

module.exports = app;
