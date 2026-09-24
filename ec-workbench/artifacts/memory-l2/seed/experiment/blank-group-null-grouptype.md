# 实验扩全 × 泳道空白组（groupType == null）

## decision
实验扩全到 FULL_PERCENTAGE 后，**不能默认空白组（groupType == null）行为不变**。
历史契约：`groupType == null` 表示用户未进实验分流语义；扩全后若代码/发券只认实验组布尔、丢掉 null 分支，会导致泳道空白组用户误发券等问题。
CloudDB / Memory 对此类问题的价值是记「历史契约」，不是事后 CR。

## mouths
- experiment
- coupon / grant

## anchors
- 概念：`groupType == null`（空白组）
- 相关：`fullOrZeroGroupResult` / `ExperimentAdapterResultVO` / `IExperimentAdapter` / `ExpClient`
- 复盘：飞书 Wiki「2026-08-09 实验扩全后泳道空白组用户发券」

## constraints
- 实验相关改动必须覆盖三态：RUNNING / FULL_PERCENTAGE / ZERO_PERCENTAGE，且验泳道空白组（groupType=null）返回值
- 不要假设「扩全 = 全员实验组语义」

## evidence
- Cursor×ec · transcript `3eca96c2` + 飞书复盘

## status
active

## updated
2026-09-24
