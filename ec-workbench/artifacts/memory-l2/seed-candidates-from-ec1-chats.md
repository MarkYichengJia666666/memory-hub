# ec-1 Cursor 历史 → Chat Memory 种子候选

> 仓：`/Users/lipeng/IdeaProjects/ec-1` · 主会话 **45** · 过滤池 **306** · 精选 **15**
> **未写入 Memory**。可与 ec 清单对照；回复 `ec-1 写 …`。

## 推荐优先写（人工复核后）

| # | 建议稳定 path | 为什么值得记 |
|---|---|---|
| **2** | `ec/auth/ec-secured-api.md` | 闸门是 `@ECSecuredApi` 不是 `@SecureAPI`；找回链路哪些接口不该瞎加登录 |
| **6** | `ec/order/lazada-credit-push-only-risk.md` | Lazada 额度变更推 API 渠道：**只有**风控结果推送链路才会走 |
| **7** | `ec/auth/reuse-deleted-user-check.md` | 注销校验复用 `UserService.checkIdnDeletedUserRegister`，别抄新逻辑 |
| **14** | `ec/homepage/interest-free-card-every-visit.md` | 免息卡弹层：**每次**进首页请求资源位都触发，不是仅首次 |
| **3** | `ec/risk/reloan-fee-will-level-storage.md` | 意愿分落 `risk_output_result`（`RELOAN_FEE_WILL_LEVEL_V4`），可 mock |

不建议：1/5/8/9/11/12/13/15（文档修订、AICR 过程、revert、Memory 方案讨论）；4 偏运维查库名，可选。

## 全表

| # | 建议 path | 主题 | 一句话 |
|---|---|---|---|
| 1 ★ | `ec/bindcard/be2a690032.md` | bindcard | 三处文档 warn 已改完，死代码也清掉了，验证结果如下。 **文档修复（4 个文件）** - `explore.md` 新增「第四轮」：合 |
| 2 | `ec/bindcard/332cfd8dea.md` | bindcard | 复核完了。项目里没有 `@SecureAPI`，实际闸门是 `@ECSecuredApi`。按**整条找回链路**（含复用的现网接口）逐个对 |
| 3 ★ | `ec/homepage/1aaebbdf65.md` | homepage | 有落库，而且可以 mock。 **链路：** ``` risk 系统输出风控结果 → 写入 risk_output_result 表（字段  |
| 4 ★ | `ec/homepage/42b9a7e5b9.md` | homepage | 可以查，而且已经查到了。先前空结果是因为 service 名解析错了（落到了空的 `eu-cashloan-ec-api`），正确库是 `i |
| 5 ★ | `ec/repay/cbd0bcf9a0.md` | repay | AICR 返回 1 条 Minor 评审。 ## Step 2 / Step 3. 评审结果 **评审文件**: `IEcTicketFus |
| 6 | `ec/repay/d6f3dac10e.md` | repay | 不是“都会”。 从当前代码看，Lazada 的“额度变更推送到 API 渠道”这件事，**只有走风控结果推送链路时**，才会落到 `Laza |
| 7 ★ | `ec/risk/a1836bd898.md` | risk | 对，已经接到端内同一套注销校验上了，没有抄一份新逻辑。 **复用的就是** `UserService.checkIdnDeletedUser |
| 8 | `ec/risk/e87515687d.md` | risk | 评审完成。上一轮 reflect 的两条 warn（tasks 发码落点、US2-6 验收矩阵）已闭环，抽样核对四处代码与产物完全一致；本轮 |
| 9 | `ec/risk/0fd5126ed4.md` | risk | ## Revert 完成 已确认 `hotfix/collision_lifecycle` 分支上关于「GOPAY撞库放开【结清历史无坏账- |
| 10 | `ec/coupon/e73c9e8274.md` | coupon | 现在我有了完整的代码全貌。整理最终深度调研报告： --- ## 深度调研报告：复贷常规_首页免息卡迭代_V3 --- ### 一、需求核心变 |
| 11 | `ec/coupon/56973d4dad.md` | coupon | 明白了，你要的是**开发阶段 100% 拦住**，而不是上线后去抽检。用 TencentDB Memory 的能力，核心思路是——**让 A |
| 12 | `ec/coupon/68498c04ed.md` | coupon | 现在看到了完整代码。让我用**真实代码**还原事故链路，然后说清楚 Memory 在开发流程每个环节怎么拦。 --- ## 还原：这个 bu |
| 13 | `ec/experiment/65ab4e4506.md` | experiment | 第一轮的两条 warn 确认修好了，但这轮暴露出一条此前没人审视过的线：**US1 登录页入口的实验分流**。另外要更正我上一轮的一个判断— |
| 14 | `ec/homepage/61a063d064.md` | homepage | 从代码可以直接回答。 **触发时机：每次进入首页（请求资源位）都会触发，但不是只有第一次。** 具体逻辑是 `HomeInterestFre |
| 15 | `ec/order/ead38749f8.md` | order | 现在我对你们团队的资产全貌有了清晰的理解。下面是完整的落地方案。 --- ## TencentDB Agent Memory × EC 印尼 |

---

## 1. `ec/bindcard/be2a690032.md` · bindcard

- transcript: `e326c978-79bd-4daf-9b51-026401ef1caa`
- 符号：`UserMobileChangeService`, `AuthStepService`, `LoanUserRiskTraceService`, `UserService`

**decision 摘录：**

```
三处文档 warn 已改完，死代码也清掉了，验证结果如下。
**文档修复（4 个文件）**
- `explore.md` 新增「第四轮」：合并执行路径从 `modifyMobileNumberForApiChannel` 订正为 `UserMobileChangeService#doMergeAccount`（附行号 + 事务内 7 项副作用清单 + 为什么不能走旧方法），并补齐 `checkUserEkycWithSource`、`hasAvailableBankAccount` / `getLatestUsedBankAccountVO`、`AuthStepService` 与 `LoanUserRiskTraceService` 委托方法的路径行号；旧结论标注「已被推翻」而非删除。
- `tasks.md`：T017 补完整路径；Phase 1 的 covers 与门禁编号改成 `US3-5/US3-7/US5-5/US5-7` 和 `US3-1/US3-2/US3-4/US3-6`，删掉重复门禁行；Notes 的 `rebindPhone` 改 `confirmMerge`。现在 tasks 引用的 37 个编号与 spec 定义的完全一致。
- `plan.md` 决策 7 的同类漂移（`US5-6/US5-7` → `US6-6/US6-7`、`rebindPhone` → `confirmMerge`）一并修正。
- `phase-notes.md` 加表头声明：该
```

## 2. `ec/bindcard/332cfd8dea.md` · bindcard

- transcript: `e326c978-79bd-4daf-9b51-026401ef1caa`
- 符号：`GET /api/generalConfig`, `POST /api/account-recovery/send-phone-otp`, `POST /api/account-recovery/verify-phone`, `POST /api/account-recovery/match`, `GET /api/account-recovery/living-source`, `POST /api/advance/AuthorizationCode`, `GET /api/facePP/v5/getBizToken`, `POST /api/account-recovery/verify/face`
- 语境：Thursday, Sep 10, 2026, 3:17 PM (UTC+8) 我这个流程里，所有的接口，你都检查一下。 再复核一下你这个结论

**decision 摘录：**

```
复核完了。项目里没有 `@SecureAPI`，实际闸门是 `@ECSecuredApi`。按**整条找回链路**（含复用的现网接口）逐个对过代码，上一轮结论大体对，有两处要收紧。
### 全接口对照
| # | 接口 | `@ECSecuredApi` | 实际登录校验 | 结论 |
|---|------|-----------------|--------------|------|
| 0 | `GET /api/generalConfig`（入口开关） | 无 | `ACCOUNT_RECOVERY_CONFIG` 在免 userId 白名单，可不登录 | **正确，别加** |
| 1 | `POST /api/account-recovery/send-phone-otp` | 无 | 无 | **正确，别加** |
| 2 | `POST /api/account-recovery/verify-phone` | 无 | 无。新号走 `createUser` 会 `setUserTokenInCookie`；**同号复用路径不签发、不回写 token**，响应体也只有 `sessionToken` | 登录前接口，**不应加注解**；复用路径后面若依赖登录态会断 |
| 3 | `POST /api/account-recovery/match` | 无 | **只认 `sessionToken`**，不读 `userToken` | **缺口** |
| 4 |
```

## 3. `ec/homepage/1aaebbdf65.md` · homepage

- transcript: `43e21908-83ba-4ff8-b453-09672c85eea2`
- 符号：`RELOAN_FEE_WILL_LEVEL_V4`, `LoanUserRiskTraceService`, `HomeInterestFreeCardService`
- 语境：Thursday, Aug 13, 2026, 2:14 PM (UTC+8) 意愿分在EC侧有没有落库？能否mock

**decision 摘录：**

```
有落库，而且可以 mock。
**链路：**
```
risk 系统输出风控结果
    → 写入 risk_output_result 表（字段 type = RELOAN_FEE_WILL_LEVEL_V4，值存 new_value）
        → LoanUserRiskTraceService.getFeeWillLevelV4(accountId)
            → BusiRiskAdapter.getFeeWillLevelV4
                → HomeInterestFreeCardService.isLowFeeWillLevel
```
**结论：**
意愿分存在 `risk_output_result` 表里，直接按 `loan_account_id` + `type = RELOAN_FEE_WILL_LEVEL_V4` 查最新一条记录的 `new_value`。
**自测 mock 方法：**
直接在测试库 `risk_output_result` 表里 insert/update 对应 `loan_account_id` 的记录，把 `new_value` 改成 `"1"` 或 `"2"`，然后打首页接口，就能验证：
- `new_value = "1"` → 应该通过意愿分校验 → 可能入组
- `new_value = "2"` → V3 应该被拦截（V2 是通过的）
- `new_value = null` /
```

## 4. `ec/homepage/42b9a7e5b9.md` · homepage

- transcript: `3755a7cd-c638-45e3-b67c-200eecab29ab`
- 符号：`RiskFacadeService`, `RELOAN_REAPPLY`, `RELOAN_CALC_CREDITS_IN_REVIEW`, `RELOAN_CAN_REAPPLY_NOW`, `RiskFacade`
- 语境：Monday, Aug 3, 2026, 8:11 PM (UTC+8) 你自己不能查询吗？通过日志的形式

**decision 摘录：**

```
可以查，而且已经查到了。先前空结果是因为 service 名解析错了（落到了空的 `eu-cashloan-ec-api`），正确库是 `indo-cashloan-ec-api`。
**结论：53318 这次 Midscene 回端自动戳额是成功触发的，不是没跑。** UI 仍停在 `Cek Limit`，是因为风控很快拒绝了。
### 日志证据链（test / indo）
| 时间 | 证据 |
|------|------|
| **20:07:15.585** | `autoSubmitCreditsOnReturnExecutor` 调实验分流：`reloan_order_26h1-measurement-abroad-renew-get_limit11` → `EXPERIMENT_GROUP`，`experimentWhiteList=TRUE` |
| **20:07:15.604** | 同线程 `RiskFacadeService`：`submitScene=REAPPLY_CALC_CREDITS`，`riskType=RELOAN_REAPPLY`，`checkAndSubmitRiskIfNeed success`，`accountId=48873` |
| **20:07:25** | 首页侧已看到审核中：`RELOAN_CALC_CREDITS_IN_REVIEW`；`triggerSource=APP_RETURN_AUTO`，`triggerTy
```

## 5. `ec/repay/cbd0bcf9a0.md` · repay

- transcript: `29048e35-ceda-4bee-9903-2e585881a40f`
- 符号：`IEcTicketFusionLoanUserService`, `MockEcTicketFusionLoanUserService`, `TicketFusionLoanUserController`, `PaymentController`

**decision 摘录：**

```
AICR 返回 1 条 Minor 评审。
## Step 2 / Step 3. 评审结果
**评审文件**: `IEcTicketFusionLoanUserService.java`, `MockEcTicketFusionLoanUserService.java`, `TicketFusionLoanUserConverter.java`, `TicketFusionLoanUserController.java`, `TicketFusionPaymentCredentialListResponse.java`
**文件数**: 5 个
**评审意见**: 1 条
变更摘要（AICR 服务端）：
> 本次改动围绕工单融合 JBP 退款新增"支付凭证列表"查询能力，目标是让调用方先获取可用入账账户，再安全组装退款请求参数。代码在 internal API 与 client 层新增 `paymentCredentials` 接口，并在控制器中按 admin 同源链路查询主账户与 payout 凭证；转换层新增专用映射逻辑，仅保留 `BANKCARD`，过滤非银行卡导致的 `null/异构` 项，统一返回结构。
---
### 🔴 必须修复 (0 条)
无
### 🟡 建议修复 (1 条)
| # | 文件 | 行号 | 问题描述 | AI 建议 | agent | rule |
|---|------|------|----------|---------|-------|
```

## 6. `ec/repay/d6f3dac10e.md` · repay

- transcript: `dedc92fa-e126-4017-b5ce-36429001c5fb`
- 符号：`ChannelPushService`, `ApiChannelCallBackService`

**decision 摘录：**

```
不是“都会”。
从当前代码看，Lazada 的“额度变更推送到 API 渠道”这件事，**只有走风控结果推送链路时**，才会落到 `LazadaBuyerApiChannelPushProvider#pushCreditModificationResultToApiChannel` 这条路径。
主链路是：
`LoanMarketObserver#processRiskEvent` -> `ChannelPushService#pushRiskResult` -> `ChannelPushService#pushCalcCreditRiskResult` / `pushOrderRiskResult` -> `LazadaBuyerApiChannelPushProvider#pushCreditStatus(...)` -> `pushCreditModificationResultToApiChannel(...)` -> `ApiChannelCallBackService#callBackCreditModificationToApiChannel(...)` -> `iApiChannelCallBackServiceApi.creditModificationResult(...)`
而且 repo 里我搜过了，`creditModificationResult` 这类 API 渠道额度变更接口的业务入口，当前只有 `ApiChannelCallBackService#
```

## 7. `ec/risk/a1836bd898.md` · risk

- transcript: `e326c978-79bd-4daf-9b51-026401ef1caa`
- 符号：`UserService`, `AccountRecoveryController`, `AccountRecoveryService`, `UserController`, `MobileController`
- 语境：Friday, Sep 11, 2026, 5:38 PM (UTC+8) 哦，那看起来，com.miyou.controllers.account.AccountRecoveryController#sendPhoneOtp 得复用这个的校验逻辑啊

**decision 摘录：**

```
对，已经接到端内同一套注销校验上了，没有抄一份新逻辑。
**复用的就是** `UserService.checkIdnDeletedUserRegister`（端内 `REGISTER` / `REGISTER_OR_LOGIN` 发 OTP 走 `migrateCheck` 时最终也是它）。
落点：
1. `AccountRecoveryController#sendPhoneOtp` 把 Header 里的 `build` / `platformType` 传下去（和端内发码一样，缩量/迁回要看端）。
2. `AccountRecoveryService#assertReusableShellUserOrThrow`：非壳账号且已注销时先调 `checkIdnDeletedUserRegister`。
3. `UserController#accountRecoveryVerifyPhone` 也走同一个方法，避免只拦发码、验码还能过。
行为：
| 新手机号状态 | 结果 |
|---|---|
| 未注册 / 可复用壳 | 照常发 OTP |
| 已注销且不允许迁回 | 「账户已被注销，无法继续使用」（和端内一致） |
| 已注销但允许迁回 | 号仍被占用，继续 2003「该手机号已注册，请直接用该号码登录」——找回新号不能当空号；真正迁回应走端内注册，不是找回 |
| 正常已注册非壳 | 仍是 2003 |
没有整段复用 `MobileController.verifyM
```

## 8. `ec/risk/e87515687d.md` · risk

- transcript: `e326c978-79bd-4daf-9b51-026401ef1caa`
- 符号：`UserSecureCheckFacade`, `LoanCreditService`, `TongDunTGService`
- 语境：Available subagent_types and a quick description of what they do: - generalPurpose: General-purpose agent for researching complex questions, searching for code,

**decision 摘录：**

```
评审完成。上一轮 reflect 的两条 warn（tasks 发码落点、US2-6 验收矩阵）已闭环，抽样核对四处代码与产物完全一致；本轮新发现一处真实漂移：**同盾风控节点接入已在代码里落了 4 处，但 372503 的 explore/plan/tasks 仍写着「已决策，不触发」**。`TAPD-377558` 走简化流程，三个维度全 pass，勾选状态与代码一致。
## 总览
### 工作流分布
- 标准流程: 1 个 (50%)
- 简化流程: 1 个 (50%)
- 跳过（无产物）: 0 个
### 问题热力图
按评价维度统计 warn + fail 占比，降序排列：
| 维度 | warn | fail | 问题率 | 严重度 |
|------|------|------|--------|--------|
| 代码现实基础 | 1 | 0 | 100% | 高频 |
| 改动面清晰度 | 1 | 0 | 100% | 高频 |
| 验收完整性 | 1 | 0 | 100% | 高频 |
| WHAT vs HOW | 1 | 0 | 100% | 高频 |
严重度分级：
- **高频**（> 50%）：团队级短板，建议专项改进
- **中频**（25%-50%）：值得关注
- **低频**（
 说明：标准流程维度的分母为标准流程需求数（=1），单个 warn 即计为 100%，需结合明细看实际严重程度。
---
## 改进建议
1. **把同盾风控节点接入正
```

## 9. `ec/risk/0fd5126ed4.md` · risk

- transcript: `9864b891-941b-493b-9976-ba365ab24783`
- 符号：`RELOAN_COMPLETED`

**decision 摘录：**

```
## Revert 完成
已确认 `hotfix/collision_lifecycle` 分支上关于「GOPAY撞库放开【结清历史无坏账-授信未下单】标签」需求本身的改动已全部撤销，代码与知识图谱都回到了"知识图谱重建 chore commit"之后、这个具体需求实施之前的干净状态（`invariant-check` 0 BLOCKING，与重建后原始基线一致）：
| 文件 | 状态 |
|---|---|
| `SubmitApplicationChecker.java` | 已还原：`NO_ORDER_AFTER_RELOAN_COMPLETED` 回到 `CHECK_USER_FAIL` 分支 |
| `SubmitApplicationCheckerTest.java` | 已还原：新增用例移除，原 `@Disabled` 失败态用例恢复 |
| `submit_application.yaml` / `invariants.yaml` / `topology.yaml` | 已还原为重建后的原始映射（`FAIL`/`MANAGEABLE_OLD_USER`） |
| `specs/collision-knowledge/` 其余 13 个 YAML、`ec-script/collision-platform/`、`.cursor/skills/collision-lifecycle/` | 未动 |
⚠️ 有一点需要你知悉：Phabricator 上的 **D6154
```

## 10. `ec/coupon/e73c9e8274.md` · coupon

- transcript: `43e21908-83ba-4ff8-b453-09672c85eea2`
- 符号：`HomeInterestFreeCardService`, `HomeInterestFreeCardHomePopupFilterRuleProcessor`, `HomeInterestFreeCardPopupFilterRuleProcessor`, `HomeInterestFreeCardFloatingIconFilterRuleProcessor`, `UserFlowExperimentEnum`, `AntiSettlementLongTermExpService`, `ExperimentKeyConstants`, `LoanUserCouponService`
- 语境：Wednesday, Aug 12, 2026, 5:31 PM (UTC+8) 你再仔细调研下吧

**decision 摘录：**

```
现在我有了完整的代码全貌。整理最终深度调研报告：
---
## 深度调研报告：复贷常规_首页免息卡迭代_V3
---
### 一、需求核心变化对比（V2 → V3）
| 维度 | V2（现状） | V3（本次） | 变化量级 |
|------|-----------|-----------|---------|
| 实验结构 | 头+中部合并一个实验 key | 拆为头部、中部**两个独立实验** | 高 |
| 意愿分 | `reloan_fee_will_level_v4 = 1 & 2` | 仅 `= 1` | 中 |
| 防结清对照组互斥 | 已有：`abResultByUserId` 前置只读 | 继续沿用，无变化 | 无 |
| 防结清券实时校验 | **不存在** | 实时校验账户内有无指定工具ID发的券 | **新功能** |
| 其余所有逻辑 | 状态机/弹窗/挂件/冷静期/频控/发券 | 完全不变 | 无 |
---
### 二、现有代码结构（V2 实现摘要）
**核心文件路径：**
```
ec-core/.../service/cashloan/activity/HomeInterestFreeCardService.java          # 核心业务逻辑
ec-core/.../service/general/pageconfig/filterstrategy/processor/
  ├── HomeInterestFreeCardHomePo
```

## 11. `ec/coupon/56973d4dad.md` · coupon

- transcript: `feba24ea-fbab-4331-990b-f4e7af9b30cd`
- 符号：`ExperimentClient`, `AbstractExpClient`, `ExpDiversionClient`, `CashLoanService`, `LoanCouponGrantService`
- 语境：Wednesday, Aug 12, 2026, 8:10 PM (UTC+8) 不，我就是要利用tencentDB能力，你这么抽检就没意思了。 我就是要保证开发完100%没问题。

**decision 摘录：**

```
明白了，你要的是**开发阶段 100% 拦住**，而不是上线后去抽检。用 TencentDB Memory 的能力，核心思路是——**让 Agent 在写代码、Review 代码时，自动想起这次事故的教训，从根上不让同类 bug 出来**。
## TencentDB Memory 怎么保证"写的时候就不出问题"
### 第一层：Chat Memory —— 团队认知自动注入
把这次事故沉淀为三层 Memory：
| 层级 | 内容 | 触发时机 |
|---|---|---|
| **L1 事实** | "泳道空白组用户 groupType=null，扩全（FP）时 fullOrZeroGroupResult 会覆盖为 EXPERIMENT_GROUP，导致空白组用户被误判为实验组" | 任何涉及实验分组的对话 |
| **L2 教训** | "封装或重构实验 getResult() 时，必须保留对 groupType==null 的判断。丢掉这个判断 = 扩全后空白组用户全部污染" | 改动实验适配器代码时 |
| **L3 团队认知** | "实验相关代码变更必须覆盖三态断言：RUNNING / FULL_PERCENTAGE / ZERO_PERCENTAGE，且每种状态都要验证泳道空白组（groupType=null）的返回值" | 所有实验相关需求的 SDD / CR |
**工作方式**：开发者在 Cursor/CC 里说"我要重构实验适配器"或者代码 diff 触及
```

## 12. `ec/coupon/68498c04ed.md` · coupon

- transcript: `feba24ea-fbab-4331-990b-f4e7af9b30cd`
- 符号：`AbstractExpClient`, `CashLoanService`, `OpenAppGrantCouponService`, `RepayPlanGrantCouponService`
- 语境：Wednesday, Aug 12, 2026, 8:16 PM (UTC+8) 串起来：完整开发流程中的防线 这个可以再细一点

**decision 摘录：**

```
现在看到了完整代码。让我用**真实代码**还原事故链路，然后说清楚 Memory 在开发流程每个环节怎么拦。
---
## 还原：这个 bug 的生命周期
事故的核心在 `ExperimentAdapter.getResult()` 方法。当前代码（**已修复后**）：
```110:128:ec-core/src/main/java/com/yqg/core/userflow/infrastructure/adapter/impl/ExperimentAdapter.java
  String getResult(ExperimentAdapterResultVO result, boolean ignoreFullPercentage, boolean ignoreZeroOrClosed) {
    if (result == null) {
      return EMPTY_RESULT;
    }
    if (result.isExperimentWhiteList() && StringUtils.isNotBlank(result.groupResult)) {
      return result.groupResult;
    }
    // 未入组
    if (result.groupType == null) {
      return StringUtils.defaultString(result.groupResult);
  
```

## 13. `ec/experiment/65ab4e4506.md` · experiment

- transcript: `e326c978-79bd-4daf-9b51-026401ef1caa`
- 符号：`VerificationService`, `UserEkycCheckService`

**decision 摘录：**

```
第一轮的两条 warn 确认修好了，但这轮暴露出一条此前没人审视过的线：**US1 登录页入口的实验分流**。另外要更正我上一轮的一个判断——`AccountRecoveryConfigProvider` 是真实存在的（在 `service/general/appconfig/provider/` 下，不在 recovery 包里），我当时只搜了 recovery 包就下了结论，已把写进 `phase-notes.md` 的那句错话改成更正条目。
## 总览
### 工作流分布
- 标准流程: 1 个 (50%)
- 简化流程: 1 个 (50%)
- 跳过（无产物）: 0 个
### 问题热力图
按评价维度统计 warn + fail 占比，降序排列：
| 维度 | warn | fail | 问题率 | 严重度 |
|------|------|------|--------|--------|
| 改动面清晰度 | 1 | 0 | 100% | 高频 |
| 验收完整性 | 1 | 0 | 100% | 高频 |
| spec ↔ plan 一致性 | 1 | 0 | 100% | 高频 |
| 可执行性（简化流程） | 1 | 0 | 100% | 高频 |
严重度分级：
- **高频**（> 50%）：团队级短板，建议专项改进
- **中频**（25%-50%）：值得关注
- **低频**（
 说明：每个维度的分母是该流程类型的需求数（标准=1，简化=1），单个 war
```

## 14. `ec/homepage/61a063d064.md` · homepage

- transcript: `43e21908-83ba-4ff8-b453-09672c85eea2`
- 符号：`HomeInterestFreeCardHomePopupFilterRuleProcessor`, `ExperimentKeyConstants`, `UserFlowExperimentEnum`
- 语境：Thursday, Aug 13, 2026, 11:52 AM (UTC+8) 这个实验是在什么时候触发？第一次进入首页吗？

**decision 摘录：**

```
从代码可以直接回答。
**触发时机：每次进入首页（请求资源位）都会触发，但不是只有第一次。**
具体逻辑是 `HomeInterestFreeCardHomePopupFilterRuleProcessor.hitRule`，在用户每次请求首页资源位配置时都会执行：
```93:108:ec-core/src/main/java/com/yqg/core/service/general/pageconfig/filterstrategy/processor/HomeInterestFreeCardHomePopupFilterRuleProcessor.java
  private boolean triggerAbtestOfHomeInterestFreeCardAbtest(GeneralPageConfigParam param) {
    // 检查是否符合基本群体条件（含版本门槛，收敛在 isEligibleUser 内部判断）
    if (!homeInterestFreeCardService.isEligibleUser(param.getUserId(), param.build)) {
      log.info("[HomeInterestFreeCard] not hit banner home popup abtest, userId is {}, build is {}", param.getUserId(), param.build);
 
```

## 15. `ec/order/ead38749f8.md` · order

- transcript: `feba24ea-fbab-4331-990b-f4e7af9b30cd`
- 符号：`MonitorService`, `CashLoanService`, `LoanAccountService`, `ConsumerPoolService`, `LoanBankAccountService`, `CashLoanCreditsService`, `CashLoanCreditsQueryService`
- 语境：Tuesday, Aug 11, 2026, 6:04 PM (UTC+8) 我想让他成为我们印尼C端团队的【经验中枢】，在我们代码开发，和问题排查等场景都能够输出经验之谈，或者依托你的优势，创造更大的可能性。 你结合一下这个memory的优势，和你熟知的开发流程。 看下如何落地

**decision 摘录：**

```
现在我对你们团队的资产全貌有了清晰的理解。下面是完整的落地方案。
---
## TencentDB Agent Memory × EC 印尼 C 端团队 —— 落地方案
### 一、现状盘点：你们已经有大量「经验」，但散落各处
| 经验载体 | 当前位置 | 痛点 |
|---|---|---|
| 26 条编码规则 | `.cursor/rules/*.mdc` | 仅 Cursor 本地生效，换 IDE/Agent 框架就丢失 |
| 13 个 Skill | `.cursor/skills/*/SKILL.md` | 包括排查、jOOQ codegen、UI 验证等，但只能被一个人的 Cursor 调用 |
| 30+ 篇业务文档 | `docs/` 8 大目录 | Agent 每次都要先 Read 几个文件才能理解上下文 |
| 架构约束 | `AGENTS.md` + `ARCHITECTURE.md` | 1100+ 行，新人/新 Agent 冷启动成本高 |
| 排查经验 | `prod-issue-investigator` 1100 行 | 11 类异常模式（A~K）是团队血泪经验，但只活在一个 SKILL.md 里 |
| 历史排查 Session | 散落在各自 Cursor 会话 | 上次排查学到的坑，下次换个人就不知道了 |
**核心矛盾：经验有沉淀，但不流动、不跨人、不跨工具。**
---
### 二、Memory 如何映射到你们的场景
#### 场景
```
