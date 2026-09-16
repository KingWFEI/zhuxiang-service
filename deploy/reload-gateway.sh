#!/usr/bin/env sh
set -eu

docker kill --signal=HUP easenest-gateway >/dev/null 2>&1 || true
