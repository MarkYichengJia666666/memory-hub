# enrichNikFallback：NIK 回退扇出慢查询

## decision
API 渠道撞库手机号 MD5 未命中时，会走 `enrichNikFallback` 按身份证 MD5 回退，可能一次命中**数千**用户，再经 merge 过滤后对 `findLatestRiskAcceptByAccountIds` 打慢查询。
热点 NIK 常是库内脏数据聚合；近观测量里实际打到该链路的线上请求多见 Akulaku。
排障 qualification/checkUser 慢时，先看是否「手机 0 命中 → NIK 回退海量 ID」，不要只盯单条 SQL。

## mouths
- apichannel / akulaku
- 慢查询 / NIK

## anchors
- symbols: `ApiChannelUserCheckerService`, `enrichNikFallback`, `ApiChannelUserMergeFilterService`, `findLatestRiskAcceptByAccountIds`
- tables: `loan_user_encrypt_info`, `loan_user_risk_trace`

## constraints
- 慢查询根因可能在 NIK 扇出，不在单 account 正常路径
- 脏 NIK 聚合 ≠ 某渠道专属问题

## evidence
- Cursor×ec · transcript `ba304bcc`

## status
active

## updated
2026-09-24
