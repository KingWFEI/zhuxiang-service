#!/usr/bin/env bash
set -Eeuo pipefail

release_dir=${1:?usage: deploy-test.sh <release-dir>}
case "$release_dir" in
    /home/*/easenest-release-*) ;;
    *) echo "unexpected release dir: $release_dir" >&2; exit 1 ;;
esac
trap 'rm -rf -- "$release_dir"' EXIT

app_dir=/opt/apps/easenest
image=easenest-backend:local
previous_image=easenest-backend:previous
backend_container=easenest-backend-1
private_dir="$app_dir/data/uploads-private"
verify_url=https://kingwangf.fun/api/health

install -m 644 "$release_dir/backend/target/zhuxiang-service-0.0.1-SNAPSHOT.jar" "$app_dir/backend/target/"
install -m 644 "$release_dir/backend/Dockerfile" "$release_dir/backend/.dockerignore" "$app_dir/backend/"
cp -f "$release_dir"/deploy/* "$app_dir/deploy/"

install -d -o 10001 -g 10001 -m 750 "$private_dir"
if [ -z "$(ls -A "$private_dir")" ] \
    && docker container inspect "$backend_container" >/dev/null 2>&1 \
    && docker exec "$backend_container" test -d /app/uploads-private; then
    docker cp "$backend_container:/app/uploads-private/." "$private_dir/"
fi

if docker image inspect "$image" >/dev/null 2>&1; then
    docker image tag "$image" "$previous_image"
fi

cd "$app_dir/deploy"
compose() {
    docker compose --env-file "$app_dir/secrets/compose.env" \
        -f docker-compose.yml -f docker-compose.test.yml "$@"
}

# 构建期间旧服务保持运行；构建失败时直接退出，线上不受影响。
compose build backend

verify_backend() {
    for _ in $(seq 1 30); do
        if curl --fail --silent --show-error --max-time 10 \
            --resolve kingwangf.fun:443:127.0.0.1 "$verify_url" >/dev/null; then
            return 0
        fi
        sleep 5
    done
    return 1
}

rollback() {
    echo "Deployment failed; restoring the previous backend image." >&2
    if docker image inspect "$previous_image" >/dev/null 2>&1; then
        docker image tag "$previous_image" "$image"
        compose up -d --no-build --wait --wait-timeout 300 \
            || echo "Rollback failed; inspect the server manually." >&2
    else
        echo "No previous backend image to roll back to." >&2
    fi
}

if compose up -d --no-build --wait --wait-timeout 300 && verify_backend; then
    docker image prune -f >/dev/null
    echo "Deployment verified: $verify_url"
else
    rollback
    exit 1
fi
