#!/usr/bin/env bash
set -Eeuo pipefail

backup_dir=/opt/backups/easenest-mysql
secret_file=/opt/apps/easenest/secrets/compose.env
container=easenest-mysql-1
database=easenest
retention_days=30

umask 077
install -d -m 700 "$backup_dir"

set -a
# shellcheck disable=SC1090
source "$secret_file"
set +a

stamp="$(date +%Y%m%d-%H%M%S)"
temporary="$backup_dir/.easenest-$stamp.sql.gz.part"
destination="$backup_dir/easenest-$stamp.sql.gz"

cleanup() {
    rm -f -- "$temporary"
}
trap cleanup EXIT

docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$container" \
    mysqldump --user=root --single-transaction --quick --routines --triggers \
    --events --set-gtid-purged=OFF "$database" \
    | gzip -9 > "$temporary"

gzip -t "$temporary"
mv "$temporary" "$destination"
trap - EXIT

find "$backup_dir" -maxdepth 1 -type f -name 'easenest-*.sql.gz' \
    -mtime "+$retention_days" -delete

printf '%s\n' "$destination"
