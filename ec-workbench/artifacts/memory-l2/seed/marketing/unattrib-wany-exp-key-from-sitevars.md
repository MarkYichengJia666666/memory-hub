# 未归因万一实验 Key 必须走 SiteVars，禁止只读枚举占位

## decision
完件前/后未归因万一分流用的实验 Key，运行时必须以 `UnattribWanyAcquisitionConfig#getExpKey()`（SiteVars `marketing.unattrib_wany_exp_key`）为准；**不能**在 `MaterialTagResolver` 等处直读 `UserFlowExperimentEnum` 占位常量，否则配置中心换 Key 或配空关停对 L1 不生效，实验平台报「实验不存在」时用户会停在 `hitTag=NONE`，资源位 `NOT_LANDING_USER`。
Key 配空串等价于关停：不发起 `abTestByUserId`。

## workspace
- `TAPD-378872-unattrib-wany-fallback`（与 EC `specs/<workspace>` 同名）

## mouths
- 未归因万一 / TAPD-378872
- 实验 Key / SiteVars

## anchors
- symbols: `MaterialTagResolver#isUnattribWanyExpGroup`, `UnattribWanyAcquisitionConfig#getExpKey`, `WanyWheelParamResolveService`
- config: `marketing.unattrib_wany_exp_key`, `marketing.unattrib_wany_login_banner_exp_key`
- enums: `UserFlowExperimentEnum.UNATTRIB_WANY_ACQUISITION`（默认值，可被覆盖）

## constraints
- 联调「素材配置都对但不出万一」先查 RPC 的 `expKey` 是否为 `TBD_*` 或配置未生效
- 枚举改正式 Key 后仍要保证 L1 与 `UnattribWanyEligibility` 读同一配置源

## evidence
- Cursor×ec-01 · transcript `9358277a`

## status
active

## updated
2026-09-28
