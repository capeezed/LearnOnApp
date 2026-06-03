const passport = require('passport');
const GoogleStrategy = require('passport-google-oauth20').Strategy;
const db = require('./db');
const authService = require('../services/authService');

if (process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_SECRET) {
  passport.use(new GoogleStrategy({
    clientID: process.env.GOOGLE_CLIENT_ID,
    clientSecret: process.env.GOOGLE_CLIENT_SECRET,
    callbackURL: process.env.GOOGLE_CALLBACK_URL,
  }, async (accessToken, refreshToken, profile, done) => {
    try {
      const email = profile.emails?.[0]?.value;
      const name = profile.displayName || email;
      if (!email) return done(new Error('Google nao retornou e-mail.'), null);

      const [rows] = await db.query('SELECT * FROM students WHERE email = ? LIMIT 1', [email]);
      let student = rows[0];

      if (!student) {
        const [result] = await db.query(
          'INSERT INTO students (name, email, password_hash) VALUES (?, ?, ?)',
          [name, email, 'google-oauth']
        );
        student = { id: result.insertId, name, email };
      }

      const tokens = await authService.issueTokenPair(student, 'student');
      return done(null, {
        token: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        name: student.name,
      });
    } catch (err) {
      return done(err, null);
    }
  }));
}

module.exports = passport;
