import httpx
import pytest

from lyftix_workers.auth import LyftixAuthenticationError, authenticate


def test_authenticates_and_configures_session_and_csrf_headers() -> None:
    requests: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        if request.url.path == "/api/auth/login":
            assert request.headers["X-XSRF-TOKEN"] == "initial-token"
            assert request.headers["Cookie"] == "XSRF-TOKEN=initial-token"
            return httpx.Response(
                200,
                headers={"set-cookie": "JSESSIONID=session-value; Path=/; HttpOnly"},
                request=request,
            )
        if request.url.path == "/api/auth/me":
            assert "JSESSIONID=session-value" in request.headers["Cookie"]
            return httpx.Response(
                200,
                json={"username": "worker", "role": "OWNER"},
                request=request,
            )
        token = "initial-token" if len(requests) == 1 else "refreshed-token"
        return httpx.Response(
            200,
            json={"headerName": "X-XSRF-TOKEN", "parameterName": "_csrf", "token": token},
            headers={"set-cookie": f"XSRF-TOKEN={token}; Path=/"},
            request=request,
        )

    with httpx.Client(transport=httpx.MockTransport(handler)) as http_client:
        authenticate(http_client, "http://worker-api:8080", "worker", "password")

        assert "Cookie" not in http_client.headers
        assert http_client.headers["X-XSRF-TOKEN"] == "refreshed-token"

    assert [request.url.path for request in requests] == [
        "/api/auth/csrf",
        "/api/auth/login",
        "/api/auth/csrf",
        "/api/auth/me",
    ]


def test_secure_session_cookie_is_not_forced_over_internal_http() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/auth/login":
            return httpx.Response(
                200,
                headers={"set-cookie": "JSESSIONID=secure-value; Path=/; Secure; HttpOnly"},
                request=request,
            )
        if request.url.path == "/api/auth/me":
            assert "JSESSIONID" not in request.headers.get("Cookie", "")
            return httpx.Response(401, request=request)
        return httpx.Response(
            200,
            json={"headerName": "X-XSRF-TOKEN", "parameterName": "_csrf", "token": "token"},
            headers={"set-cookie": "XSRF-TOKEN=token; Path=/"},
            request=request,
        )

    with (
        httpx.Client(transport=httpx.MockTransport(handler)) as http_client,
        pytest.raises(LyftixAuthenticationError, match="Could not authenticate"),
    ):
        authenticate(http_client, "http://worker-api:8080", "worker", "password")


def test_authentication_failure_does_not_expose_credentials() -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(401, request=request))

    with (
        httpx.Client(transport=transport) as http_client,
        pytest.raises(LyftixAuthenticationError, match="Could not authenticate"),
    ):
        authenticate(http_client, "http://worker-api:8080", "worker", "do-not-report")
