# ProjectAdminSystem

ProjectAdminSystem is an HR analytics and administration system built with
Spring Boot and Vue. Version 1 focuses on stabilizing existing behavior,
refactoring domain boundaries, and unifying the user interface.

## Technology stack

- Java 21 / Spring Boot 3
- Vue 3 / TypeScript / Vite / Vuetify
- MySQL, MongoDB, and Redis
- AWS EC2, RDS, S3, ECR, Secrets Manager, and Systems Manager
- Terraform
- Cloudflare Tunnel and Cloudflare Access
- GitHub Actions with AWS OIDC

## Repository layout

```text
backend/         Spring Boot application
frontend/        Vue application
infrastructure/  Terraform, Docker runtime, and operations scripts
openapi/         OpenAPI assets
test-data/       Demonstration seed data
```

## Security

This repository does not contain production credentials, Terraform state,
generated reports, customer data, signing keys, or environment-specific
configuration files.

Copy only the provided `.example` files and keep populated local files out of
version control. AWS deployment uses GitHub OIDC instead of long-lived AWS
access keys.

## Current status

The project is in V1 stabilization. The AWS DEV runtime and Cloudflare access
path are operational. A reproducible local Docker environment is available for
development while the remaining known test debt is resolved domain by domain.

## Local development

Prerequisites:

- Docker Desktop
- Java 21 for host-side backend development
- Node.js 22.12 or later for host-side frontend development

### Run the complete application

The standard local environment starts MySQL, MongoDB, Redis, Spring Boot, and
the production-style Nginx frontend. Local credentials in `docker-compose.yml`
are development-only values and must never be reused in a shared environment.

```bash
npm run docker:dev
npm run docker:ps
```

Open:

- Frontend: <http://localhost:5173/login>
- Backend health: <http://localhost:8080/actuator/health>

Stop the environment without deleting database volumes:

```bash
npm run docker:stop
```

### Run application code on the host

Start only the supporting services:

```bash
npm run docker:services
```

Run the backend from `backend/` with local connection values:

```bash
SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/ADMIN?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Tokyo&characterEncoding=UTF-8' \
SPRING_DATASOURCE_USERNAME='projectadmin_app' \
SPRING_DATASOURCE_PASSWORD='projectadmin-app-local' \
SPRING_DATA_MONGODB_URI='mongodb://localhost:27017/project_admin_local' \
SPRING_DATA_REDIS_HOST='localhost' \
APP_AI_ENABLED='false' \
SPRING_AI_MODEL_CHAT='none' \
SPRING_AI_MODEL_EMBEDDING='none' \
SPRING_AI_MODEL_IMAGE='none' \
SPRING_AI_MODEL_AUDIO_TRANSCRIPTION='none' \
SPRING_AI_MODEL_AUDIO_SPEECH='none' \
SPRING_AI_MODEL_MODERATION='none' \
./gradlew bootRun
```

Run the frontend from `frontend/`:

```bash
npm ci
npm run dev
```

## Validation

Pull requests run stable backend tests, a frontend production build, and
Terraform formatting and validation as required checks. Full backend tests,
frontend type checking, and frontend lint are also reported as observational
baselines until their existing failures are fixed one domain at a time.

## License

Copyright (c) ProjectAdminSystem contributors. All rights reserved.

This repository is made publicly available for source-code viewing only. No
license is granted to use, copy, modify, merge, publish, distribute,
sublicense, or sell any part of this software unless the copyright holder has
provided prior written permission.

Third-party components included in this repository remain subject to their own
licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
