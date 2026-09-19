import json
import os
import subprocess
import unittest
from pathlib import Path


INFRASTRUCTURE_DIRECTORY = Path(__file__).resolve().parents[1]


class GrafanaPortConfigurationTests(unittest.TestCase):
    def test_only_frontend_and_grafana_publish_loopback_ports(self) -> None:
        environment = os.environ.copy()
        environment.update(
            {
                "POSTGRES_DB": "lyftix",
                "POSTGRES_USER": "lyftix_user",
                "POSTGRES_PASSWORD": "test-password",
            }
        )
        environment.pop("FRONTEND_PORT", None)
        environment.pop("GRAFANA_PORT", None)

        result = subprocess.run(
            [
                "docker", "compose", "--env-file", "/dev/null", "--profile", "workers",
                "config", "--format", "json",
            ],
            cwd=INFRASTRUCTURE_DIRECTORY,
            env=environment,
            check=True,
            capture_output=True,
            text=True,
        )
        services = json.loads(result.stdout)["services"]

        self.assertEqual(
            services["grafana"]["ports"],
            [{"mode": "ingress", "host_ip": "127.0.0.1", "target": 3000,
              "published": "3000", "protocol": "tcp"}],
        )
        self.assertEqual(services["frontend"]["ports"][0]["host_ip"], "127.0.0.1")
        self.assertEqual(services["frontend"]["ports"][0]["published"], "8088")
        for name, service in services.items():
            if name not in {"frontend", "grafana"}:
                self.assertFalse(service.get("ports"), name)


if __name__ == "__main__":
    unittest.main()
