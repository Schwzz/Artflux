const express = require('express');

const app = express();
const PORT = process.env.PORT || 8080;
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-3.5-flash';

// Impose reasonable request size limit (64 KB)
app.use(express.json({ limit: '64kb' }));

// Health check endpoint for container probes & monitoring
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'ok', service: 'artflux-ai-backend' });
});

// Reject non-POST requests to analysis endpoint
app.all('/api/analyze-source', (req, res, next) => {
  if (req.method !== 'POST') {
    return res.status(405).json({
      error: 'Method Not Allowed',
      message: 'Use POST to submit source analysis requests.'
    });
  }
  next();
});

// System prompt defining the Artflux source schema
const SYSTEM_PROMPT = `
You are an API integration engineer. The user is configuring an image/art media browser source.
Analyze the provided API documentation or example JSON response.
Extract or infer the fields required to query and parse the API.
Do NOT write executable code or scripts. Output valid JSON only with exactly these string keys:
{
  "sourceName": "A concise name for the source",
  "apiUrl": "Base HTTP GET endpoint URL to search or list media items",
  "searchParam": "Query parameter name for search keywords (e.g. q, query, tags, or empty)",
  "pageParam": "Query parameter name for pagination (e.g. page, p, offset, or empty)",
  "itemsPath": "JSON path to the array containing items (e.g. data, results, images, or empty if root is array)",
  "imageUrlField": "JSON path for the full-resolution image URL (e.g. url, file_url, path, download_url)",
  "thumbUrlField": "JSON path for the thumbnail URL (e.g. thumbnail, preview_url, thumb)",
  "postUrlField": "JSON path for post or webpage URL (e.g. link, url, id, short_url)",
  "tagsField": "JSON path for tags list or string (e.g. tags, labels)",
  "ratingField": "JSON path for content rating (e.g. rating, purity)",
  "mediaTypeField": "JSON path for media type (e.g. type, file_type)",
  "titleField": "JSON path for title (e.g. title, name, description)",
  "authorField": "JSON path for creator/artist (e.g. author, user.name, artist)",
  "description": "Brief summary of what this source provides"
}
`.trim();

app.post('/api/analyze-source', async (req, res) => {
  try {
    // 1. Validate request body
    const body = req.body;
    if (!body || typeof body !== 'object') {
      return res.status(400).json({
        error: 'Invalid Request',
        message: 'Request body must be a valid JSON object.'
      });
    }

    const input = typeof body.input === 'string' ? body.input.trim() : '';
    if (!input) {
      return res.status(400).json({
        error: 'Bad Request',
        message: 'Field "input" is required and cannot be empty.'
      });
    }

    if (input.length > 32768) {
      return res.status(413).json({
        error: 'Payload Too Large',
        message: 'Input exceeds maximum allowed length of 32,768 characters.'
      });
    }

    // 2. Retrieve server-side secret
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey || apiKey === 'MY_GEMINI_API_KEY') {
      return res.status(503).json({
        error: 'Backend Configuration Error',
        message: 'GEMINI_API_KEY is not configured in the server environment.'
      });
    }

    // 3. Query Google Gemini API
    const geminiEndpoint = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${apiKey}`;

    const geminiPayload = {
      contents: [
        {
          parts: [
            { text: `Input to analyze:\n\n${input}` }
          ]
        }
      ],
      systemInstruction: {
        parts: [
          { text: SYSTEM_PROMPT }
        ]
      },
      generationConfig: {
        responseMimeType: 'application/json',
        temperature: 0.1
      }
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 30000);

    let geminiResponse;
    try {
      geminiResponse = await fetch(geminiEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(geminiPayload),
        signal: controller.signal
      });
    } finally {
      clearTimeout(timeoutId);
    }

    if (!geminiResponse.ok) {
      const status = geminiResponse.status;
      return res.status(status >= 500 ? 502 : 400).json({
        error: 'Gemini Analysis Failed',
        message: `Upstream model service responded with status code ${status}.`
      });
    }

    const geminiData = await geminiResponse.json();
    const candidateText = geminiData?.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!candidateText) {
      return res.status(502).json({
        error: 'Invalid AI Response',
        message: 'No candidate text returned by the model.'
      });
    }

    let parsedConfig;
    try {
      parsedConfig = JSON.parse(candidateText.trim());
    } catch {
      return res.status(502).json({
        error: 'Parsing Error',
        message: 'Upstream response could not be parsed as structured JSON schema.'
      });
    }

    // Return the sanitized source config schema
    return res.status(200).json({
      sourceName: String(parsedConfig.sourceName || 'AI Configured Source').trim(),
      apiUrl: String(parsedConfig.apiUrl || '').trim(),
      searchParam: String(parsedConfig.searchParam || '').trim(),
      pageParam: String(parsedConfig.pageParam || '').trim(),
      itemsPath: String(parsedConfig.itemsPath || '').trim(),
      imageUrlField: String(parsedConfig.imageUrlField || '').trim(),
      thumbUrlField: String(parsedConfig.thumbUrlField || '').trim(),
      postUrlField: String(parsedConfig.postUrlField || '').trim(),
      tagsField: String(parsedConfig.tagsField || '').trim(),
      ratingField: String(parsedConfig.ratingField || '').trim(),
      mediaTypeField: String(parsedConfig.mediaTypeField || '').trim(),
      titleField: String(parsedConfig.titleField || '').trim(),
      authorField: String(parsedConfig.authorField || '').trim(),
      description: String(parsedConfig.description || 'Imported via Artflux Serverless AI Setup').trim()
    });

  } catch (err) {
    if (err.name === 'AbortError') {
      return res.status(504).json({
        error: 'Gateway Timeout',
        message: 'Gemini API call timed out after 30 seconds.'
      });
    }

    return res.status(500).json({
      error: 'Internal Server Error',
      message: 'An unexpected error occurred while processing the request.'
    });
  }
});

// Error handling middleware (e.g. payload too large from express.json)
app.use((err, req, res, next) => {
  if (err && err.type === 'entity.too.large') {
    return res.status(413).json({
      error: 'Payload Too Large',
      message: 'Request payload exceeded 64 KB limit.'
    });
  }
  return res.status(500).json({
    error: 'Internal Server Error',
    message: 'An unexpected error occurred.'
  });
});

if (require.main === module) {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Artflux AI Backend running on port ${PORT}`);
  });
}

module.exports = app;
