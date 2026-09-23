# 余量 Phase 4 至 Phase 10 实施与验收报告

## 当前结论

Phase 4 至 Phase 9 的源码已实现；Phase 10 的版本、Release 签名入口、回归测试代码和 CI 配置已完成。当前执行环境在恢复工程后无法访问 Gradle 发行服务器，也没有预装 Android SDK、Gradle 或依赖缓存，因此本轮新增代码尚未完成实际编译、测试、Lint、Release APK 和 AAB 生成。不能把此前 Phase 0 至 Phase 3 的 23 项通过结果冒充为本轮结果。

本报告将这一状态标记为：**实现完成，最终阶段门槛待构建环境恢复后验证**。

## 恢复说明

接续开发时临时工作区已经清空。已从保存的 `Yuliang_Phase0-3_Project.zip` 恢复最后一个可验证基线。此前尚未交付的 Phase 4 至 Phase 7 临时代码无法恢复，因此按任务书和已有业务决策重新实现。产品名、applicationId、Room Schema、Budget Engine 规则和 Phase 0 至 Phase 3 数据约束均保持不变。

## 技术基线

| 项目 | 当前值 |
| --- | --- |
| 项目名 / 应用名 | 余量 |
| applicationId | `com.yuliang.app` |
| Debug applicationId | `com.yuliang.app.debug` |
| Kotlin | 2.1.20 |
| Android Gradle Plugin | 8.11.0 |
| Gradle Wrapper | 8.13 |
| JDK | 17 |
| Compose BOM | 2025.05.01 |
| Room | 2.7.2 |
| minSdk | 26 |
| compileSdk / targetSdk | 35 / 35 |
| versionCode | 10000 |
| versionName | 1.0.0 |
| Room 数据库版本 | 1 |

## Phase 4：首页

- 建立首页、账单、统计、我的四栏主导航和全局“＋记一笔”核心动作。
- 首页第一层显示“今日还能花”，超支时主值最低为 0，并显示超出建议金额。
- 第二层显示本周剩余和预计月底余额；样本不足时显示明确文字。
- 第三层显示本月计划进度和存钱目标状态；第四层显示最近 5 笔账单。
- 无计划、零账单、正常、预警、风险、预测不足均有独立表达。
- 首页只消费 `DashboardUseCase` 和统一 `BudgetCalculator` 结果，Composable 不复制预算公式。

## Phase 5：快速记账与账单

- 默认快速路径只显示金额、分类和保存；“更多”中提供收支类型、收入归属、备注和日期。
- 输入金额后由 Domain `TransactionImpactCalculator` 显示“记账后今日剩余”或“今日将超出建议”。
- 保存先写 Room，Flow 自动驱动首页、账单与统计更新，动画不阻塞持久化。
- 全部账单支持搜索、收入/支出筛选、分类筛选、全部/本月/近 7 天筛选和按日期分组。
- 账单详情支持金额、分类、备注、日期编辑和删除；固定支出关联账单禁止直接编辑删除。
- 分类管理支持新增和归档。归档不删除实体，历史账单仍能显示原分类名称。

## Phase 6：统计与预测

- `StatisticsCalculator` 独立于 UI，聚合本月支出、分类占比、每日趋势、最高消费分类和近期有效消费日日均。
- 预测、存钱目标和风险状态直接复用 Budget Engine 结果。
- 空数据使用专门状态，不绘制误导性趋势。
- 分类图提供金额文字；趋势图提供文字摘要和 TalkBack 描述。
- 趋势图支持横向拖动并磁吸最近日期，联动显示日期、金额和 Halo。

## Phase 7：数据管理

- 完整 JSON 备份包含计划、分类、固定支出模板、月度实例、交易和 Reduce Motion 设置。
- CSV 导出包含账单通用字段，并添加 UTF-8 BOM 以提高表格软件兼容性。
- 使用 Android Storage Access Framework 创建/选择文件，写入后重新读取并验证字节数大于 0，成功提示包含真实字节数。
- 分享通过系统分享面板发送用户创建的 URI，并授予临时读取权限。
- 恢复前有覆盖风险二次确认；导入先完成格式、版本、主键和引用关系校验。
- 数据替换在一个 Room 事务中完成；预校验失败或数据库约束失败均不会留下半恢复数据。

## Phase 8：Design / Motion

- 建立统一 Color、Shape、Spacing、Typography 和 Motion Tokens。
- 建立 `PressableButton`、`YuliangBottomSheet`、`RollingMoney` 等复用组件。
- “＋记一笔”由胶囊核心动作连续展开为记账面板，面板内部使用内容尺寸动画，持久化不等待动画。
- 首页核心金额、月支出等使用 Rolling Number；列表前 7 项只在首次出现时轻量交错入场。
- 用户可开启 Reduce Motion；同时检测系统动画关闭状态。

## Phase 9：视觉增强与性能降级

- 首页 Hero 根据触点最大约 ±3° 微倾，松手弹簧回正。
- Budget Safe / Warning / Risk 由 Domain 风险等级驱动柔和状态光，同时保留文字语义。
- 页面和账单详情使用轻量容器过渡；为降低 API 与性能风险，没有引入实验性共享元素依赖。
- 系统 Reduce Motion、用户 Reduce Motion 或低内存设备任一满足时，关闭 3D 微倾、呼吸光、复杂弹簧与列表交错，只保留必要状态反馈。
- 未使用实时 Blur、高成本 Shader 或大面积持续阴影。

## Phase 10：发布配置与回归测试

- 版本更新为 `versionCode=10000`、`versionName=1.0.0`。
- Release 保持 R8 压缩，并从四个环境变量读取签名文件、密码、别名和密钥密码；私钥不进入仓库。
- 正式签名必须由产品所有者长期保管。临时测试密钥不能伪装成正式发布密钥。
- 新增回归测试后，测试源码共 32 项：Budget Engine 13、Room/Repository 7、Dashboard/Statistics 4、备份恢复与真实文件 4、Compose 启动导航与记账面板 4。

### 应执行的最终命令

```bash
./gradlew --no-daemon \
  :app:assembleDebug \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleRelease \
  :app:bundleRelease
```

Release 构建还需设置：

```text
YULIANG_RELEASE_STORE_FILE
YULIANG_RELEASE_STORE_PASSWORD
YULIANG_RELEASE_KEY_ALIAS
YULIANG_RELEASE_KEY_PASSWORD
```

## 本轮验证状态

| 验证项 | 状态 |
| --- | --- |
| `git diff --check` | 通过 |
| 禁止项扫描（破坏性迁移、主线程全局协程、TODO/FIXME） | 未发现 |
| 新增测试源码 | 32 项总测试已编写 |
| Debug Build | 未执行：Gradle 下载网络不可达 |
| Unit / Room / Compose Test | 未执行：缺少 Gradle 与 Android SDK |
| Android Lint | 未执行：缺少构建环境 |
| Release APK / AAB | 未生成 |
| 真机交互与性能 | 未执行 |

失败记录：Gradle Wrapper 尝试访问 `https://services.gradle.org/distributions/gradle-8.13-bin.zip` 时返回 `java.net.SocketException: Network is unreachable`。当前系统也未发现 Android SDK、Gradle 发行版或可复用依赖缓存。

## 已知限制

- Phase 10 不能在未实际构建和测试的情况下判定通过；当前没有新的可安装 APK 或 AAB。
- 多账户属于任务书标注的高级能力，本版未实现；转账 Domain 规则仍保持不计入预算。
- 页面详情采用稳定的轻量容器过渡，没有使用实验性共享元素 API。
- 图表磁吸已实现，但触感反馈属于可选项，当前未加入。
- 需在至少一台 Android 8、Android 12 和 Android 15 设备上验证 SAF、系统返回手势、字体放大、深色模式和动画降级。

## 下一步门槛

恢复可访问 Google Maven、Maven Central、Gradle 发行服务器的构建环境后，必须先执行上述完整命令。只有 Debug、32 项测试、Lint、签名 Release APK 和 AAB 全部成功，才能把 Phase 10 状态从“待验证”改为“通过”。
