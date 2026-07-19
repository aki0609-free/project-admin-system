#!/usr/bin/env python3

import argparse
import base64
import hashlib
import hmac
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Optional


DATE = "11111111"
SERVICE = "ses"
TERMINAL = "aws4_request"
MESSAGE = "SendRawEmail"
VERSION = 0x04


def run_aws(arguments: list[str]) -> str:
    command = ["aws", *arguments]
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        message = result.stderr.strip() or "AWS CLI command failed"
        raise RuntimeError(message)
    return result.stdout


def sign(key: bytes, message: str) -> bytes:
    return hmac.new(key, message.encode("utf-8"), hashlib.sha256).digest()


def smtp_password(secret_access_key: str, region: str) -> str:
    signature = sign(("AWS4" + secret_access_key).encode("utf-8"), DATE)
    signature = sign(signature, region)
    signature = sign(signature, SERVICE)
    signature = sign(signature, TERMINAL)
    signature = sign(signature, MESSAGE)
    return base64.b64encode(bytes([VERSION]) + signature).decode("ascii")


def parse_json(value: str, description: str) -> dict[str, object]:
    try:
        parsed = json.loads(value)
    except json.JSONDecodeError as error:
        raise RuntimeError(f"{description} is not valid JSON") from error
    if not isinstance(parsed, dict):
        raise RuntimeError(f"{description} must be a JSON object")
    return parsed


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Create dedicated SES SMTP credentials and store them in Secrets Manager."
    )
    parser.add_argument("--user-name", required=True)
    parser.add_argument("--secret-name", required=True)
    parser.add_argument("--from-address", required=True)
    parser.add_argument("--region", default="ap-northeast-1")
    args = parser.parse_args()

    existing_keys = parse_json(
        run_aws(
            [
                "iam",
                "list-access-keys",
                "--user-name",
                args.user_name,
                "--output",
                "json",
            ]
        ),
        "IAM access key response",
    ).get("AccessKeyMetadata", [])
    if existing_keys:
        raise RuntimeError(
            "SMTP IAM user already has an access key; rotate it instead of creating another one"
        )

    access_key_id = ""
    try:
        created = parse_json(
            run_aws(
                [
                    "iam",
                    "create-access-key",
                    "--user-name",
                    args.user_name,
                    "--output",
                    "json",
                ]
            ),
            "IAM access key response",
        )
        access_key = created.get("AccessKey")
        if not isinstance(access_key, dict):
            raise RuntimeError("IAM access key response is missing AccessKey")

        access_key_id = str(access_key.get("AccessKeyId", ""))
        secret_access_key = str(access_key.get("SecretAccessKey", ""))
        if not access_key_id or not secret_access_key:
            raise RuntimeError("IAM access key response is incomplete")

        current_secret = parse_json(
            run_aws(
                [
                    "secretsmanager",
                    "get-secret-value",
                    "--secret-id",
                    args.secret_name,
                    "--query",
                    "SecretString",
                    "--output",
                    "text",
                    "--region",
                    args.region,
                ]
            ),
            "runtime secret",
        )
        current_secret.update(
            {
                "mailHost": f"email-smtp.{args.region}.amazonaws.com",
                "mailPort": "587",
                "mailUsername": access_key_id,
                "mailPassword": smtp_password(secret_access_key, args.region),
                "mailFromAddress": args.from_address,
            }
        )

        temporary_path: Optional[Path] = None
        try:
            with tempfile.NamedTemporaryFile(
                mode="w", encoding="utf-8", delete=False
            ) as temporary:
                json.dump(current_secret, temporary, separators=(",", ":"))
                temporary_path = Path(temporary.name)
            os.chmod(temporary_path, 0o600)
            version_id = run_aws(
                [
                    "secretsmanager",
                    "put-secret-value",
                    "--secret-id",
                    args.secret_name,
                    "--secret-string",
                    f"file://{temporary_path}",
                    "--query",
                    "VersionId",
                    "--output",
                    "text",
                    "--region",
                    args.region,
                ]
            ).strip()
        finally:
            if temporary_path is not None:
                temporary_path.unlink(missing_ok=True)

        print("SES_SMTP_CREDENTIALS_STORED")
        print(f"IAM_USER={args.user_name}")
        print(f"SECRET_NAME={args.secret_name}")
        print(f"SECRET_VERSION_ID={version_id}")
        return 0
    except Exception:
        if access_key_id:
            subprocess.run(
                [
                    "aws",
                    "iam",
                    "delete-access-key",
                    "--user-name",
                    args.user_name,
                    "--access-key-id",
                    access_key_id,
                ],
                capture_output=True,
                text=True,
                check=False,
            )
        raise


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        sys.exit(1)
