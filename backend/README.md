# Artflux AI Backend Proxy

A lightweight, secure serverless backend proxy for **Artflux**. It handles source documentation and JSON analysis using Google's Gemini API (`gemini-3.5-flash`), keeping the `GEMINI_API_KEY` stored exclusively server-side.

---

## Architecture

```text
Artflux Android App
        |
        | POST /api/analyze-source { "input": "..." }
        v
Artflux Backend Proxy (Cloud Run / Node.js)
        |
        | GEMINI_API_KEY (Server Secret)
        v
Google Gemini API (gemini-3.5-flash)
```

The Android client contains **no API keys or credentials**.

---

## Endpoints

* `GET /health` - Health check returning `{ "status": "ok" }`
* `POST /api/analyze-source` - Accepts `{ "input": "<API documentation or JSON sample>" }` and returns the structured `MediaSourceConfig` schema.

---

## Local Development

```bash
cd backend
npm install
export GEMINI_API_KEY="your-gemini-api-key"
npm start
```

Run tests:
```bash
npm test
```

---

## Deployment to Google Cloud Run

### Option 1: Using Google Cloud CLI
```bash
gcloud run deploy artflux-ai-backend \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GEMINI_API_KEY="your-gemini-api-key"
```

### Option 2: Using GitHub Actions
The repository includes automated CI/CD in `.github/workflows/deploy-backend.yml` that builds and deploys to Cloud Run using **GitHub Actions Secrets**:
* `GEMINI_API_KEY`: Your Google Gemini API Key.
* `GCP_PROJECT_ID`: Your Google Cloud Project ID.
* `GCP_SA_KEY`: Google Cloud Service Account JSON Key with Cloud Run Admin permissions.
