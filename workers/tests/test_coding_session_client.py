import json

import httpx
import pytest

from lyftix_workers.coding_sessions.client import CodingSessionApiError, CodingSessionClient
from lyftix_workers.coding_sessions.parser import CodingSessionRecord


def session() -> CodingSessionRecord:
    return CodingSessionRecord(
        project_name="lyftix",
        language="Python",
        started_at="2026-09-08T08:00:00Z",
        ended_at="2026-09-08T09:00:00Z",
        source="file",
        notes="Worker milestone",
    )


def test_posts_exact_backend_payload_and_accepts_created() -> None:
    requests: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        return httpx.Response(201, request=request)

    with httpx.Client(transport=httpx.MockTransport(handler)) as http_client:
        CodingSessionClient(http_client, "http://lyftix.test/").create(session())

    assert str(requests[0].url) == "http://lyftix.test/api/coding-sessions"
    assert json.loads(requests[0].content) == {
        "projectName": "lyftix",
        "language": "Python",
        "startedAt": "2026-09-08T08:00:00Z",
        "endedAt": "2026-09-08T09:00:00Z",
        "source": "file",
        "notes": "Worker milestone",
    }


@pytest.mark.parametrize("status_code", [400, 422, 500, 503])
def test_client_and_server_responses_are_failures(status_code: int) -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(status_code, request=request))
    with (
        httpx.Client(transport=transport) as http_client,
        pytest.raises(CodingSessionApiError, match=f"Lyftix returned HTTP {status_code}"),
    ):
        CodingSessionClient(http_client, "http://lyftix.test").create(session())
