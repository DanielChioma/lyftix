import json

import httpx
import pytest

from lyftix_workers.system_metrics.client import SystemMetricApiError, SystemMetricClient
from lyftix_workers.system_metrics.collector import SystemMetricSnapshot


def snapshot() -> SystemMetricSnapshot:
    return SystemMetricSnapshot(
        hostname="lyftix-host",
        source="local",
        cpu_percent=37.5,
        memory_used_bytes=400,
        memory_total_bytes=1000,
        disk_used_bytes=500,
        disk_total_bytes=2000,
        load_average_1m=1.25,
        collected_at="2026-09-08T12:00:00Z",
    )


def test_posts_exact_payload_and_accepts_created() -> None:
    requests: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        return httpx.Response(201, request=request)

    with httpx.Client(transport=httpx.MockTransport(handler)) as http_client:
        SystemMetricClient(http_client, "http://lyftix.test/").create(snapshot())

    assert str(requests[0].url) == "http://lyftix.test/api/system-metrics"
    assert json.loads(requests[0].content) == {
        "hostname": "lyftix-host",
        "source": "local",
        "cpuPercent": 37.5,
        "memoryUsedBytes": 400,
        "memoryTotalBytes": 1000,
        "diskUsedBytes": 500,
        "diskTotalBytes": 2000,
        "loadAverage1m": 1.25,
        "collectedAt": "2026-09-08T12:00:00Z",
    }


@pytest.mark.parametrize("status_code", [400, 500])
def test_http_error_responses_fail(status_code: int) -> None:
    transport = httpx.MockTransport(lambda request: httpx.Response(status_code, request=request))
    with (
        httpx.Client(transport=transport) as http_client,
        pytest.raises(SystemMetricApiError, match=f"Lyftix returned HTTP {status_code}"),
    ):
        SystemMetricClient(http_client, "http://lyftix.test").create(snapshot())


def test_transport_failure_is_wrapped() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("connection refused", request=request)

    with (
        httpx.Client(transport=httpx.MockTransport(handler)) as http_client,
        pytest.raises(SystemMetricApiError, match="Lyftix request failed"),
    ):
        SystemMetricClient(http_client, "http://lyftix.test").create(snapshot())
