#!/usr/bin/env bash
# Sends traffic that triggers both bugs. Stop with Ctrl-C.
set -euo pipefail
URL=${URL:-http://localhost:8080}

trap 'kill 0' EXIT

# Leak: about 2.5 MiB per second stays in the cache.
( while true; do curl -s -X POST "$URL/orders?sizeKb=256" > /dev/null; sleep 0.1; done ) &

# Pool saturation: 8 concurrent slow reports for a pool of 2 connections.
for _ in $(seq 8); do
  ( while true; do curl -s "$URL/reports/monthly" > /dev/null; done ) &
done

wait
