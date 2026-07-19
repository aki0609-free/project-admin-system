#!/usr/bin/env python3

import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path


REGION = os.environ.get("AWS_REGION", "ap-northeast-1")
DATABASE_SECRET = os.environ.get(
    "DATABASE_SECRET_NAME", "project-admin/dev/database/application"
)
RUNTIME_SECRET = os.environ.get(
    "RUNTIME_SECRET_NAME", "project-admin/dev/application/runtime"
)
OUTPUT_DIR = Path(
    os.environ.get("CONFIGTREE_DIR", "/opt/project-admin/config/backend-secrets")
)
CONTAINER_UID = 10001
CONTAINER_GID = 10001


def get_secret(name: str, required: bool) -> dict[str, object]:
    command = [
        "aws",
        "secretsmanager",
        "get-secret-value",
        "--secret-id",
        name,
        "--query",
        "SecretString",
        "--output",
        "text",
        "--region",
        REGION,
    ]

    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        if required:
            print(f"Required secret is unavailable: {name}", file=sys.stderr)
            sys.exit(1)
        print(f"Optional runtime secret is not configured yet: {name}")
        return {}

    try:
        value = json.loads(result.stdout)
    except json.JSONDecodeError:
        print(f"Secret is not valid JSON: {name}", file=sys.stderr)
        sys.exit(1)

    if not isinstance(value, dict):
        print(f"Secret must be a JSON object: {name}", file=sys.stderr)
        sys.exit(1)
    return value


def require_keys(secret: dict[str, object], keys: list[str], name: str) -> None:
    missing = [key for key in keys if not str(secret.get(key, "")).strip()]
    if missing:
        print(f"Secret {name} is missing required keys: {', '.join(missing)}", file=sys.stderr)
        sys.exit(1)


def write_property(directory: Path, property_name: str, value: object) -> None:
    target = directory / property_name
    with tempfile.NamedTemporaryFile(
        mode="w", encoding="utf-8", dir=directory, delete=False
    ) as temporary:
        temporary.write(str(value))
        temporary_path = Path(temporary.name)

    os.chown(temporary_path, CONTAINER_UID, CONTAINER_GID)
    os.chmod(temporary_path, 0o400)
    os.replace(temporary_path, target)


database = get_secret(DATABASE_SECRET, required=True)
if "usename" in database and "username" not in database:
    print(
        f"Secret {DATABASE_SECRET} contains invalid key 'usename'; "
        "rename it to 'username'",
        file=sys.stderr,
    )
    sys.exit(1)
require_keys(database, ["username", "password", "host", "port", "dbname"], DATABASE_SECRET)

runtime = get_secret(RUNTIME_SECRET, required=False)

OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
os.chown(OUTPUT_DIR, 0, CONTAINER_GID)
os.chmod(OUTPUT_DIR, 0o750)

jdbc_url = (
    f"jdbc:mysql://{database['host']}:{database['port']}/{database['dbname']}"
    "?sslMode=REQUIRED&serverTimezone=Asia/Tokyo&characterEncoding=UTF-8"
)

properties: dict[str, object] = {
    "spring.datasource.url": jdbc_url,
    "spring.datasource.username": database["username"],
    "spring.datasource.password": database["password"],
}

runtime_mapping = {
    "jwtSecret": "jwt.secret",
    "openAiApiKey": "spring.ai.openai.api-key",
    "mongoUri": "spring.data.mongodb.uri",
    "cloudflareTunnelToken": "cloudflare.tunnel.token",
    "mailHost": "spring.mail.host",
    "mailPort": "spring.mail.port",
    "mailUsername": "spring.mail.username",
    "mailPassword": "spring.mail.password",
    "mailFromAddress": "project.mail.from-address",
    "keystorePassword": "keystore.password",
}

for secret_key, property_name in runtime_mapping.items():
    value = runtime.get(secret_key)
    if value is not None and str(value):
        properties[property_name] = value

for property_name, value in properties.items():
    write_property(OUTPUT_DIR, property_name, value)

print(f"CONFIGTREE_READY properties={len(properties)} directory={OUTPUT_DIR}")
