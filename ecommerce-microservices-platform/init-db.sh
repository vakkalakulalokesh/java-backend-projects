#!/usr/bin/env bash
set -euo pipefail
: "${PGHOST:=localhost}"
: "${PGPORT:=5432}"
: "${PGUSER:=ecommerce}"
export PGPASSWORD="${PGPASSWORD:-ecommerce}"
for db in product_db order_db payment_db inventory_db notification_db; do
  exists=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname='${db}'")
  if [[ "$exists" != "1" ]]; then
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres -c "CREATE DATABASE ${db}"
  fi
done
