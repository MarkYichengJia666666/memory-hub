# 跨端登录 & 下单规则（H5 / APP / 渠道）

## decision
跨端行为不能只看代码分支，要同时看**线上开关**和**账号 sourceType**。
- 登录：`user.h5_user_allow_login_switch=true` 时 APP 注册用户可登 H5；`sourceType` 是账号维度不是请求维度。
- 下单：渠道侧有未还清单（INIT/READY）→ 端内不能再下；渠道单已还清但渠道在 `api_channel.not_allow_reloan_channel_list`（线上目前主要是 `INDOSAT_CL2`）→ 禁二次放款；其他渠道已还清一般可下。

## mouths
- ec-api / H5
- api-channel

## anchors
- config: `user.h5_user_allow_login_switch`
- config: `api_channel.not_allow_reloan_channel_list`（线上值常见 `INDOSAT_CL2`）
- symbols: `UserService`, `CashLoanService`, `ApiChannelOrderCheckService`, `assertUserAllOrderFinished`

## constraints
- 不查配置就下「APP 登 H5 会被拦截」之类结论会错
- 复贷 vs 续借跨端差异要分开看，不要混成一条规则

## evidence
- Cursor×ec-1 · transcript `b8b6287d`（跨端登录&下单规则沉淀）

## status
active

## updated
2026-09-24
