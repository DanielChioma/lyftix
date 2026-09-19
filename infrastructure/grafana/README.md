# Grafana initial admin password

Before the first Grafana start, create a strong password in a private host file at
`infrastructure/secrets/grafana-initial-admin-password`. The directory is ignored by Git.
Alternatively, set `GRAFANA_INITIAL_ADMIN_PASSWORD_FILE` to an existing private file path.
The file must be readable by the Grafana container's non-root process (UID 472).
For the default path, keep `infrastructure/secrets/` accessible only to the deploying
host user (mode `0700`) and make the file readable in the container (for example,
mode `0644` inside that private directory). Compose file-backed secrets retain host
file ownership and permissions; Compose cannot remap them for the container user.

Compose mounts this file only into Grafana as `/run/secrets/grafana_initial_admin_password`.
The password is used to create the initial admin account on a fresh `grafana_data` volume.
It does not reset an existing admin password, including one changed through the Grafana UI.
Keep the file available for container recreation and a possible fresh installation.

The old `GRAFANA_ADMIN_PASSWORD` environment variable is no longer used. Remove it from
private deployment configuration after the new secret file is in place. Do not delete
`grafana_data` during this transition.
