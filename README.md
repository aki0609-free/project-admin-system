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
path are operational. Local Docker development setup and the remaining known
test debt are being reorganized before the V1 release is declared complete.

## License

Copyright (c) ProjectAdminSystem contributors. All rights reserved.

This repository is made publicly available for source-code viewing only. No
license is granted to use, copy, modify, merge, publish, distribute,
sublicense, or sell any part of this software unless the copyright holder has
provided prior written permission.

Third-party components included in this repository remain subject to their own
licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
