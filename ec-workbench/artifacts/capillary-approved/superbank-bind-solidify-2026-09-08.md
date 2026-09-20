# Capillary 全量固化计划 · superbank-bind

- 平台 key：`technology-auth-abroad-loan-superbank_rename_0908`
- grade：`candidate_solidify`
- safe_auto：False

## 目标

实验已 **FULL_PERCENTAGE**：去掉分流门闸，**保留实验组赢家行为**，
不要把整段业务当死代码删掉。

## 为什么要固化

- 平台状态=FULL_PERCENTAGE
- 流量：对照 0.0% / 实验 100.0% / 空白 0.0%
- 剩余约 0.0 天
- 描述：【鉴权】superbank改名&排位
- 已全量：建议 revision——去掉分流门闸，保留实验组赢家行为（需人确认；不是当死代码整枝删）
- Superbank 渠道绑卡改名&排位；代码经 ExpFacade + ExperimentNameSpace
- 固化保留：有效 mediaSource 下只保留 SuperBank(BankType.FAMA)； 去掉 isInSuperBankBindCardExperiment / ExpFacade 门闸后， 对该渠道直接执行 keepOnlySuperBank 过滤（原实验组 TRUE 行为）


## 建议保留的行为

- 有效 mediaSource 下只保留 SuperBank(BankType.FAMA)； 去掉 isInSuperBankBindCardExperiment / ExpFacade 门闸后， 对该渠道直接执行 keepOnlySuperBank 过滤（原实验组 TRUE 行为）


## 建议改动步骤

1. 找到分流调用（`ExpFacade` / `getResult` / `abTestByUserId` 等），确认赢家组取值。
2. 删除「是否进实验组」判断；直接执行原实验组分支。
3. 删除仅服务于分流的私有方法 / 枚举项 / ABTestConfig 版本表行（确认无其它引用）。
4. 更新/删除只测分流门闸的单测；补一条「固化后行为」单测。
5. 跑相关单测 → arc diff → Code Review。

## 涉及符号（图）

- `SUPERBANK_CHANNEL_USER_BINDING_CARD`
- `isInSuperBankBindCardExperiment`
- `keepOnlySuperBankIfInExperiment`

## 图查询摘要

- `search:SUPERBANK_CHANNEL_USER_BINDING_CARD`：**Search Results (1 found)**  **SUPERBANK_CHANNEL_USER_BINDING_CARD** (enum_member) ec-core/src/main/java/com/yqg/core/service/abtest/enums/ExperimentNameSpace.java:1366
- `callers:SUPERBANK_CHANNEL_USER_BINDING_CARD`：No callers found for "SUPERBANK_CHANNEL_USER_BINDING_CARD"
- `search:isInSuperBankBindCardExperiment`：**Search Results (1 found)**  **isInSuperBankBindCardExperiment** (method) ec-core/src/main/java/com/yqg/core/service/loan/bankaccount/LoanBankAccountService.java:1033 `boolean (Long userId, Long build)`
- `callers:isInSuperBankBindCardExperiment`：**Callers of isInSuperBankBindCardExperiment (1 found)**  - keepOnlySuperBankIfInExperiment (method) - ec-core/src/main/java/com/yqg/core/service/loan/bankaccount/LoanBankAccountService.java:986
- `search:keepOnlySuperBankIfInExperiment`：**Search Results (1 found)**  **keepOnlySuperBankIfInExperiment** (method) ec-core/src/main/java/com/yqg/core/service/loan/bankaccount/LoanBankAccountService.java:986 `List<BankConfigVO> (List<BankConfigVO> banks, Long userId, Long build, S
- `callers:keepOnlySuperBankIfInExperiment`：**Callers of keepOnlySuperBankIfInExperiment (1 found)**  - getIdnSupportedBankListWithLogo (method) - ec-api/src/main/java/com/miyou/controllers/cashloan/LoanBankAccountController.java:349

## 禁止

- 不要删赢家行为本身（例如 Superbank 只留 FAMA 的过滤逻辑）。
- 不要在未确认产运的情况下改其它渠道 / 白名单逻辑。
