# Production Dockerfile for Artflux AI Backend (Google Cloud Run / Container Engine)
FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=8080

COPY package*.json ./
RUN npm ci --omit=dev

COPY server.js ./

# Run as non-privileged user for security
USER node

EXPOSE 8080

CMD ["node", "server.js"]
