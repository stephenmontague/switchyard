# Cloud <-> Edge Proxy monorepo — build & demo recipes
# No Docker. Local dev uses the Temporal CLI dev server + Maven (spring-boot:run).
# Requires: just, Java 17+, Maven, Temporal CLI (v1.7.0+ for Standalone Activities).
#
# Modules: proxy/ (the connector), dummy-cloud/ + dummy-edge/ (demo harness).
# All recipes run from the repo root; the aggregator pom builds everything.

set shell := ["bash", "-cu"]

# Ports (keep in sync with PLAN.md > Appendix)
proxy_admin_port := "8080"
cloud_port       := "8081"
edge_port        := "8082"
temporal_ui_port := "8233"

# Show available recipes
default:
    @just --list

# ---------------------------------------------------------------------------
# Build & test (reactor: all three modules)
# ---------------------------------------------------------------------------

# Build everything (proxy + dummies)
build:
    mvn -q clean package

# Run unit tests (routing core, codecs, validators, control workflow)
test:
    mvn -q test

# Compile without packaging
compile:
    mvn -q compile

# Remove build output
clean:
    mvn -q clean

# ---------------------------------------------------------------------------
# Local Temporal (no Docker)
# ---------------------------------------------------------------------------

# Start a local Temporal dev server with the Web UI.
# Standalone Activities (Public Preview) ship in Server 1.31.0 (CLI 1.7.0) but are gated
# behind the `activity.enableStandalone` dynamic config flag — enabled here explicitly.
temporal-dev:
    temporal server start-dev --ui-port {{temporal_ui_port}} \
        --dynamic-config-value activity.enableStandalone=true

# Quick health check against the local dev server
temporal-status:
    temporal operator namespace list

# ---------------------------------------------------------------------------
# Run the components (each in its own terminal, from the repo root)
# ---------------------------------------------------------------------------

# Run the proxy against the local dev server (Spring profile: local)
run-proxy:
    mvn -q -pl proxy spring-boot:run -Dspring-boot.run.profiles=local

# Run the dummy cloud app
run-dummy-cloud:
    mvn -q -pl dummy-cloud spring-boot:run -Dspring-boot.run.profiles=local

# Run the dummy edge target
run-dummy-edge:
    mvn -q -pl dummy-edge spring-boot:run -Dspring-boot.run.profiles=local

# ---------------------------------------------------------------------------
# Demo (assumes: temporal-dev, run-proxy, run-dummy-cloud, run-dummy-edge are up)
# ---------------------------------------------------------------------------

# End-to-end HTTP round trip: WAVE_RELEASE (cloud->edge) then PICK_CONFIRM (edge->cloud)
demo-http:
    @echo ">> Triggering WAVE_RELEASE via dummy-cloud ..."
    curl -fsS -X POST localhost:{{cloud_port}}/demo/wave-release \
        -H 'content-type: application/json' \
        -d '{"orderId":"ORD-1001","items":[{"sku":"ABC-123","qty":2}]}' | jq .
    @echo ">> Inspect both standalone activities in the Temporal UI: http://localhost:{{temporal_ui_port}}"
    @echo ">> Check dummy-cloud received the PICK_CONFIRM:"
    curl -fsS localhost:{{cloud_port}}/demo/confirms | jq .

# TCP round trip: CONTAINER_PUTAWAY (cloud->edge, device port 9001) then
# PUTAWAY_CONFIRM (edge->cloud, proxy port 6001)
demo-putaway-tcp:
    @echo ">> Triggering CONTAINER_PUTAWAY via dummy-cloud ..."
    curl -fsS -X POST localhost:{{cloud_port}}/demo/putaway \
        -H 'content-type: application/json' \
        -d '{"containerId":"CTN-2001","location":"A-01-03"}' | jq .
    @sleep 2
    @echo ">> Check dummy-cloud received the PUTAWAY_CONFIRM:"
    curl -fsS localhost:{{cloud_port}}/demo/confirms | jq .

# FTP round trip: CYCLE_COUNT_REQ (cloud->edge, device folder cycle-count) then
# CYCLE_COUNT_CONFIRM (edge->cloud, proxy folder cycle-count-confirm)
demo-cycle-count-ftp:
    @echo ">> Triggering CYCLE_COUNT_REQ via dummy-cloud ..."
    curl -fsS -X POST localhost:{{cloud_port}}/demo/cycle-count \
        -H 'content-type: application/json' \
        -d '{"countId":"CC-3001","location":"B-02-07"}' | jq .
    @sleep 3
    @echo ">> Check dummy-cloud received the CYCLE_COUNT_CONFIRM:"
    curl -fsS localhost:{{cloud_port}}/demo/confirms | jq .

# Remotely DISABLE this install via the control workflow (soft off)
demo-disable:
    curl -fsS -X POST localhost:{{cloud_port}}/control/disable | jq .

# Remotely ENABLE this install via the control workflow
demo-enable:
    curl -fsS -X POST localhost:{{cloud_port}}/control/enable | jq .

# Push a routing config update (hot reload, no restart)
demo-apply-config file="config/sample-routes.json":
    curl -fsS -X POST localhost:{{cloud_port}}/control/apply-config \
        -H 'content-type: application/json' \
        --data-binary @{{file}} | jq .

# Push an INVALID routing config (TCP port outside the pool) -> expect rejection
demo-apply-bad-config:
    curl -fsS -X POST localhost:{{cloud_port}}/control/apply-config \
        -H 'content-type: application/json' \
        --data-binary @config/invalid-routes.json | jq .

# Query the control workflow's desired state (via dummy-cloud -> Temporal)
demo-state:
    curl -fsS localhost:{{cloud_port}}/control/state | jq .

# Show the proxy's locally applied state (listeners, routes, enabled flag)
proxy-status:
    curl -fsS localhost:{{proxy_admin_port}}/admin/status | jq .

# Idempotency check: fire the same WAVE_RELEASE twice -> expect one execution
demo-idempotency:
    curl -fsS -X POST localhost:{{cloud_port}}/demo/wave-release \
        -H 'content-type: application/json' \
        -d '{"orderId":"ORD-DUP","items":[{"sku":"X","qty":1}]}' | jq -c .
    curl -fsS -X POST localhost:{{cloud_port}}/demo/wave-release \
        -H 'content-type: application/json' \
        -d '{"orderId":"ORD-DUP","items":[{"sku":"X","qty":1}]}' | jq -c .
    @echo ">> Second call should report duplicate:true — exactly ONE execution in the Temporal UI."
