#!/usr/bin/env python3
"""Generate a DEV-ONLY HS256 JWT accepted by the gateway (stdlib only, no dependencies).

There is no auth/users service on main yet, so this is the way to call protected endpoints locally.

Usage:
    python scripts/dev-token.py [user-uuid]
Reads JWT_SECRET from the environment (falls back to the docker-compose dev default).
"""
import base64
import hashlib
import hmac
import json
import os
import sys
import time
import uuid

DEV_DEFAULT = "dev-only-insecure-jwt-secret-change-me-0123456789"


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def main() -> None:
    secret = os.environ.get("JWT_SECRET", DEV_DEFAULT).encode()
    subject = sys.argv[1] if len(sys.argv) > 1 else str(uuid.uuid4())
    header = b64url(json.dumps({"alg": "HS256", "typ": "JWT"}).encode())
    payload = b64url(json.dumps({
        "sub": subject,
        "roles": ["USER"],
        "iat": int(time.time()),
        "exp": int(time.time()) + 3600,
    }).encode())
    signature = b64url(hmac.new(secret, f"{header}.{payload}".encode(), hashlib.sha256).digest())
    print(f"{header}.{payload}.{signature}")


if __name__ == "__main__":
    main()
