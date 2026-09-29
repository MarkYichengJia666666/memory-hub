# 端外协议预览：按 H5/API 渠道取 ForH5 配置，勿发 App 协议

## decision
API/H5 全流程下单页预览协议报错的根因：月收入动态声明把协议从首页迁到 `productDetail` 后，端外仍走 App 协议配置。修复口径：按 `isApiChannel` / `isH5WholeProcess` 分流到 `*ForH5`（或等价端外）配置。
排障区分：协议 **key 错域名/打不开** = 配置 map 选错；key 已是正确枚举但 detail 提示重新登录 = 鉴权/token，不是协议配置问题。

## workspace
- （缺陷 TAPD-1355523 / bug 1153182677001076228；协议渠道配置）

## mouths
- 协议预览 / productDetail
- H5 全流程 / API 渠道

## anchors
- flags: `isApiChannel`, `isH5WholeProcess`
- page: `productDetail` 协议下发

## constraints
- 端外回归不要只测 App 包协议 key
- 监控协议访问异常时用 key/detail 二分配置 vs 鉴权

## evidence
- Cursor×ec-01 · transcript `702d5c8e`

## status
active

## updated
2026-09-28
