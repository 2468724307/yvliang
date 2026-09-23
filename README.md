# 余量

面向生活费规划的全新原生 Android 工程，已实现任务书 Phase 0 至 Phase 10 的源码范围。具体进度与验证结果以 `docs` 下的最终交付报告为准。

## 打开工程

使用支持 AGP 8.11 的 Android Studio，安装 JDK 17、Android SDK 35 和 Build Tools 35.0.0。打开本目录，等待 Gradle 同步。`local.properties` 由本机 Android Studio 生成，不随工程交付。

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

macOS 或 Linux：

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

首次执行需要访问 Google Maven、Maven Central、Gradle 发行服务器。Wrapper 已固定版本与 SHA256。不要将本机构建环境的代理地址提交到工程。

## 构建与测试

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。

JUnit 和 Robolectric 报告：`app/build/reports/tests/testDebugUnitTest/index.html`。

测试使用本地 JVM 与 Robolectric Android 34 沙箱。它能验证 Activity、Compose 导航与真实 Room SQLite 行为，但不替代实际手机的触摸手势、设备性能和系统兼容性验收。

GitHub Actions：将工程完整上传到专属的新仓库，CI 会执行 Debug 构建、测试与 Lint，并上传 APK 和报告。当前工作区没有配置远端仓库，也没有触发远程 CI。

## 版本与发布

正式 applicationId 固定为 `com.yuliang.app`，Debug 为 `com.yuliang.app.debug`。当前 versionCode 为 10000，versionName 为 1.0.0。Release 签名从以下环境变量读取，不把私钥或密码提交到仓库：

- `YULIANG_RELEASE_STORE_FILE`
- `YULIANG_RELEASE_STORE_PASSWORD`
- `YULIANG_RELEASE_KEY_ALIAS`
- `YULIANG_RELEASE_KEY_PASSWORD`

配置后执行 `./gradlew :app:assembleRelease :app:bundleRelease`。测试分发密钥只能用于本次验收，正式发布前必须换成由产品所有者长期保管的正式密钥。

## 结构与约束

`app` 中的 UI、Domain、Data 按包隔离。预算使用 Long 分，Domain 是唯一计算来源。完整备份使用带版本的 JSON，账单通用导出使用 CSV；恢复先校验后在单个 Room 事务中替换数据。
