# 过件后千一：勿用「只看日息」的 NOT 规则误杀万一素材

## decision
过件后若千一口径**只看利率、不看素材标签**，低息用户会被当成千一，万一素材若再绑 `QIAN1_ACQUISITION_NOT`（`requireRealRate=true`）会被排除。联调/排障：不要给万一素材挂该 NOT 规则；过件后千一判定应补上素材标签条件，并与万一主 diff 分开提审（避免把口径修复并进承接大包）。deviceToken 须在万一 mock 白名单，否则先被千一链路接住。

## workspace
- `TAPD-1369319-wany-funnel-landing`

## mouths
- 千一 / 万一互斥
- 过件后资源位

## anchors
- rules: `QIAN1_ACQUISITION_NOT` / `requireRealRate`
- symbols: 千一 hit 判定（过件后）、`WanyAcquisition` Filter

## constraints
- 可下单号测万一：先确认 deviceToken 在 `wany_acquisition_mock_device_whitelist`
- 千一口径修复与万一承接主 diff 分开 land

## evidence
- Cursor×ec · transcript `43b4379d`

## status
active

## updated
2026-09-28
