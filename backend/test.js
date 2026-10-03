const http = require('http');
const assert = require('assert');
const app = require('./server');

const server = app.listen(0, '127.0.0.1', async () => {
  const port = server.address().port;
  const baseUrl = `http://127.0.0.1:${port}`;

  console.log(`Test server running at ${baseUrl}`);

  try {
    // Test 1: GET /health returns 200
    const healthRes = await fetch(`${baseUrl}/health`);
    assert.strictEqual(healthRes.status, 200, 'Health endpoint must return 200');
    const healthJson = await healthRes.json();
    assert.strictEqual(healthJson.status, 'ok', 'Health status must be ok');
    console.log('✓ Test 1: GET /health passed');

    // Test 2: GET /api/analyze-source returns 405 Method Not Allowed
    const methodRes = await fetch(`${baseUrl}/api/analyze-source`);
    assert.strictEqual(methodRes.status, 405, 'GET to analyze-source must return 405');
    console.log('✓ Test 2: Method validation (405) passed');

    // Test 3: POST /api/analyze-source with empty body returns 400
    const emptyRes = await fetch(`${baseUrl}/api/analyze-source`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({})
    });
    assert.strictEqual(emptyRes.status, 400, 'Empty input must return 400');
    console.log('✓ Test 3: Empty input validation (400) passed');

    // Test 4: POST /api/analyze-source with missing API key returns 503
    delete process.env.GEMINI_API_KEY;
    const noKeyRes = await fetch(`${baseUrl}/api/analyze-source`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ input: 'https://example.com/api' })
    });
    assert.strictEqual(noKeyRes.status, 503, 'Missing GEMINI_API_KEY must return 503');
    const noKeyJson = await noKeyRes.json();
    assert.ok(noKeyJson.error.includes('Configuration Error'), 'Error must indicate configuration error');
    console.log('✓ Test 4: Missing secret handling (503) passed');

    // Test 5: Verify no API keys in responses
    const responseString = JSON.stringify(noKeyJson);
    assert.strictEqual(responseString.includes('AIza'), false, 'Response must never leak API keys');
    console.log('✓ Test 5: Leak check passed');

    console.log('\nAll backend unit tests passed successfully!');
    server.close();
    process.exit(0);
  } catch (err) {
    console.error('Backend test failed:', err);
    server.close();
    process.exit(1);
  }
});
