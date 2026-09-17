# Lyftix workers

## Production host metrics

The Compose `system-metrics-worker` uses `SYSTEM_METRICS_COLLECTION_MODE=host` to
collect metrics for the Linux Docker host. The hostname is supplied explicitly by
`SYSTEM_METRICS_HOSTNAME`; `SYSTEM_METRICS_SOURCE` remains only the persisted
collector label.

The worker receives read-only bind mounts for `/proc/stat`, `/proc/meminfo`,
`/proc/loadavg`, and `/proc/uptime`. It does not use privileged mode, the host PID
namespace, the Docker socket, or a host-root mount.

Before starting the workers profile on the HP server, create an empty directory on
the root filesystem for disk statistics:

```shell
sudo install -d -m 0755 /var/lib/lyftix-host-metrics
```

Set `SYSTEM_METRICS_HOST_DISK_PATH` if that directory must live elsewhere. The
selected directory must be on the filesystem whose total and used capacity Lyftix
should record. It is mounted read-only at `/host-disk`; only that directory, not the
host root filesystem, is visible to the worker.
