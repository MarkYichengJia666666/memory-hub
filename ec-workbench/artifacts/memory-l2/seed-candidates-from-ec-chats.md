# EC 历史对话 → Chat Memory 种子候选（精炼）

> 扫描会话文件约 **181** · 硬约束+符号过滤后池 **279** · 精选 **12**
> **未写入 Memory**。回复例如：`按推荐写` / `写 1,4,10` / `全写`。

已有可跳过：`ec/coupon/open-app-grant.md`（发券在 Kafka）。

## 推荐优先写

| # | 建议稳定 path | 为什么值得记 |
|---|---|---|
| **1** | `ec/bindcard/saqu-superbank-pattern.md` | Saqu 绑卡可对标 SuperBank（开关+只留自家卡） |
| **4** | `ec/coupon/v1-dead-v2-kafka.md` | v1 发放已死；活着的是 v2→Kafka（补强 open-app-grant） |
| **10** | `ec/risk/jooq-null-traceid-bind.md` | `findByTraceIdAndType(null,…)` 仍绑 WHERE → EMPTY_BIND |

不建议进种子：3/5（过程态）、6/7/8（单次排查）、2（实验态易过期）、11/12（与 10 重复）。9 可选。

## 全表（自动精选）

| # | 建议 path | 主题 | 一句话 | 来源 |
|---|---|---|---|---|
| 1 | `ec/bindcard/19d4a01363.md` | bindcard | **做过，而且 H5（WEB）走的就是同一套。** SuperBank 渠道「只能绑自家卡 + 可开关」已经落地，Saqu 可直接对标复用。 | ec |
| 2 | `ec/bindcard/231ac001fe.md` | bindcard | **结论：不能埋。** Capillary 判成 **`live`（还在生效）**。 报告：`tools/capillary/reports | memory-hub-ws |
| 3 | `ec/coupon/56e9c01740.md` | coupon | **第一刀已经落地：平台状态、切片图、记忆都对上了。** 面板在 [http://localhost:8125](http://localh | ec |
| 4 | `ec/coupon/a7511cf8a8.md` | coupon | 第二问打完了。这根券把说明书又揭了一层。 **v1 那个 key，发放已经不在了。** 换成 `RELOAN_TOP_LOW_WILL_CO | ec |
| 5 | `ec/coupon/1f647f626a.md` | coupon | [Graph tasks](e763e9e0-4140-47da-bf6f-9e2093aad51e) 已落 `sdd-proof/live | ec |
| 6 | `ec/homepage/3d9982d6c6.md` | homepage | 上报接口 502，已向你告知。排查结论如下： --- ## 排查结论 — trace `452881f5392e19904c19b995b7 | ec |
| 7 | `ec/repay/bedf2e8427.md` | repay | 现在我有了完整的分析。让我整理出结论： **核心风险点：** `ChannelPushService.getChannel()` 第 66- | ec |
| 8 | `ec/repay/4f7c963bb8.md` | repay | 已找到完整调用链路。让我上报排查结果。 Now I have a very clear picture of the notificatio | ec |
| 9 | `ec/repay/7ce4f4dc36.md` | repay | 已按「新起一个 Consumer Group」做完改造，并修了 Service 里的方法名拼写。变更总结如下。 --- ## 1. 新增独立 | ec |
| 10 | `ec/risk/2f3d2a67b0.md` | risk | **Case 6 已完成**，6 个 `EMPTY_BIND_VALUE` case 全部修完。 ## 根因 `findByTraceIdA | ec |
| 11 | `ec/risk/5f87eec0b9.md` | risk | 你说得对，这里需要把「改几处」和「为什么」分开说清楚。 ## 不是只改了两处 Case 6 实际动了 **4 个 Service 文件**（ | ec |
| 12 | `ec/risk/ffdec6bfef.md` | risk | 全量扫完了。可以确认。 --- **是的，之前的汇总是完整的。** 以下是核验结论： **涉及两张表的所有模块**（逐一排查）： | 模块  | ec |

---

## 1. `ec/bindcard/19d4a01363.md` · bindcard

- 来源：`ec` / `8a9e3015-a628-4453-95eb-027a69480838`
- 符号：`GET /api/idn/cashloan/getSupportedBank`, `LoanBankAccountService`, `keepOnlySuperBank`, `LoanBankAccountController`, `SUPERBANK_CHANNEL_USER_BINDING_CARD`
- 语境：Wednesday, Aug 26, 2026, 4:42 PM (UTC+8) 1）Saqu用户只能绑Saqu的银行卡； 2）加个开关，随时可以切换是否只支持saqu的银行卡或者所有的银行卡 H5全流程做过类似的东西吗？你可以搜一下SuperBank关键字

**decision 摘录：**

```
**做过，而且 H5（WEB）走的就是同一套。** SuperBank 渠道「只能绑自家卡 + 可开关」已经落地，Saqu 可直接对标复用。
## 现成能力（SuperBank）
挂在印尼支持银行列表接口上：
- 接口：`GET /api/idn/cashloan/getSupportedBank`
- 端外清单：`wiki/app-perspective/outside-web-api-inventory.md` 已标该接口给 **WEB**（H5 全流程会调）
核心逻辑在 `LoanBankAccountService.keepOnlySuperBankIfInExperiment`：
```983:1040:ec-core/src/main/java/com/yqg/core/service/loan/bankaccount/LoanBankAccountService.java
  /**
   * 命中"SuperBank渠道绑卡"实验组（B组）时，仅保留 SuperBank。
   */
  public List
 keepOnlySuperBankIfInExperiment(...) {
    // 1) deviceId → Appsflyer mediaSource
    // 2) mediaSource 必须在配置白名单内
    // 3) 命中实验才过滤
    // 4) 仅保留 BankType.FAMA（展示名 Super Bank Ind
```

## 2. `ec/bindcard/231ac001fe.md` · bindcard

- 来源：`memory-hub-ws` / `9e77d785-12d2-4eba-816e-60c2303afe4b`
- 符号：`LoanBankAccountController`, `keepOnlySuperBank`, `ExpFacade`, `SUPERBANK_CHANNEL_USER_BINDING_CARD`
- 语境：Tuesday, Sep 8, 2026, 6:48 PM (UTC+8) 那你看看 com.yqg.core.service.abtest.enums.ExperimentNameSpace#SUPERBANK_CHANNEL_USER_BINDING_CARD 这个，用你的代码半自动agent检测一下

**decision 摘录：**

```
**结论：不能埋。** Capillary 判成 **`live`（还在生效）**。
报告：`tools/capillary/reports/capillary-20260908-184847.md`（vessel `superbank-bind`）
### 平台（MCP 实时）
| 项 | 值 |
|---|---|
| key | `technology-auth-abroad-loan-superbank_rename_0908` |
| 状态 | **FULL_PERCENTAGE** |
| 流量 | 对照 0% / **实验 100%（TRUE）** / 空白 0% |
| 描述 | 【鉴权】superbank改名&排位 |
### 代码（绑卡图 `cg-ld353q4p`）
```
LoanBankAccountController.getIdnSupportedBankListWithLogo
  → keepOnlySuperBankIfInExperiment
  → isInSuperBankBindCardExperiment
  → ExpFacade(... "technology-auth-abroad-loan-superbank_rename_0908" ...)
  → ExperimentNameSpace.SUPERBANK_CHANNEL_USER_BINDING_CARD
```
枚举本身 callers 空（老分流常见）；**方法链是
```

## 3. `ec/coupon/56e9c01740.md` · coupon

- 来源：`ec` / `19835a78-4101-4c44-887a-5b8a41d14957`
- 符号：`NationalDay817ExpService`, `isAtmosphereGroup`, `RELOAN_TOP_LOW_WILL_COUPON`, `NationalDay817HomeBannerFilterRuleProcessor`
- 语境：Monday, Aug 31, 2026, 2:17 PM (UTC+8) 继续

**decision 摘录：**

```
**第一刀已经落地：平台状态、切片图、记忆都对上了。** 面板在 [http://localhost:8125](http://localhost:8125)。
三个 key 是现查的实验平台，不是猜的：
| key | 平台 | 图上能钉住的根 |
|---|---|---|
| `first_loan_...-Rayakan_Kemerdekaan` | **ZERO_PERCENTAGE**，实验/对照 0%，BLANK 100% | `NationalDay817ExpService.isAtmosphereGroup` |
| `reloan_...-Rayakan_Kemerdekaan` | 同上 | 同一个方法，按客群换 key |
| `reloan_...-top_willing_coupon_v1` | **LIGHT** 且剩余时长 0；重开 v3 已关量；代码 `@Deprecated` | 常量 `RELOAN_TOP_LOW_WILL_COUPON` |
陪跑实验 `product_detail_refactor_compare` 仍是 **RUNNING** 且还有流量，没放进这一刀。
**CodeGraph** `cg-pl6vfmyi`：ready，9 个 Java 文件，652 节点 / 753 边。不是全仓。
图上已经跑通的查询：
- `isAtmosphereGroup` 的 callers：`ExpInfoDisplayDataBuil
```

## 4. `ec/coupon/a7511cf8a8.md` · coupon

- 来源：`ec` / `19835a78-4101-4c44-887a-5b8a41d14957`
- 符号：`RELOAN_TOP_LOW_WILL_COUPON`, `RELOAN_TOP_WILLING_COUPON_V2`, `AppStartUpGrantCouponProcessor`, `ExpDiversionClient`
- 语境：Monday, Aug 31, 2026, 5:33 PM (UTC+8) 继续

**decision 摘录：**

```
第二问打完了。这根券把说明书又揭了一层。
**v1 那个 key，发放已经不在了。**  
换成 `RELOAN_TOP_LOW_WILL_COUPON` 能搜到常量，callers 是空的。它还写在 `ABTestConfig` 版本表里，但 `map.put(常量)` 图不当成调用。没有 Kafka、没有 Job、没有 HTTP 再发这张券。
**活着的是 v2。**  
枚举 `RELOAN_TOP_WILLING_COUPON_V2`。入口不是定时任务，是 **Kafka** `AppStartUpGrantCouponProcessor` → `grantCutInterestCouponForOpenApp` → `resolveReloanHeadCutInterestCouponIds` → `ExpDiversionClient.getResult`。这截方法调用图上有。
C 端开屏 HTTP 只组参、发事件，发券在消费端。枚举本身也没有 callers：代码写的是 `.getKey()`，图不把「用了这个枚举」连成边。
另外：817 走 `IExperimentAdapter`，灌券走 `ExpDiversionClient`。同一家公司两套分流，查图不能只认一种。
图现在 26 个文件。说明书在 `PLAYBOOK.md`，复跑：
`python3 /Users/lipeng/IdeaProjects/exp-ligation-slice/tools/que
```

## 5. `ec/coupon/1f647f626a.md` · coupon

- 来源：`ec` / `19835a78-4101-4c44-887a-5b8a41d14957`
- 符号：`RealTimeEventController`, `AppController`, `AppStartUpGrantCouponProcessor`, `ConsumerPoolService`, `CouponTaskGrantJob`
- 语境：Tuesday, Sep 1, 2026, 7:29 PM (UTC+8) Perform any necessary follow-up actions in response to the subagent completion above. If no follow-up work is needed, no f

**decision 摘录：**

```
[Graph tasks](e763e9e0-4140-47da-bf6f-9e2093aad51e) 已落 `sdd-proof/live/tasks-graph.json`。没再搜仓、没问图，墙上 37 秒。
它点名了 `RealTimeEventController`、`AppController`、`AppStartUpGrantCouponProcessor`，以及消费拉起的 `ConsumerPoolService` / `topic_config`。说明里又写了「不要把 `CouponTaskGrantJob` 写成入口」——针还在，对照到了再一起判。
另外三臂还在跑。
```

## 6. `ec/homepage/3d9982d6c6.md` · homepage

- 来源：`ec` / `3ff96fd7-bf28-4761-b4a3-44e7ce957278`
- 符号：`GET /api/v5/cashloan/home`, `LoanInvitationConfigService`, `ProductConfigService`, `LoanMarketUserQualifyService`

**decision 摘录：**

```
上报接口 502，已向你告知。排查结论如下：
---
## 排查结论 — trace `452881f5392e19904c19b995b7d0801e`
### 基本信息
| 字段 | 值 |
|------|-----|
| 接口 | `GET /api/v5/cashloan/home?triggerSource=INIT` |
| 时间 | 2026-06-30 17:15:39 |
| 用户 | userId=**54806114**，Android 39013，Telkomsel |
| 响应 | **HTTP 200**，耗时 263ms |
### 唯一 ERROR 日志
```
[LoanInvitationConfigService] current time do not have invitation campaign
```
**结论：这是正常业务分支，不是故障。** 该用户访问首页时，系统检查邀请活动配置，当前没有活跃的邀请活动，代码走 ERROR 日志分支，但接口整体正常返回 200。
### 其他值得关注的 INFO 日志（非 ERROR）
- **`ProductConfigService`**：productId 3224 / 3154 / 3106 的 `upperLimit(0) < lowerLimit(100000)`，该用户 credits=0，属于还款中用户，无可借额度，配置过滤正常
- **`ProductInfoServiceI
```

## 7. `ec/repay/bedf2e8427.md` · repay

- 来源：`ec` / `a8c30e01-64c2-45a8-8965-440f9f93fe81`
- 符号：`ChannelPushService`, `LoanCommonApiChannelController`, `LoanRiskProcessor`, `UserService`
- 语境：Tuesday, Apr 28, 2026, 3:51 PM (UTC+8) 1. 已经停止合作了，这意味着不会有新的用户和新的订单 通过这个你判断下可删除的范围

**decision 摘录：**

```
现在我有了完整的分析。让我整理出结论：
**核心风险点：** `ChannelPushService.getChannel()` 第 66-67 行：
```java
if (SourceType.API_CHANNEL_LIST.contains(orderVO.sourceType)) {
    return ApiChannel.fromSourceType(orderVO.sourceType).pushChannel;  // 删 ApiChannel.KREDUIT 后会抛异常
}
```
`SourceType.KREDUIT` 在 ec-common 的 `API_CHANNEL_LIST` 里（不能动），4 笔在还款的订单触发推送时，`fromSourceType(KREDUIT)` 会抛出未被捕获的异常，传播到 `LoanMarketObserver`，可能影响还款流程。
Now I have a clear picture of all the files involved. Let me put together the plan:
**Root cause of the risk:** `ChannelPushService.getChannel()` at line 66-67 does:
```java
if (SourceType.API_CHANNEL_LIST.contains(orderVO.sourceType)) {
    retur
```

## 8. `ec/repay/4f7c963bb8.md` · repay

- 来源：`ec` / `b8cdc13d-885d-440c-b893-dc1724fb39dc`
- 符号：`TargetTaskProducerJob`, `TargetTaskConsumerJob`, `NotifUserSelectorController`

**decision 摘录：**

```
已找到完整调用链路。让我上报排查结果。
Now I have a very clear picture of the notification flow. Let me summarize what I've found:
1. The message "Hi, pinjamanmu jatuh tempo pada 30/06/2026, mohon masuk ke dalam APP untuk membayar." is a loan repayment due reminder.
2. The flow is:
   - `TargetTaskProducerJob` (ec-scheduler) → triggers selectors via `NotifUserSelectorFactory`
   - The relevant selectors are:
     - `LoanRepaymentDueSelector` (NotifQueryType: `LRD` - "贷款偿还到期") - selects users based on instalment billing date
     - `RepaymentReminderInPersonDimensionSelector` (NotifQueryType: `RRIPD` - "贷款偿还到期人维度通知") - selects users based on person dimension
3. 
```

## 9. `ec/repay/7ce4f4dc36.md` · repay

- 来源：`ec` / `80274d26-9e58-45bf-913f-43f69317c415`
- 符号：`DownstreamOrderRepaySyncProcessor`, `BaseCashLoanEventProcessor`, `DownstreamOrderRepaySyncService`
- 语境：我觉得这样不够保险，你还是给我新起一个consumerGroup吧

**decision 摘录：**

```
已按「新起一个 Consumer Group」做完改造，并修了 Service 里的方法名拼写。变更总结如下。
---
## 1. 新增独立 Consumer Group
- **枚举**：在 `ConsumerGroup` 中新增 `CASH_LOAN_EVENT_DOWNSTREAM_SYNC`，对应 `DownstreamOrderRepaySyncConsumerGroupContext`。
- **Context**：`DownstreamOrderRepaySyncConsumerGroupContext`  
  - Topic：`CASH_LOAN_EVENT`  
  - Group Id：`CASH_LOAN_EVENT_DOWNSTREAM_SYNC`  
  - `auto.offset.reset=latest`（只消费新消息，不回溯历史）  
  - 继承 `AbstractGenericConsumerGroupContext`。
- **Processor**：`DownstreamOrderRepaySyncProcessor` 继承 `BaseCashLoanEventProcessor`，只挂载下面的 Observer。
- **Observer**：`DownstreamOrderRepaySyncObserver` 实现 `ICashLoanOrderObserver`，仅重写：
  - `onOrderReady` → `Downstr
```

## 10. `ec/risk/2f3d2a67b0.md` · risk

- 来源：`ec` / `b0caa16c-8535-404b-8b93-2c4c96a98233`
- 符号：`POST /api/v2/loan/reapply`, `RiskOutputService`, `CashLoanCreditsService`, `LoanUserRiskTraceService`

**decision 摘录：**

```
**Case 6 已完成**，6 个 `EMPTY_BIND_VALUE` case 全部修完。
## 根因
`findByTraceIdAndType(null, …)` 时仍绑定 `WHERE trace_id = ?`，index 0 为 null → 告警。
| 样本 | 说明 |
|------|------|
| Bitable `330ef99…` | ec-internal-api `getUserFeatures`，payload 里 **`traceId=null`**；当次只拉 `userAge`，该 trace 未见 Model 告警（可能与 Case 2/4 一样 Bitable 错位） |
| 主样本 `fb130217…` | ec-api `POST /api/v2/loan/reapply`，同 trace **3 次**告警，均为 **traceId=null** |
## 修复（Service 源头，已撤销 Model 判空）
1. **`RiskOutputService.findByTraceIdAndType`** — 统一入口守卫  
2. **`CashLoanCreditsService.insertOrUpdateRiskOutputResult`** — 风控输出落库主路径（直调 Model）  
3. **`LoanUserRiskTraceService`** — `findLatestByTraceIdAndTypeVa
```

## 11. `ec/risk/5f87eec0b9.md` · risk

- 来源：`ec` / `b0caa16c-8535-404b-8b93-2c4c96a98233`
- 符号：`RiskOutputService`, `CashLoanCreditsService`, `LoanUserRiskTraceService`, `CashLoanOrderAdditionalInfoService`, `LoanMarketUserQualifyService`, `NotifParamResolveService`, `FundRacService`

**decision 摘录：**

```
你说得对，这里需要把「改几处」和「为什么」分开说清楚。
## 不是只改了两处
Case 6 实际动了 **4 个 Service 文件**（外加撤销 Model 判空）：
| 文件 | 原因 |
|------|------|
| `RiskOutputService` | 已有包装方法，给**走 Service 的调用方**统一守卫 |
| `CashLoanCreditsService` | **直调 Model**，不走 `RiskOutputService` |
| `LoanUserRiskTraceService` | **直调 Model**（reapply 样本里 3 次告警的主路径之一） |
| `UserLevelAdRuleMethod` | **直调 Model**（internal-api 广告特征链路） |
如果你说的「俩地方」是指 Case 5 那种 **「包装 Service + 直调源头」** 的模式——Case 6 也是同一思路，只是直调 Model 的热点不止一个。
## 为什么不能只改 `RiskOutputService` 一处？
因为大量代码**绕过** `RiskOutputService`，直接调 Model：
```java
// CashLoanCreditsService — 风控结果落库，reapply 主路径
riskOutputResultModel.findByTraceIdAndType(traceId, type)
```

## 12. `ec/risk/ffdec6bfef.md` · risk

- 来源：`ec` / `a1c8081e-d8b1-495e-8a7a-11240e8f476c`
- 符号：`GopayController`, `IndosatH5Controller`, `KreditListController`, `ApiChannelCollisionController`, `IEcApiChannelCollisionService`, `ApiChannelService`, `ApiMarketingUserCheckResultService`

**decision 摘录：**

```
全量扫完了。可以确认。
---
**是的，之前的汇总是完整的。** 以下是核验结论：
**涉及两张表的所有模块**（逐一排查）：
| 模块 | 是否涉及 | 备注 |
|---|---|---|
| `ec-core` | ✅ | 所有 Model/Service 读写，已全部分析 |
| `ec-api` | ✅ | `GopayController`（U3）、`IndosatH5Controller`（U2-b）、`KreditListController`（仅写入）|
| `ec-internal-api` | ✅ | `ApiChannelCollisionController`（M2）|
| `ec-client-spring` | ✅ | `IEcApiChannelCollisionService`（M2 的对外接口声明）|
| `ec-scheduler` | ✅ 无 | 定时任务模块完全不涉及这两张表 |
| `ec-kafka-consumer` | ✅ 无 | Kafka 消费者模块完全不涉及 |
| `ec-admin` | ✅ 无 | Admin 后台完全不涉及 |
| `ec-stat-scheduler` | ✅ 无 | 统计调度不涉及 |
**两个补充说明**（之前汇总没有专门列出的安全调用）：
1. `ApiChannelService.findListByMobileNumberMd5AndIdentityNumberMd5AndResultI
```
