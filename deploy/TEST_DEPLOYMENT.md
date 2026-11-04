# 测试环境部署

## 分支约定

- `main`：稳定代码，不自动部署到当前测试服务器。
- `test`：集成测试分支；推送后由 GitHub Actions 自动测试并部署后端。
- 功能分支：从 `main` 创建，完成后先合并到 `test` 联调，通过后再合并到 `main`。

环境差异只通过 GitHub Environment Secrets 和服务器 `/opt/apps/easenest/secrets` 注入，
不要在 `test` 分支保存密码、令牌、服务器地址专属逻辑或测试环境专属业务代码。

## GitHub Environment: test

工作流需要以下 Secrets：

- `TEST_SERVER_HOST`
- `TEST_SERVER_USER`
- `TEST_SERVER_SSH_KEY`
- `TEST_SERVER_KNOWN_HOSTS`

## 服务器目录

```text
/opt/apps/easenest/
├── backend/          # Dockerfile、.dockerignore、后端 Jar
├── deploy/           # docker-compose.yml、docker-compose.test.yml、nginx.conf、deploy-test.sh 等
├── secrets/          # compose.env、backend.env（不进入 Git）
└── data/
    └── uploads-private/   # 私有文件（房源资料、隐私文件），挂载进容器，不随镜像重建丢失
```

数据库、Redis、公开上传文件（Docker 卷 `uploads`）同样保存在服务器上，不随部署覆盖。

## 密钥与必填配置

以下变量没有代码默认值，缺失时服务会启动失败。部署前必须写入 `/opt/apps/easenest/secrets/backend.env`
（参考 [backend.env.example](backend.env.example)）：

- `TOKEN_SECRET`：访问令牌签名密钥，更换后所有已登录用户需要重新登录。
- `ALIPAY_APP_ID`、`ALIPAY_MERCHANT_PRIVATE_KEY`、`ALIPAY_ALIPAY_PUBLIC_KEY`
- `AMAP_WEB_SERVICE_KEY`
- `TTLOCK_CLIENT_ID`、`TTLOCK_CLIENT_SECRET`、`TTLOCK_USERNAME`、`TTLOCK_PASSWORD`
- `ESIGN_APP_ID`、`ESIGN_APP_SECRET`

数据库密码、超级管理员密码等仍在 `compose.env` 中配置。

## 部署流程

1. GitHub Actions 运行 `./mvnw -B clean verify`，通过后打包发布目录并上传到服务器。
2. 服务器执行 `deploy/deploy-test.sh`：
   - 将旧镜像打上 `easenest-backend:previous` 标签，便于回滚。
   - 构建新镜像（构建期间旧服务继续运行，构建失败不影响线上）。
   - `docker compose up -d --wait` 启动新容器，并通过 `https://kingwangf.fun/api/health` 校验。
   - 任一步失败时自动恢复为旧镜像。

Dockerfile 只复制已经在 CI 中构建好的 Jar，构建时不运行 Maven，内存占用较小。

## 健康检查

`GET /api/health` 无需登录，返回 `code: 200` 即表示后端已正常处理请求。
部署脚本校验的是经过 Nginx 转发到后端的地址，因此后端停止时校验会失败。

## 支付宝网关

测试服务器的 `ALIPAY_GATEWAY_URL` 在 [docker-compose.yml](docker-compose.yml) 中设为支付宝正式网关，
测试环境实际调用的是正式网关，请只使用真实且可控的支付测试数据。

## 私有目录迁移

首次使用挂载目录时，部署脚本会从正在运行的旧容器中把 `/app/uploads-private` 复制到
`/opt/apps/easenest/data/uploads-private`。之后私有文件只保存在宿主机目录中。
