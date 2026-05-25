const router = require('express').Router();
const { auth, role } = require('../middlewares/auth');
const passport = require('../config/passport');

const authCtrl = require('../controllers/authController');
const requestCtrl = require('../controllers/requestController');
const matchCtrl = require('../controllers/matchController');
const courseCtrl = require('../controllers/courseController');
const interactionCtrl = require('../controllers/interactionController');
const teacherApplicationCtrl = require('../controllers/teacherApplicationController');
const instructorCtrl = require('../controllers/instructorController');
const videoCtrl = require('../controllers/videoController');
const paymentCtrl = require('../controllers/paymentController');
const multer = require('multer');

const authValidation = require('../validations/authValidation');
const requestValidation = require('../validations/courseRequestValidation');
const teacherApplicationValidation = require('../validations/teacherApplicationValidation');
const videoUpload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: Number(process.env.MAX_VIDEO_UPLOAD_MB || 250) * 1024 * 1024 },
});

router.post('/auth/students/register', authValidation.register, authCtrl.registerStudent);
router.post('/auth/students/login', authValidation.login, authCtrl.loginStudent);
router.post('/auth/instructors/register', authValidation.register, authCtrl.registerInstructor);
router.post('/auth/instructors/login', authValidation.login, authCtrl.loginInstructor);
router.post('/auth/admin/register', auth, role('admin'), authValidation.register, authCtrl.registerAdmin);
router.post('/auth/admin/login', authValidation.login, authCtrl.loginAdmin);
router.post('/auth/refresh', authValidation.refresh, authCtrl.refresh);
router.post('/auth/logout', authCtrl.logout);
router.post(
  '/courses/:courseId/payments',
  auth,
  role('aluno'),
  paymentCtrl.createCoursePayment
);

router.post(
  '/payments/webhooks/mercado-pago',
  paymentCtrl.mercadoPagoWebhook
);

router.get(
  '/payments/:id',
  auth,
  role('aluno'),
  paymentCtrl.getMyPayment
);

router.post('/teacher-applications', teacherApplicationValidation.create, teacherApplicationCtrl.create);
router.get('/teacher-applications', auth, role('admin'), teacherApplicationValidation.list, teacherApplicationCtrl.list);
router.patch('/teacher-applications/:id/status', auth, role('admin'), teacherApplicationValidation.updateStatus, teacherApplicationCtrl.updateStatus);

router.get('/auth/google',
  passport.authenticate('google', { scope: ['profile', 'email'], session: false })
);

router.get('/auth/google/callback',
  passport.authenticate('google', { session: false, failureRedirect: `${process.env.FRONTEND_URL}/login?erro=google` }),
  (req, res) => {
    const { token, refreshToken, name } = req.user;
    res.redirect(`${process.env.FRONTEND_URL}/auth/callback?token=${token}&refreshToken=${refreshToken}&name=${encodeURIComponent(name)}`);
  }
);

router.post('/requests', auth, role('aluno'), requestValidation.createRequest, requestCtrl.createRequest);
router.get('/requests', auth, role('aluno'), requestCtrl.listMyRequests);
router.get('/requests/queue', auth, role('instrutor', 'admin'), requestValidation.listQueue, requestCtrl.listQueue);
router.post('/requests/:id/claim', auth, role('instrutor'), requestValidation.requestId, matchCtrl.claimRequest);
router.get('/requests/:id', auth, role('aluno'), requestValidation.requestId, requestCtrl.getRequest);
router.patch('/requests/:id/status', auth, role('admin'), requestValidation.updateStatus, requestCtrl.updateStatus);
router.post('/requests/:id/recalculate-priority', auth, role('admin'), requestValidation.requestId, requestCtrl.recalculatePriority);

router.get('/matches/pending', auth, role('instrutor'), matchCtrl.listPendingMatches);
router.put('/matches/:id/respond', auth, role('instrutor'), matchCtrl.respond);
router.put('/matches/:id/confirm-format', auth, role('aluno'), matchCtrl.confirmFormat);

router.get('/instructors/me/dashboard', auth, role('instrutor'), instructorCtrl.dashboard);
router.get('/instructors/me/profile', auth, role('instrutor'), instructorCtrl.profile);
router.get('/instructors/me/courses', auth, role('instrutor'), instructorCtrl.courses);
router.get('/instructors/me/questions', auth, role('instrutor'), instructorCtrl.questions);
router.get('/instructors/me/reviews', auth, role('instrutor'), instructorCtrl.reviews);
router.get('/instructors/me/finance', auth, role('instrutor'), instructorCtrl.finance);
router.get('/instructors/me/notifications', auth, role('instrutor'), instructorCtrl.notifications);
router.get('/instructors/me/analytics', auth, role('instrutor'), instructorCtrl.analytics);

router.get('/courses', courseCtrl.listPublic);
router.get('/courses/my', auth, role('aluno'), courseCtrl.myCourses);
router.get('/courses/:id', courseCtrl.getCourse);
router.post('/courses', auth, role('instrutor'), courseCtrl.createCourse);
router.post('/courses/:id/publish', auth, role('instrutor'), courseCtrl.publishCourse);
router.post('/courses/:id/progress', auth, role('aluno'), courseCtrl.saveProgress);
router.post('/courses/:id/videos', auth, role('instrutor'), videoUpload.single('video'), videoCtrl.createVideo);
router.get('/courses/:id/videos', auth, role('aluno', 'instrutor'), videoCtrl.listCourseVideos);
router.get('/videos/:id', auth, role('aluno', 'instrutor'), videoCtrl.getVideo);
router.put('/videos/:id', auth, role('instrutor'), videoCtrl.updateVideo);
router.delete('/videos/:id', auth, role('instrutor'), videoCtrl.deleteVideo);
router.post('/videos/:id/progress', auth, role('aluno'), videoCtrl.saveVideoProgress);

router.post('/schedules', auth, role('instrutor'), interactionCtrl.createSchedule);
router.get('/schedules', auth, role('aluno', 'instrutor'), interactionCtrl.listSchedules);

router.get('/courses/:courseId/questions', auth, interactionCtrl.listQuestions);
router.post('/courses/:courseId/questions', auth, role('aluno'), interactionCtrl.postQuestion);
router.post('/questions/:questionId/answers', auth, role('instrutor'), interactionCtrl.postAnswer);
router.post('/courses/:courseId/reviews', auth, role('aluno'), interactionCtrl.postReview);

module.exports = router;
