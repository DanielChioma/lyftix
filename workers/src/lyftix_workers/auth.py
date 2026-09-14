from __future__ import annotations

import httpx


class LyftixAuthenticationError(RuntimeError):
    """Raised when a worker cannot establish an authenticated Lyftix session."""


def authenticate(
    http_client: httpx.Client,
    base_url: str,
    username: str,
    password: str,
) -> None:
    auth_url = f"{base_url.rstrip('/')}/api/auth"
    try:
        csrf_response = http_client.get(f"{auth_url}/csrf")
        csrf_response.raise_for_status()
        csrf = csrf_response.json()
        header_name = csrf["headerName"]
        token = csrf["token"]
        login_response = http_client.post(
            f"{auth_url}/login",
            json={"username": username, "password": password},
            headers={header_name: token},
        )
        login_response.raise_for_status()
        refreshed_csrf_response = http_client.get(f"{auth_url}/csrf")
        refreshed_csrf_response.raise_for_status()
        refreshed_csrf = refreshed_csrf_response.json()
        http_client.headers[refreshed_csrf["headerName"]] = refreshed_csrf["token"]
        authenticated_response = http_client.get(f"{auth_url}/me")
        authenticated_response.raise_for_status()
    except (httpx.HTTPError, KeyError, TypeError, ValueError) as exc:
        raise LyftixAuthenticationError("Could not authenticate with the Lyftix API") from exc
