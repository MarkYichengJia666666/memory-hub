# Flip H5 白名单：在贷续借 ≠ 结清后复贷

## decision
`homepage_v5_config.web_channel_revolving_whitelist` 对 Flip/H5 开放的是**在贷续借**主按钮路径（`MultiLoanProductProcessor`），不是「结清后再借」。
排数据时口径要用 `user.CHANNEL in (web_Flip, Flip)` **且** 订单 `SOURCE_TYPE=WEB`；仅 `CHANNEL=web_Flip` 但 `SOURCE_TYPE=ANDROID/IOS` 是 **App 端**，不要算进 Flip H5 续借。

## mouths
- Flip / H5
- 续借 / homepage

## anchors
- symbols: `MultiLoanProductProcessor`, `homepage_v5_config.web_channel_revolving_whitelist`
- channels: `web_Flip`, `Flip`
- filter: `SOURCE_TYPE=WEB`

## constraints
- 白名单 channel 须与请求头精确匹配（`Flip` ≠ `web_Flip`）
- 结清后复贷成功 ≠ 在贷续借白名单已验证

## evidence
- Cursor×ec · transcript `15f64268`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
