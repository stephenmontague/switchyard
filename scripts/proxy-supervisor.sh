#!/usr/bin/env bash
# Restart-on-exit supervisor for the proxy. The cloud's "restart" lifecycle command makes
# the proxy exit with code 10 (ProxyControlPoller.RESTART_EXIT_CODE) — we relaunch it.
# Any other exit code (including 0 from a "shutdown" command) stays down.
# In production this role is played by systemd / a Windows service with Restart=on-failure.
set -u
cd "$(dirname "$0")/.."

JAR=$(ls proxy/target/proxy-app-*.jar 2>/dev/null | head -1)
if [ -z "${JAR}" ]; then
  echo ">> no proxy jar found — run 'mvn -q -pl proxy package -DskipTests' first" >&2
  exit 1
fi

while true; do
  echo ">> supervisor: launching ${JAR}"
  java -jar "${JAR}" --spring.profiles.active=local "$@"
  code=$?
  if [ "${code}" -eq 10 ]; then
    echo ">> supervisor: proxy requested RESTART (exit 10) — relaunching in 1s"
    sleep 1
    continue
  fi
  echo ">> supervisor: proxy exited with code ${code} — staying down"
  exit "${code}"
done
