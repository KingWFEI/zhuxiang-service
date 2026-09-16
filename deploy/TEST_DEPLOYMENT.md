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

服务器上的数据库、Redis、上传文件和密钥目录不会提交到 GitHub。自动部署只更新后端 Jar、
Dockerfile 和部署编排文件，然后执行健康检查。

## App 测试账号

测试账号由服务器环境变量启用。它是普通 `TENANT` 用户，不使用管理端密码登录。
App 仍按原流程先请求登录验证码，再使用测试环境配置的固定验证码登录。固定验证码仅对配置的
测试手机号及 `login` 场景生效，其他手机号不会获得固定验证码。

生产环境必须设置：

```properties
APP_TEST_USER_ENABLED=false
```
