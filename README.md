# Gym App

这是一个个人健身记录 Android 应用，用于记录今日训练、管理动作库、查看日历历史，并通过 Cloudflare 同步账号与数据。

## 项目规范

项目协作规范统一放在 [项目规范](./项目规范.md)。

## 发布流程

### 一键发布

```powershell
./release.ps1 <版本号> "<更新说明1>" "<更新说明2>" ...
```

示例：

```powershell
./release.ps1 1.3 "新增深色模式" "修复数据同步问题"
```

脚本会自动完成：

1. 更新 App 版本号（`versionCode` 和 `versionName`）
2. 更新 Worker 版本信息和更新日志
3. 提交代码
4. 推送到 GitHub，触发 CI 自动构建 APK 并部署到 Cloudflare Pages
5. 部署 Cloudflare Worker

### 版本号规则

- `versionName`：语义化版本号，例如 `1.3`
- `versionCode`：按 `主版本 * 100 + 次版本` 自动计算，例如 `1.3` 对应 `103`

## CI/CD

GitHub Actions 自动化流程如下：

- `push` 到 `main`：构建 debug APK，并部署到 Cloudflare Pages
- `push` 标签 `v*`：构建 release APK，并部署到 Cloudflare Pages

## 依赖服务

| 服务 | 用途 |
|------|------|
| Cloudflare Workers | 后端 API，负责认证、数据同步和更新检查 |
| Cloudflare D1 | 数据库存储 |
| Cloudflare Pages | APK 下载分发 |
| GitHub Actions | CI/CD 自动构建 |

## 本地开发

```bash
# Android
./gradlew assembleDebug

# Worker
cd cloudflare/worker
npm run dev

# 数据库迁移
npm run db:migrate:local
```
