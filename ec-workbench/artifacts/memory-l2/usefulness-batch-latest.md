# Memory 有用性验收 · Proxy+L2 · 20260930-113055

model=`deepseek-v4-flash-0731` · **8/8**

scenario/read 期望 path → Proxy 作答 → must_hit 全中且标明来源 Memory

| # | id | 结果 | 命中 | hub |
|---:|---|---|---|---|
| 1 | Q1-saqu | **过** | keepOnlySuperBankIfInExperiment | `ec/bindcard/saqu-superbank-pattern.md` |
| 2 | Q2-merge | **过** | MERGE_USER_ID,MERGED | `ec/auth/merge-user-id-vs-merged-user-id.md` |
| 3 | Q3-h5 | **过** | showDownloadButton,isWholeProcess | `ec/homepage/h5-whole-process-hides-main-button.md` |
| 4 | Q4-web-calc | **过** | WEB,channel | `ec/risk/web-calc-credits-no-channel.md` |
| 5 | Q5-lazada-reject | **过** | LazadaCreateOrderChecker,LAZADA_WITHIN_REJECTION_PERIOD | `ec/apichannel/lazada-first-reject-long-can-retry.md` |
| 6 | Q6-startup | **过** | Kafka | `ec/kafka/app-startup-at-least-once-realtime.md` |
| 7 | Q7-lrd | **过** | LRD,LoanRepaymentDueSelector | `ec/notif/repay-due-lrd-selector-chain.md` |
| 8 | Q8-1001 | **过** | 1001,GopayController | `ec/gopay/api-channel-rejected-map-1001.md` |

## 答摘录

### 1. Q1-saqu · 过
问：Saqu「只能绑自家卡 + 要可开关」，应对标哪套已有过滤逻辑？关键方法名？
```
【来源：Memory】path=ec/bindcard/saqu-superbank-pattern.md

Saqu「只能绑自家卡 + 可开关」应对标已落地的 **SuperBank 渠道逻辑**，H5（WEB）与同一套接口。

关键方法名：`LoanBankAccountService.keepOnlySuperBankIfInExperiment`

行为要点：
- deviceId → mediaSource 白名单 → 命中实验才过滤 → 仅保留 `BankType.FAMA`（SuperBank）

约束：不要另起一套过滤，直接复用/对标该模式；开关要能随时切「只自家卡 / 全部卡」。
```

### 2. Q2-merge · 过
问：user_merge_history 里 MERGE_USER_ID 与 MERGED_USER_ID，谁是留存用户？
```
【来源：Memory】path=ec/auth/merge-user-id-vs-merged-user-id.md

`MERGE_USER_ID` = **合并后留存可用账号**；`MERGED_USER_ID` = **被注销废弃账号**。

注意 `UserMergeHistoryModel.insert(newUserId, oldUserId, …)` 参数名易误导：实际 `setMergeUserId(oldUserId)`、`setMergedUserId(newUserId)`——调用方传入的「被删」常落在第一参数。查合并关系先认字段语义，不要按参数名字面理解。
```

### 3. Q3-h5 · 过
问：H5 全流程为什么经常看不到「立即申请」主按钮？关键判断？
```
【来源：Memory】path=ec/homepage/h5-whole-process-hides-main-button.md

H5 全流程看不到「立即申请」主按钮，关键判断是：

**MAIN_BUTTON 是否展示统一受 `HomepageWholeProcessTool#showDownloadButton()` 控制**——H5 全流程（`isWholeProcess=true`）直接返回 false，不展示按钮；App / 非全流程 H5 才展示。

排查时先查 `showDownloadButton`，不要先怪产品 Processor（如 `MultiLoanProductProcessor`、`OrderProgressStepProcessor`）。

注意例外：`MULTI_LOAN_INIT`（可续借测算）仍走老的 `HomePageMainCardInfo`，不
```

### 4. Q4-web-calc · 过
问：WEB 测额 / 额度过期重新测额的风控 trace 有没有 flip/ovo 等细分 channel？
```
【来源：Memory】path=ec/risk/web-calc-credits-no-channel.md

没有。WEB 下「戳额 / 额度过期重新测额」的风控 trace **没有 flip/ovo 等细分 channel**，只记到 `source_type = WEB`。

原因：入口 `LoanAccountService.submitCalcCreditsApplication` → `SubmitCalcCreditsApplicationVO` **无 channel 字段**，`RiskProcessParam.fromForCalcCredits()` 也就带不进细分渠道。

查渠道应看 `user.channel` / `loan_account_details_new.channel` 做归因推断，不要指望本次测额 risk_trace 精确到页面渠道。
```

### 5. Q5-lazada-reject · 过
问：Lazada 首贷长拒可重审（FIRST_LOAN_REJECT_LONG_CAN_RETRY）下单撞库还会不会拒？
```
【来源：Memory】path=ec/apichannel/lazada-first-reject-long-can-retry.md

会拒。

按该 L2 场景块的记录：

- **前筛 / 二次撞库侧**（`LazadaPreSelectChecker` / `LazadaSecondPreSelectChecker`）：可映射 **`LAZADA_APPROVE`**，分组 **`LAPSED_USER`**，即允许重回授信审批。
- **下单撞库侧**（`LazadaCreateOrderChecker`）：仍映射 **`LAZADA_WITHIN_REJECTION_PERIOD`（拒）**。

也就是说：`FIRST_LOAN_REJECT_LONG_CAN_RETRY` 只放开到**二次撞库**，**未过审不能直接借款**——下单撞库依然会拒。改规则时需按 Checker 
```

### 6. Q6-startup · 过
问：APP_STARTUP_AT_LEAST_ONCE 是不是每天 0 点 cron 批量跑？
```
【来源：Memory】path=ec/kafka/app-startup-at-least-once-realtime.md

不是。代码里**不存在**「每天 00:00 cron 批量扫用户」的 Job。

`APP_STARTUP_AT_LEAST_ONCE` 是 **C 端实时行为 → outbox/Kafka → 异步消费**链路。午夜看起来像批处理，只是因为午夜 C 端流量高峰。

触发入口包括：
- `openApp`（`RealTimeEventController#openApp`）
- 首页 `asyncLogUserInfo`（`LoanHomePageLogService#asyncLogUserInfo`）
- `app/startup` → `AppStartupService.sendAppStartupEvent` → topic `app_startup_
```

### 7. Q7-lrd · 过
问：贷款到期还款提醒带账单日参数时，常见对应哪个 Selector / queryType？
```
【来源：Memory】path=ec/notif/repay-due-lrd-selector-chain.md

贷款到期还款提醒带账单日参数 `INSTALMENT_BILLING_DATE` 时，最常见对应 **`LoanRepaymentDueSelector`（queryType=`LRD`）**；人维度则可能是 `RepaymentReminderInPersonDimensionSelector`（`RRIPD`）。

链路：`ec-scheduler` 的 `TargetTaskProducerJob` 按后台 Selector 配置筛用户 → 写 notif 任务 → Consumer 填模板发送。查配置看 Admin「通知用户 Selector」或表 `notif_user_selector`。

排「谁发的到期提醒」时先对 Selector/queryType，不要只
```

### 8. Q8-1001 · 过
问：Gopay 遇到 API_CHANNEL_USER_REJECTED 应对前端映成什么？现码落在哪？
```
【来源：Memory】path=ec/gopay/api-channel-rejected-map-1001.md

`API_CHANNEL_USER_REJECTED` 对前端应映射为 `COMMON_SERVER_ERROR(1001)`，避免英文 `detailTT` 直出。

**现码落地位置**：已在 `GopayController`（含 pollAuthStatus 相关 catch）落地——命中该异常类型则 `throw EcException.error(COMMON_SERVER_ERROR, …)`。`EcControllerExceptionAdvice` 全局转换**尚未**见到。

排障/改文案先看 `GopayController`；若要全渠道覆盖再考虑 Advice。注意：其它渠道若仍直出 REJECTED 英文，不要假设全局已映射；ERROR 级还会被 
```

