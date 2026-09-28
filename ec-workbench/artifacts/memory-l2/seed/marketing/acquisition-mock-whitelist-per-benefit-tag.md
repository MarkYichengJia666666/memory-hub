# 承接联调 mock 白名单按利益点分立，禁止串写千一 key

## decision
千一 / 万一 / 0息360 的 deviceToken mock 白名单是**不同配置 key**，联调伪造「利益点 → 某标签」时必须写对应 key：
- 万一：`marketing.wany_acquisition_mock_device_whitelist`
- 千一：`marketing.qian1_mock_device_whitelist`
- 0息侧用各自 0息/360 mock（若有），**禁止**为出 0息/万一去写千一白名单——会伪造千一标签并抢展示。
登录页万一轮播图走 `marketing.login_banner_wany_list`（命中后整组替换）；首页/完件资源位的 `redirectUrl`（如 `native/auth`、`native/subhome-loan`）是另一套，**不要**写进 `login_banner_wany_list`。实验 expKey 挂产品泳道即可，**不要**把承接 expKey 加进 API 渠道白名单。

## workspace
- `TAPD-1369319-wany-funnel-landing`
- `TAPD-378150-zero-interest-360-lottery`

## mouths
- 承接联调 / mock device
- 登录轮播 / 资源位

## anchors
- config: `marketing.wany_acquisition_mock_device_whitelist`, `marketing.qian1_mock_device_whitelist`, `marketing.login_banner_wany_list`
- symbols: `WanyAcquisitionService#isLoginPageHit`, `LoginBannerService`
- build: `marketing.wany_acquisition_min_build`（默认 Android ≥ 38600）

## constraints
- banner 不出：先查日志 `WanyAcquisition landing miss` / `[Zero360Acquisition] hitRule false` 的 reason，再查 MC
- 空的 `login_banner_wany_list` 会回落现网默认图，不是 Filter miss

## evidence
- Cursor×ec-01 · transcript `2ec8d40f`
- Cursor×ec · transcript `62632d23`
- Cursor×ec-01 · transcript `1c5db5d0`

## status
active

## updated
2026-09-28
