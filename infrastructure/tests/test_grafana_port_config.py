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
                "SYSTEM_METRICS_HOSTNAME": "host.example.test",
                "GRAFANA_ADMIN_PASSWORD": "obsolete-test-value",
                "GRAFANA_INITIAL_ADMIN_PASSWORD_FILE": "/dev/null",
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
        configuration = json.loads(result.stdout)
        services = configuration["services"]
        grafana = services["grafana"]

        self.assertEqual(
            services["system-metrics-worker"]["environment"]["SYSTEM_METRICS_HOSTNAME"],
            "host.example.test",
        )

        self.assertEqual(
            grafana["ports"],
            [{"mode": "ingress", "host_ip": "127.0.0.1", "target": 3000,
              "published": "3000", "protocol": "tcp"}],
        )
        self.assertEqual(services["frontend"]["ports"][0]["host_ip"], "127.0.0.1")
        self.assertEqual(services["frontend"]["ports"][0]["published"], "8088")
        for name, service in services.items():
            if name not in {"frontend", "grafana"}:
                self.assertFalse(service.get("ports"), name)

        self.assertNotIn("GF_SECURITY_ADMIN_PASSWORD", grafana["environment"])
        self.assertEqual(
            grafana["environment"]["GF_SECURITY_ADMIN_PASSWORD__FILE"],
            "/run/secrets/grafana_initial_admin_password",
        )
        self.assertEqual(grafana["secrets"][0]["source"], "grafana_initial_admin_password")
        self.assertIn(
            {"type": "volume", "source": "grafana_data", "target": "/var/lib/grafana", "volume": {}},
            grafana["volumes"],
        )
        self.assertEqual(
            configuration["secrets"]["grafana_initial_admin_password"]["file"],
            "/dev/null",
        )


if __name__ == "__main__":
    unittest.main()
