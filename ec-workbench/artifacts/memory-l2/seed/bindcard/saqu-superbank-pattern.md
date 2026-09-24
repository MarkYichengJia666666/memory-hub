# Saqu 绑卡对标 SuperBank 模式

## decision
Saqu「只能绑自家卡 + 可开关」可直接对标已落地的 SuperBank 渠道逻辑，H5（WEB）与同一套接口。

## mouths
- ec-api
- outside-web / H5

## anchors
- routes: `GET /api/idn/cashloan/getSupportedBank`
- symbols:
  - `LoanBankAccountService.keepOnlySuperBankIfInExperiment`
  - `LoanBankAccountController`
  - `ExperimentNameSpace.SUPERBANK_CHANNEL_USER_BINDING_CARD`（对标实验枚举）
- 行为要点：deviceId→mediaSource 白名单 → 命中实验才过滤 → 仅保留 `BankType.FAMA`（SuperBank）

## constraints
- 不要另起一套过滤；复用/对标 `keepOnlySuperBankIfInExperiment` 模式
- 开关要能随时切「只自家卡 / 全部卡」

## evidence
- Cursor×ec 对话（Saqu 需求 vs SuperBank 关键字）2026-08-26
- transcript `8a9e3015-a628-4453-95eb-027a69480838`

## status
active

## updated
2026-09-23
