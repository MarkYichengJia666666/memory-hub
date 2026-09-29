# 0息360 登录轮播走 LoginBannerService，不走资源位 Filter

## decision
TAPD-378150 登录页轮播替换（US5）应接在 `POST /api/user/marketing/loginBanner` → `LoginBannerService#buildBanner`，**不要**挂 `Zero360AcquisitionFilterRuleProcessor` 的 `LOGIN_PAGE` 场景，也不要用 `Zero360LoginBannerReplacedCache` 做 Redis 频控。
流程：一次 `MaterialTagResolver` 拉 ads 标签 → 优先级 **0息 → 万一 → 千一**（互斥）→ 再按标签做实验/门控；置首一张图用配置 `marketing.login_banner_zero360_image`，不是 MC 整组替换。

## workspace
- `TAPD-378150-zero-interest-360-lottery`（与 EC `specs/<workspace>` 同名）

## mouths
- 0息360 / zero360
- 登录页轮播 / loginBanner

## anchors
- symbols: `LoginBannerService#buildBanner`, `UserController#loginBanner`, `Zero360AcquisitionFilterRuleProcessor`（登录页 intentionally 不接）
- routes: `POST /api/user/marketing/loginBanner`
- config: `marketing.login_banner_zero360_image`

## constraints
- 勿把 US5 接到 `GeneralPageConfigFilterRuleType` 登录页 Filter
- 勿为「只换一张轮播图」单独上 Redis 永久频控 cache

## evidence
- Cursor×ec · transcript `cb302df1`

## status
active

## updated
2026-09-28
