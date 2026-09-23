# 余量 Phase 0 至 Phase 3 实施计划

基线：完整阅读《余量_ChatGPT_Work全新项目开发任务书》19 页，637 行。工作区为空，从零创建原生 Android 项目。本次上限为 Phase 3，阶段门槛没有通过时不进入下一阶段。

## Phase 0 工程初始化

单 app 模块，按 model、domain、data/database、data/repository、ui/feature、ui/theme 分层，随着功能加入包。采用 Kotlin 2.1.20、Compose 编译器 2.1.20、Compose BOM 2025.05.01、Material 3、AGP 8.11.0、Gradle 8.13、JDK 17。基础主题支持深浅色。初始化页与关于页用于启动和导航验证，不伪装成已完成首页。

固定正式 applicationId 为 com.yuliang.app；debug 使用 com.yuliang.app.debug，避免覆盖正式数据。minSdk 26（原生 java.time），compileSdk/targetSdk 35，versionCode 1，versionName 0.1.0。Release 开启压缩，尚未配置正式签名，不能把 debug 签名当正式方案。暂不申请网络权限，禁用系统自动备份，完整备份在 Phase 7 实施。

门槛：assembleDebug、启动与导航测试通过；记录测试运行环境，不将 Robolectric 等同于真机。

## Phase 1 数据层

Room 数据库版本 1。MonthlyPlan 增加 effectiveStartDate 表示月中启用；交易以分为 Long，记录收入归属；分类归档不硬删。固定支出采用模板与月度实例，实例有 yearMonth 和支付关联。建立月份、时间、关联唯一索引与外键。Repository 暴露 Flow；写入使用 suspend 和数据库事务；DataStore 保存主题等设置。使用简单显式依赖容器，当前规模不引入 Hilt。

门槛：真实 Room 插入、读取、更新、删除、外键及唯一约束、事务失败回滚测试；检查没有主线程磁盘 IO。

## Phase 2 预算引擎

Domain 不依赖 Android。统一 BudgetCalculator、BalancePredictor、FixedExpenseManager、SavingGoalCalculator。金额校验并防 Long 溢出。仅本月已发生交易参与当前可用预算；转账不影响收支。UPCOMING 与 PAID 固定支出只冻结一次；SKIPPED 不冻结，关联支出规则测试覆盖。

待编码时明确并写入算法说明：计划起始日到月底逐日分配整分，余数留给后续日期；今日建议取当日开始时额度，今日消费单独扣除。周范围裁剪到月和启用日，计算有效日期分配；不足 3 个消费日不预测。预测仅使用当月最近至多 7 个消费日样本，显示预计并记录样本数；具体余额口径、风险阈值须集中定义。

门槛：覆盖正常、零消费、今日超支与节省、负余额、固定支付防重、月中启动、三种收入、编辑删除、30/31 日与闰年、跨周跨月、预留超收入、异常金额和日期。

## Phase 3 计划与固定支出

通过 ViewModel/StateFlow/UseCase 连接计划表单、月度固定支出编辑、跳过和支付。支付关联使用原子事务，防重复点击。先成功落库再发布成功事件。可显示 Domain 结果用于验收，但不实现 Phase 4 首页。

门槛：修改计划、冻结/支付/跳过固定支出后，观察流产生正确派生结果；重复支付只产生一笔交易；跨月实例独立；输入错误有明确反馈；Build、数据与业务测试通过后停止。

## 后续范围

Phase 4 至 Phase 10 不在本次范围：首页、快速记账与完整账单、统计页面、备份导出、品牌动效、视觉增强、正式发布均未完成。

## 依赖依据

- https://developer.android.com/build/releases/agp-8-11-0-release-notes
- https://android-developers.googleblog.com/2025/05/whats-new-in-jetpack-compose.html
- https://kotlinlang.org/docs/whatsnew2120.html

版本固定是起点，实际兼容性必须以构建测试验证。
