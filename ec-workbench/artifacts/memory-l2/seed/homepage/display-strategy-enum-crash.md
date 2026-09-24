# 首页：HomeDisplayStrategy 枚举缺失崩

## decision
首页组装若把实验 `groupType`/策略名反序列成 `HomeDisplayStrategy`，而枚举**没有**对应常量（例如 `EXPERIMENT_GROUP`），会抛 `IllegalArgumentException: No enum constant ...HomeDisplayStrategy.EXPERIMENT_GROUP`，常见于复贷拒绝等状态用户进首页。
根因是**枚举与实验/策略产出值不同步**，不是单纯展示文案问题。

## mouths
- homepage / ec-api

## anchors
- symbols: `HomeDisplayStrategy`, `HomepageContentTool`, `HomePageResponseBuildTool`
- related: `RejectUserinfoProcessor`, `LoanMarketV3EntranceDisplayStrategy`
- example missing: `EXPERIMENT_GROUP`

## constraints
- 新增实验组展示策略必须先补枚举再放量
- 排障首页 5xx 先搜 `No enum constant ...HomeDisplayStrategy`

## evidence
- Cursor×ec · transcript `4b26920e`

## status
active

## updated
2026-09-24
