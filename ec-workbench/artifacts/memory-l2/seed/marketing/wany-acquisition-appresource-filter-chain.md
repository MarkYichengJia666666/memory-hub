# 万一资源位唯一 Filter 链路与 MC 回调 internal-api

## decision
万一承接在资源位侧**只有一条** Filter 判定点：`WanyAcquisitionFilterRuleProcessor`（规则码 `WANY_ACQUISITION` / `WYACQ`），内部调 `WanyEligibility#shouldShowPreAuth` / `shouldShowPostAuth`。
HTTP 入口是 `POST /api/appResourceV2`（ec-api），无单独「万一资源位接口」；MC 按素材策略挂载 Filter，展示资格判定常经 MC 回调 **ec-internal-api** 的 `/ecInternalApi/checkStrategy` 执行，ec-api 响应里 `userConfigHit is false` 需到 internal-api 日志查 `missReason`。

## workspace
- `TAPD-378872-unattrib-wany-fallback`（与 EC `specs/<workspace>` 同名）

## mouths
- 万一承接 / WANY
- 资源位 / appResourceV2

## anchors
- symbols: `WanyAcquisitionFilterRuleProcessor`, `WanyEligibility`, `GeneralPageConfigFilterRuleType.WANY_ACQUISITION`
- routes: `POST /api/appResourceV2`, `/ecInternalApi/checkStrategy`
- MC payload: `scene`（`UNFINISHED` / `CAN_ORDER_FIRST_LOAN`）、`crowd`、`requirePrizeParams`

## constraints
- 排查「664/完件 banner 不出」不要只盯 ec-api；先看 internal-api 规则命中与实验 key
- 转盘低日息动参走 `WanyWheelParamResolveService`，与 Filter 人群/奖参 OR 关系需与 Processor 一致

## evidence
- Cursor×ec-01 · transcript `bd216c70`
- Cursor×ec-01 · transcript `9358277a`

## status
active

## updated
2026-09-28
