# 账号找回：防护 DWLog 失败不影响主流程

## decision
`RecoverAccountProtectBlockLogger#safeLog` 全包 try/catch，DwLog 失败只 warn，**不向外抛**。找回限流/错误码/文案语义不变；正常成功路径不打点。
改动性质是「拦截现场加埋点」，不是改找回业务。相对风险点：锁定期再验卡时的 Redis 计数写（`incrementBlockAfterLimitCount`）若不包住，理论上可能让 soft fail 变硬错——与 safeLog 本身无关。

## mouths
- auth / 账号找回
- 埋点

## anchors
- symbols: `RecoverAccountProtectBlockLogger#safeLog`, `incrementBlockAfterLimitCount`
- log: `LogBusinessType.RECOVER_ACCOUNT_PROTECT_BLOCK_LOG`

## constraints
- 不要因加了 DWLog 就怀疑找回主流程被改坏
- 加固计数写应另包 try/catch，勿与 safeLog 混淆

## evidence
- Cursor×ec-1 · transcript `1eed3344`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
