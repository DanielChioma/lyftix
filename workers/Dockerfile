FROM python:3.12-slim-bookworm AS build

WORKDIR /build

COPY requirements.lock ./
RUN python -m pip install --no-cache-dir setuptools==80.9.0 \
    && python -m pip wheel --no-cache-dir --wheel-dir /wheels -r requirements.lock

COPY pyproject.toml ./
COPY src ./src
RUN python -m pip wheel --no-cache-dir --no-deps --no-build-isolation \
    --wheel-dir /wheels .

FROM python:3.12-slim-bookworm AS runtime

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

RUN groupadd --system --gid 10001 lyftix \
    && useradd --system --uid 10001 --gid lyftix --home-dir /var/lib/lyftix-workers lyftix \
    && mkdir -p /var/lib/lyftix-workers \
    && chown lyftix:lyftix /var/lib/lyftix-workers

COPY --from=build /wheels /wheels
RUN python -m pip install --no-cache-dir --no-index --find-links=/wheels lyftix-workers \
    && rm -rf /wheels

USER 10001:10001
WORKDIR /var/lib/lyftix-workers

ENTRYPOINT ["lyftix-worker"]
CMD ["--help"]
