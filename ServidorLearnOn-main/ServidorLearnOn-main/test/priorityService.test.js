const test = require('node:test');
const assert = require('node:assert/strict');
const { calculatePriorityScore } = require('../src/services/priorityService');

test('fast-track usa fila separada e recebe boost de urgencia', () => {
  const score = calculatePriorityScore({
    createdAt: new Date('2026-05-01T00:00:00.000Z'),
    now: new Date('2026-05-03T00:00:00.000Z'),
    urgency: 'fast_track',
    similarRequestsCount: 4,
    availableInstructors: 2,
    totalCompatibleInstructors: 4,
  });

  assert.equal(score.queueType, 'fast_track');
  assert.equal(score.urgencyScore, 45);
  assert.equal(score.similarDemandScore, 16);
  assert.equal(score.waitingTimeScore, 3.2);
  assert.equal(score.availabilityScore, 3);
  assert.equal(score.totalScore, 67.2);
});

test('pedido normal envelhece na fila sem boost de urgencia', () => {
  const score = calculatePriorityScore({
    createdAt: new Date('2026-05-01T00:00:00.000Z'),
    now: new Date('2026-05-06T12:00:00.000Z'),
    urgency: 'normal',
    similarRequestsCount: 1,
    availableInstructors: 0,
    totalCompatibleInstructors: 3,
  });

  assert.equal(score.queueType, 'normal');
  assert.equal(score.urgencyScore, 0);
  assert.equal(score.similarDemandScore, 8);
  assert.equal(score.waitingTimeScore, 8.8);
  assert.equal(score.availabilityScore, 0);
  assert.equal(score.totalScore, 16.8);
});

test('disponibilidade considera a proporcao de instrutores compativeis livres', () => {
  const score = calculatePriorityScore({
    createdAt: new Date('2026-05-01T00:00:00.000Z'),
    now: new Date('2026-05-01T00:00:00.000Z'),
    urgency: 'normal',
    similarRequestsCount: 0,
    availableInstructors: 3,
    totalCompatibleInstructors: 6,
  });

  assert.equal(score.availabilityScore, 3);
  assert.equal(score.totalScore, 3);
});
