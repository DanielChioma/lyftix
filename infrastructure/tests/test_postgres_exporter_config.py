import json
import os
import subprocess
import unittest
from pathlib import Path


INFRASTRUCTURE_DIRECTORY = Path(__file__).resolve().parents[1]


class PostgresExporterComposeTests(unittest.TestCase):
    def test_reserved_password_is_not_embedded_in_exporter_uri(self) -> None:
        password = "p@ss:/?#[]!$&'()*+,;=%"
        environment = os.environ.copy()
        environment.update(
            {
                "POSTGRES_DB": "lyftix",
                "POSTGRES_USER": "lyftix_user",
                "POSTGRES_PASSWORD": password,
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
        exporter_environment = json.loads(result.stdout)["services"]["postgres-exporter"][
            "environment"
        ]

        self.assertEqual(
            exporter_environment["DATA_SOURCE_URI"],
            "postgres:5432/lyftix?sslmode=disable",
        )
        self.assertEqual(exporter_environment["DATA_SOURCE_USER"], "lyftix_user")
        self.assertEqual(exporter_environment["DATA_SOURCE_PASS"].replace("$$", "$"), password)
        self.assertNotIn("DATA_SOURCE_NAME", exporter_environment)


if __name__ == "__main__":
    unittest.main()
