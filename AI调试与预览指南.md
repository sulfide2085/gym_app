# AI 调试与预览指南

本文档用于告诉 AI 或协作者：在不打开 Android Studio 的情况下，如何预览当前 Android App、查看启动日志，并基于日志协助调试。

## 项目结论

- 当前项目是原生 Android 项目，使用 Gradle 构建，界面技术为 Jetpack Compose。
- App 启动入口是 `com.example.gym_app/.MainActivity`。
- 命令行预览不依赖 Android Studio IDE，但依赖 Android SDK、JDK、设备或模拟器。
- `adb` 不是构建工具，主要负责连接设备、安装 APK、启动 Activity、查看日志。
- 当前推荐使用 `preview.py` 作为主入口，`preview.bat` 只是一个 Windows 薄包装。

## 启动链路

1. 执行 Gradle 构建生成 APK。
2. 使用 `adb install` 将 APK 安装到设备或模拟器。
3. 使用 `adb shell am start -n com.example.gym_app/.MainActivity` 启动 App。
4. 使用 `adb logcat` 或按 PID 过滤日志，观察启动过程和报错。

## 推荐入口脚本

仓库根目录提供了：

- `preview.py`：主逻辑，推荐直接使用
- `preview.bat`：Windows 快捷入口，内部转发到 `preview.py`

常用示例：

```powershell
python .\preview.py doctor
python .\preview.py emulators
python .\preview.py --avd-name Pixel_8 run
python .\preview.py startup-log
python .\preview.py logcat
```

如果你更习惯双击或 `cmd`：

```bat
preview.bat doctor
preview.bat emulators
preview.bat run
```

## 命令说明

### 1. 环境检查

```powershell
python .\preview.py doctor
```

作用：

- 检查 Android SDK 路径是否可解析。
- 检查 `adb` 是否可用。
- 检查模拟器工具是否可用。
- 检查 Gradle Wrapper 是否存在。
- 列出已连接设备。

### 2. 仅构建 APK

```powershell
python .\preview.py build
python .\preview.py --variant release build
```

默认构建 `debug` 包，对应输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 3. 查看可用模拟器

```powershell
python .\preview.py emulators
```

### 4. 启动模拟器

```powershell
python .\preview.py --avd-name Pixel_8 start-emulator
```

如果不传 `--avd-name`，脚本会优先选择 `Pixel_8`，否则选择第一个可用 AVD。

### 5. 安装 APK 到设备

```powershell
python .\preview.py install
```

默认执行覆盖安装，相当于：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 6. 启动 App

```powershell
python .\preview.py start
```

默认启动：

```text
com.example.gym_app/.MainActivity
```

### 7. 一键构建 + 安装 + 启动

```powershell
python .\preview.py run
```

这是最适合“预览当前 App 实际效果”的命令。

说明：

- 如果当前没有连接设备，脚本会自动启动一个可用模拟器。
- 脚本会等待模拟器真正开机完成，再执行安装，避免 `adb` 过早安装失败。

### 8. 查看实时日志

```powershell
python .\preview.py logcat
```

作用：

- 进入实时日志流。
- 默认优先按当前 App 的 PID 过滤。
- 如果暂时取不到 PID，会回退到普通 `adb logcat`。

### 9. 查看启动日志并辅助调试

```powershell
python .\preview.py startup-log
```

作用：

1. 先清空旧日志。
2. 启动 App。
3. 自动获取当前 App PID。
4. 进入按 PID 过滤后的日志流，便于观察冷启动阶段报错。

这个命令最适合 AI 排查：

- 启动闪退
- 登录页打不开
- 本地数据库初始化异常
- 网络请求失败
- 权限问题

## 多设备场景

当同时连接多个设备或模拟器时，传入 `--device-id`：

```powershell
python .\preview.py --device-id emulator-5554 run
python .\preview.py --device-id R3CT30XXXX startup-log
```

可先用以下命令查看设备 ID：

```powershell
python .\preview.py devices
```

## AI 调试工作约定

当 AI 需要帮助调试 Android App 时，建议遵循以下顺序：

1. 先执行 `python .\preview.py doctor`，确认环境和设备状态。
2. 如果没有设备，执行 `python .\preview.py emulators`。
3. 再执行 `python .\preview.py --avd-name Pixel_8 run`，确认 App 是否能正常安装和启动。
4. 如果是启动异常，执行 `python .\preview.py startup-log`，优先观察首个异常栈。
5. 如果 App 已运行但功能异常，执行 `python .\preview.py logcat`，在操作复现时抓取日志。
6. 根据日志中的异常类型定位代码，再修改并重复上述步骤验证。

## AI 读取日志时重点关注

- `FATAL EXCEPTION`
- `AndroidRuntime`
- `Caused by`
- `SecurityException`
- `IllegalStateException`
- `NullPointerException`
- `SQLiteException`
- `NetworkOnMainThreadException`
- Compose 渲染和状态相关异常

## 与 Android Studio 的关系

- Android Studio 可以运行、调试、查看 Compose Preview。
- `preview.py` 走的是命令行链路，更适合自动化、AI 协作和快速复现。
- 命令行方式不是 Android Studio Preview 的完全替代，但能完成真实设备上的安装、启动和日志分析。

## 手动等价命令

以下是脚本背后的核心等价命令，便于 AI 在脚本失效时回退：

```powershell
.\gradlew.bat assembleDebug
adb devices
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.gym_app/.MainActivity
adb logcat
adb shell pidof com.example.gym_app
adb logcat --pid <PID>
```

## 备注

- 当前仓库已存在 `local.properties`，其中包含本机 Android SDK 路径。
- 若 AI 在其他机器执行，优先读取 `ANDROID_SDK_ROOT`、`ANDROID_HOME`，再回退到 `local.properties`。
- 若没有连接设备，脚本会尝试自动启动模拟器。
