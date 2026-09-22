import json
import os
import subprocess
import unittest
from pathlib import Path


INFRASTRUCTURE_DIRECTORY = Path(__file__).resolve().parents[1]


class CadvisorComposeTests(unittest.TestCase):
    def test_containerd_snapshotter_configuration_is_present_and_least_privilege(
        self,
    ) -> None:
        environment = os.environ.copy()
        environment.update(
            {
                "POSTGRES_DB": "lyftix",
                "POSTGRES_USER": "lyftix_user",
                "POSTGRES_PASSWORD": "test-password",
                "SYSTEM_METRICS_HOSTNAME": "host.example.test",
            }
        )

        result = subprocess.run(
            ["docker", "compose", "config", "--format", "json"],
            cwd=INFRASTRUCTURE_DIRECTORY,
            env=environment,
            check=True,
            capture_output=True,
            text=True,
        )
        cadvisor = json.loads(result.stdout)["services"]["cadvisor"]

        self.assertEqual(cadvisor["image"], "ghcr.io/google/cadvisor:v0.60.5")
        self.assertIn(
            "--containerd=/run/containerd/containerd.sock",
            cadvisor["command"],
        )

        containerd_mount = next(
            mount
            for mount in cadvisor["volumes"]
            if mount["target"] == "/run/containerd/containerd.sock"
        )
        self.assertEqual(
            containerd_mount["source"],
            "/run/containerd/containerd.sock",
        )
        self.assertTrue(containerd_mount["read_only"])
        self.assertFalse(cadvisor.get("privileged", False))
        self.assertNotEqual(cadvisor.get("pid"), "host")


if __name__ == "__main__":
    unittest.main()
