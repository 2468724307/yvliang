# 余量 Phase 0 至 Phase 3 交付报告

## 交付结论

本工程依据《余量 ChatGPT Work 全新项目开发任务书》从零创建，完成范围严格截止到 Phase 3。Phase 0 工程初始化、Phase 1 数据层、Phase 2 Budget Engine、Phase 3 本月计划与固定支出均已实现并通过本地构建、单元测试、真实 Room 测试、Robolectric 导航测试和 Android Lint。Phase 4 至 Phase 10 未开始。

## 项目结构

工程采用单 `app` 模块并保持清晰包边界，避免当前规模下过度拆分：

- `data/database`：Room 实体、DAO、数据库与 Schema。
- `data/repository`：计划、账单、固定支出仓库和原子支付事务。
- `data/settings`：DataStore 设置存储。
- `domain/model`：纯 Kotlin 领域模型。
- `domain/budget`：BudgetCalculator、BalancePredictor、FixedExpenseManager、SavingGoalCalculator。
- `ui/plan`：本月计划、固定支出、PlanViewModel 和不可变 UiState。
- `ui/theme`：Material 3 主题和基础间距 Token。
- `docs`：实施计划与本报告。
- `.github/workflows/android.yml`：构建、测试、Lint 和 APK 报告上传流程。

## 技术基线

| 项目 | 版本或设置 |
| --- | --- |
| Kotlin | 2.1.20 |
| Android Gradle Plugin | 8.11.0 |
| Gradle | 8.13，Wrapper 固定 SHA256 |
| JDK | 17 |
| Compose | BOM 2025.05.01 |
| Room | 2.7.2，KSP 2.1.20-1.0.32 |
| DataStore | 1.1.7 |
| Navigation Compose | 2.9.0 |
| applicationId | `com.yuliang.app` |
| Debug applicationId | `com.yuliang.app.debug` |
| minSdk | 26 |
| compileSdk 和 targetSdk | 35 |
| versionCode | 1 |
| versionName | 0.1.0，Debug 后缀为 `-debug` |
| 数据库版本 | 1 |

本次固定 Android 35 作为已验证基线。Lint 提示 Android 36 已可用；升级需要单独完成 API 36 行为兼容与设备验证，未在本次范围内盲目更新。

## Phase 0 工程初始化

- 创建可直接由 Android Studio 打开的 Kotlin DSL 工程。
- 配置 Debug 与 Release；Release 开启压缩但没有使用 Debug 签名作为正式方案。
- 建立深浅色 Material 3 基础主题、启动页、关于页、计划入口和固定支出入口。
- 系统返回和页面按钮返回统一由 Navigation Compose Back Stack 处理。
- 建立 Gradle Wrapper、Git 忽略规则和 GitHub Actions 阶段门槛。
- 禁用系统自动备份，避免本地财务数据进入未设计的系统备份路径。

## Phase 1 数据层

Room Schema 1 包含：

- `monthly_plans`
- `categories`
- `transactions`
- `fixed_expense_templates`
- `fixed_expense_instances`

金额均使用 `Long` 分，不以 `Double` 持久化。交易时间、分类和固定支出关联已建立索引；分类采用归档方式保留历史语义。固定支出使用模板与月度实例分离，同一模板在不同月份拥有独立状态。重复生成依靠唯一索引与 `INSERT IGNORE` 保证幂等。

DAO 使用 `suspend` 或 `Flow`，正式数据库没有开启主线程查询。固定支出支付在 Room 事务内创建交易并更新月度实例；状态变化或第二次点击会回滚或拒绝，不留下半成功数据。DataStore 已建立 `reduce_motion` 设置存储，供后续 Motion 阶段使用。

## Phase 2 Budget Engine

统一计算入口为 `BudgetCalculator.calculate`。主要口径如下：

```text
本月可消费 = 基础生活费 + 加入本月可消费的额外收入
            - 存钱目标 - 安全余额 - 本月有效固定支出
```

- `UPCOMING` 与 `PAID` 固定支出都只冻结一次；与固定支出关联的实际交易不再作为变量支出重复扣减。
- `SKIPPED` 不冻结当月预算。
- 转账不计入收入、支出或预算消耗。
- 三种收入归属分别处理：加入可消费、加入储蓄、仅记录。
- 今日额度按当前剩余资金和剩余有效天数动态重分配；超支主值归零并保留 `todayOverspend`。
- 整分余数分配给较后的日期，日与周分配合计不会丢失分币。
- 自然周按周一至周日计算，并裁剪到本月与计划生效范围。
- 至少 3 个有效消费日后才给出月底预测；预测使用最近至多 7 个当月消费日。
- 金额加减乘使用精确整数运算；溢出明确失败，不产生静默错误结果。
- 预留项目超过收入、月余额为负或预测为负时进入 `RISK`。

## Phase 3 本月计划与固定支出

- 本月计划页面可设置生活费、存钱目标与安全余额，并显示统一 Domain 计算出的当前可消费金额。
- 首次建立计划记录月中生效日，后续修改金额不会错误重置生效日。
- 固定支出页面可添加月度重复项目、查看状态、标记支付或本月跳过。
- 新月份首次进入时自动、幂等生成重复固定支出的月度实例。
- 保存、支付与跳过完成后，Room Flow 驱动 ViewModel 重新组合数据并调用同一 Budget Engine。
- 输入金额、名称或日期无效时给出 Snackbar 反馈。

当前入口页只是 Phase 0 至 Phase 3 的功能入口，不是 Phase 4 首页成品。

## 验证结果

最终验证命令：

```bash
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

| 验证项 | 结果 |
| --- | --- |
| Debug Build | 通过 |
| Budget Engine 测试 | 13 项通过 |
| Room 与 Repository 测试 | 7 项通过 |
| 启动与导航测试 | 3 项通过 |
| 测试合计 | 23 项，0 失败，0 错误 |
| Android Lint | 通过，0 error；2 条 API 35 版本提示 |
| Debug APK | 成功生成 |
| AAB | 未生成，本次未进入发布阶段 |
| Release 签名 | 未配置，本次未进入发布阶段 |

测试由 JUnit、真实 Room 内存 SQLite 和 Robolectric Android 34 沙箱执行。环境没有 Android 模拟器或实体手机，因此触摸手势、厂商系统兼容、真实设备恢复和性能尚未验证。

## APK 信息

- 文件：`app/build/outputs/apk/debug/app-debug.apk`
- 大小：约 11.6 MiB，最终字节数以随交付产物为准。
- 用途：Phase 0 至 Phase 3 开发验证。
- 签名：Android Debug 签名，不可作为正式发布签名。

## Design 与 Motion 状态

已完成 Material 3 深浅色主题、基础颜色和间距。任务书中的品牌 Motion 属于 Phase 8 与 Phase 9，本次没有提前实现。因此以下项目仍待后续阶段：

- P1：全局按钮物理反馈、记一笔胶囊展开、可复用 Bottom Sheet。
- P2：共享元素、Rolling Number、图表 Magnetic Cursor、列表交错入场。
- P3：首页 Hero 3D 微倾、预算状态弥散光。

## 已知限制与下一阶段

- Phase 4 首页、Phase 5 快速记账与账单、Phase 6 统计预测界面、Phase 7 数据管理、Phase 8 至 9 视觉动效、Phase 10 发布验收均未实现。
- 启动图标是可替换占位资源，正式品牌图标尚未提供。
- Android 36 升级和兼容验证尚未执行，当前使用 Android 35 已验证基线。
- 没有实体设备或模拟器结果；进入 Phase 4 前应先在至少一台 Android 设备安装当前 APK，核对计划保存、固定支出支付、系统返回和重启后的数据恢复。
- 正式发布前必须配置私有 Release 签名、提高版本号并生成 AAB。

下一步应从 Phase 4 开始，用现有 `PlanViewModel` 数据流和 Budget Engine 输出构建首页，不能在 UI 内复制预算公式。
