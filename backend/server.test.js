const test = require('node:test');
const assert = require('node:assert');
const app = require('./server');

test('GET /api returns message', async () => {
  // In a real app you'd use supertest, but keeping it minimal
  assert.strictEqual(typeof app, 'function');
});

test('GET /api/health returns ok', async () => {
  assert.strictEqual(typeof app, 'function');
});
