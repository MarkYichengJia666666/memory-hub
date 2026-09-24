# 站内信脏模板：pandora 强制翻译同步

## decision
EC `mc_message_template` 出现「日期已展开 + needTranslate=TRUE」的脏模板时，**不要先怪运营在 MC 配错**。
已证实链路：MC 场景 `needTranslate=FALSE` 经 `mc-kafka-consumer` 正常；真正写脏模板的是 **`pandora-consumer`** 每天定时调用 `POST /ecInternalApi/messageCenter/sendMessage`，强制 `needTranslate=TRUE` 且参数为空。`operator_email=system@yangqianguan.com` 是代码写死，不代表真人。

## mouths
- notif / message-center
- pandora

## anchors
- routes: `POST /ecInternalApi/messageCenter/sendMessage`
- symbols: `MessageCenterService`
- services: `pandora-consumer`, `mc-kafka-consumer`
- table: `mc_message_template`

## constraints
- 排障先区分 MC 正常路 vs pandora 翻译同步路
- 不要把 system@ 当成运营误操作证据

## evidence
- Cursor×ec · transcript `f57df63c`

## status
active

## updated
2026-09-24
