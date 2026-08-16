FROM node:22-alpine AS builder

WORKDIR /workspace
ENV NODE_OPTIONS=--max-old-space-size=4096

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN --mount=type=secret,id=syncfusion_license \
    if [ -s /run/secrets/syncfusion_license ]; then \
      license_secret="$(cat /run/secrets/syncfusion_license)"; \
      license_key="$(printf '%s\n' "$license_secret" | sed -n 's/^VITE_SYNCFUSION_LICENSE_KEY=//p' | head -n 1)"; \
      if [ -z "$license_key" ]; then license_key="$license_secret"; fi; \
      export VITE_SYNCFUSION_LICENSE_KEY="$license_key"; \
    fi; \
    npm run build-only

FROM nginxinc/nginx-unprivileged:1.30-alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/healthz || exit 1
