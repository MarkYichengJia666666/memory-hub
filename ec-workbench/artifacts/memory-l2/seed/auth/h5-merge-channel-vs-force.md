# 账户合并：H5 更常走 CHANNEL_MERGE

## decision
H5 全流程与 App **共用**合并核心（`changeBindCheckRewrite` → `doMergeAccount`）和同一套 check/confirm 接口，**没有单独合并实现**。
差别在触发与 confirm 分支：H5 常通过 `generalConfig` 的 `channel_merge_check` 预检；confirm 时若 `idNo`/`userName` 为空，`resolveMergeType` 会判成 **`CHANNEL_MERGE`**（App 有证件时常是 `FORCE_MERGE`）。WEB 也不受 `notReachForceMergeBuild` 限制。

## mouths
- auth / 账户合并
- H5 / Flip

## anchors
- routes: `GET /api/user/forceMergeAccount/check`, `POST /api/user/forceMergeAccount/confirm`, `GET /api/generalConfig?keys=app_config.channel_merge_check`
- symbols: `UserController#resolveMergeType`, `doMergeAccount`, `ChannelMergeCheckProvider`
- merge types: `FORCE_MERGE`, `CHANNEL_MERGE`

## constraints
- 不要假设 H5 另有一套合并后端
- 排障 H5 合并先看 confirm 是否空证件 → CHANNEL_MERGE

## evidence
- Cursor×ec · transcript `9961c484`

## status
active

## updated
2026-09-24
