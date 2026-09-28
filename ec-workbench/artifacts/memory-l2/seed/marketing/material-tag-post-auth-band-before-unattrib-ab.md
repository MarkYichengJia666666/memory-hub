# 完件后 MaterialTag：先利率落带，未归因实验一后打

## decision
`MaterialTagResolver#resolveForPostAuth` 对首贷可下单用户：**先**用券后最低真实日息落带（千一档优先）→ 再 `classifyMaterialTag`；**千一档已命中时不调用**未归因实验一，避免未归因用户先进实验样本再被千一盖掉。
万一带内：原生万一素材 → `hitTag=WANY` 走现网门控；万一带 + 非原生万一 + 实验一组 → `ofPostAuthAttributedByUnattribExp`（本期出料，不依赖现网万一对照/空白）。
出料侧 `WanyEligibility#shouldShowPostAuth` 以 `resolveForPostAuth` 的 `hitTag==WANY` 为准（含 attributed 路径）。

## workspace
- `TAPD-378872-unattrib-wany-fallback`（与 EC `specs/<workspace>` 同名）

## mouths
- 未归因万一 / 完件后
- 利率带 / 千一万一免息

## anchors
- symbols: `MaterialTagResolver#resolveForPostAuth`, `MaterialTagResolver#resolvePostAuthHitTag`, `MaterialTagResolver#isInQian1Band`, `MaterialTagResolver#isInWanyBand`, `WanyEligibility#shouldShowPostAuth`
- VO: `MaterialTagHitVO.attributedByUnattribExp`
- 日息: `Qian1RealRateResolver` / `dailyRateAfterTotalDeductAmount` 最小值

## constraints
- 勿在千一档用户上先 `abTest` 再落带（样本污染）
- 勿假定「日息落万一带」 alone 等于「本期万一」；本期需 attributed 或未归因支路
- 完件后 0 息承接若关闭，`ZERO_INTEREST` hit 仍可能由 L1 返回但 Eligibility 不出料

## evidence
- Cursor×ec-01 · transcript `f06ce5e0`

## status
active

## updated
2026-09-28
