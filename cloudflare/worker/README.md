# Gym App Cloudflare Worker

这个 Worker 负责将 Android App 的训练状态存储到 Cloudflare D1，并提供同步所需的后端接口。

## 环境准备

1. 安装依赖：

   ```powershell
   npm install
   ```

2. 登录 Cloudflare 并创建 D1 数据库：

   ```powershell
   npx wrangler login
   npx wrangler d1 create gym_app_db
   ```

3. 将命令返回的 `database_id` 复制到 `wrangler.jsonc`。

4. 执行迁移并部署：

   ```powershell
   npm run db:migrate:remote
   npm run deploy
   ```

5. 将部署后的 Worker 地址填写到 Android App 的 `CloudflareConfig.API_BASE_URL` 中。

## 可选配置

如果需要个人构建，可以为 Worker 设置 `APP_SYNC_TOKEN` 机密，并在 Android 配置中使用相同值：

```powershell
npx wrangler secret put APP_SYNC_TOKEN
```
