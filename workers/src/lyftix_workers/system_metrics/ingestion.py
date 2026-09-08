from __future__ import annotations

import logging

from lyftix_workers.system_metrics.client import SystemMetricClient
from lyftix_workers.system_metrics.collector import SystemMetricCollector

LOGGER = logging.getLogger(__name__)


class SystemMetricIngestionJob:
    def __init__(self, collector: SystemMetricCollector, client: SystemMetricClient) -> None:
        self._collector = collector
        self._client = client

    def run(self) -> None:
        LOGGER.info("Starting system metric collection")
        snapshot = self._collector.collect()
        LOGGER.info(
            "Collected system metric for hostname=%s source=%s",
            snapshot.hostname,
            snapshot.source,
        )
        self._client.create(snapshot)
        LOGGER.info("Submitted system metric successfully")
