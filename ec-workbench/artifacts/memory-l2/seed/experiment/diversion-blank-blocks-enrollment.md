# 实验入组率：泳道空白在挡人

## decision
入组率只有六成左右时，**不要先怀疑 33/33/33 没分满**。
常见根因是合格用户被 **泳道空白**挡住：日志里大量 `DIVERSION_BLANK_GROUP` / `PARENT_DIVERSION_BLANK_GROUP` / `BLANK_GROUP`，字面也不是简单的 `EXPERIMENT_NOT_IN`。
入组率公式可以没问题，失败归因要先拆空白组占比。

## mouths
- experiment
- kafka-consumer（分流日志）

## anchors
- group labels: `DIVERSION_BLANK_GROUP`, `PARENT_DIVERSION_BLANK_GROUP`, `BLANK_GROUP`
- related: `EXPERIMENT_NOT_IN`（勿与空白混淆）

## constraints
- 分析入组先按 expGroup 字面量拆桶
- 空白组占比高时先查泳道/父实验空白配置

## evidence
- Cursor×ec · transcript `0a2722a5`

## status
active

## updated
2026-09-24
