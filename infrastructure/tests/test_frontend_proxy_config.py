import json
import os
import subprocess
import unittest
from pathlib import Path


REPOSITORY_DIRECTORY = Path(__file__).resolve().parents[2]
INFRASTRUCTURE_DIRECTORY = REPOSITORY_DIRECTORY / "infrastructure"
NGINX_TEMPLATE = REPOSITORY_DIRECTORY / "frontend" / "nginx.conf.template"
FRONTEND_DOCKERFILE = REPOSITORY_DIRECTORY / "frontend" / "Dockerfile"


class FrontendProxyConfigurationTests(unittest.TestCase):
    def test_scheme_is_deployment_configured_not_inferred_from_host_or_request_headers(self) -> None:
        configuration = NGINX_TEMPLATE.read_text()
        dockerfile = FRONTEND_DOCKERFILE.read_text()

        self.assertIn("proxy_set_header Host $http_host;", configuration)
        self.assertIn("proxy_set_header X-Forwarded-Host $http_host;", configuration)
        self.assertIn(
            "proxy_set_header X-Forwarded-Proto ${LYFTIX_PUBLIC_SCHEME};",
            configuration,
        )
        self.assertIn('proxy_set_header Forwarded "";', configuration)
        self.assertIn('proxy_set_header X-Forwarded-Port "";', configuration)
        self.assertNotRegex(configuration, r"\.tail[0-9a-f]+\.ts\.net")
        self.assertNotIn("map $http_host", configuration)
        self.assertNotIn("$http_x_forwarded_proto", configuration)
        self.assertIn("ENV LYFTIX_PUBLIC_SCHEME=http", dockerfile)
        self.assertIn(
            "COPY nginx.conf.template /etc/nginx/templates/default.conf.template",
            dockerfile,
        )

    def test_compose_uses_https_and_loopback_only_with_http_override(self) -> None:
        environment = os.environ.copy()
        environment.update(
            {
                "POSTGRES_DB": "lyftix",
                "POSTGRES_USER": "lyftix_user",
                "POSTGRES_PASSWORD": "test-password",
                "SYSTEM_METRICS_HOSTNAME": "host.example.test",
            }
        )
        environment.pop("LYFTIX_PUBLIC_SCHEME", None)
        environment.pop("FRONTEND_PORT", None)

        def frontend_configuration() -> dict:
            result = subprocess.run(
                ["docker", "compose", "--env-file", "/dev/null", "config", "--format", "json"],
                cwd=INFRASTRUCTURE_DIRECTORY,
                env=environment,
                check=True,
                capture_output=True,
                text=True,
            )
            return json.loads(result.stdout)["services"]["frontend"]

        frontend = frontend_configuration()
        self.assertEqual(frontend["environment"]["LYFTIX_PUBLIC_SCHEME"], "https")
        self.assertEqual(len(frontend["ports"]), 1)
        self.assertEqual(frontend["ports"][0]["host_ip"], "127.0.0.1")
        self.assertEqual(frontend["ports"][0]["published"], "8088")
        self.assertEqual(frontend["ports"][0]["target"], 8080)

        environment["LYFTIX_PUBLIC_SCHEME"] = "http"
        environment["FRONTEND_PORT"] = "8090"
        frontend = frontend_configuration()
        self.assertEqual(frontend["environment"]["LYFTIX_PUBLIC_SCHEME"], "http")
        self.assertEqual(frontend["ports"][0]["host_ip"], "127.0.0.1")
        self.assertEqual(frontend["ports"][0]["published"], "8090")


if __name__ == "__main__":
    unittest.main()
