# Chat Memory 蒸馏批次 · 2026-09-28（覆盖对话 2026-08-01～09-28）

范围：本机 `idea_file2/ec` + `ec-01` agent-transcripts；只收可复用业务判决；跳过 `/reflect`、verify 编排、JRebel/harness 过程向。

## 本批新增 seed（13）

| 业务口 | slug | workspace / 主题 |
|--------|------|------------------|
| homepage | reborrow-date-plus-one-display-only | TAPD-1371851 |
| homepage | auto-jump-exemption-repay-precheck-not-list | TAPD-375653 / 365607 |
| order | post-confirm-borrow-more-cap-truncate | TAPD-371433 |
| order | first-loan-borrow-more-delta-asc-coupon | TAPD-367267 |
| order | order-back-boost-coupon-scope | TAPD-368547 |
| order | h5-api-protocol-preview-use-channel-config | TAPD-1355523 |
| coupon | erank-openapp-grant-bypass-global-cache | TAPD-1371553 |
| coupon | enrollment-not-equals-grant | TAPD-374680 / 377551 |
| coupon | retain-grant-idempotent-lock-not-db-unique | TAPD-365113 |
| marketing | qian1-postauth-must-not-exclude-wany-by-rate-only | TAPD-1369319 |
| marketing | zero360-rate-gate-green-card-not-loaninfo | TAPD-378150 |
| living | privy-liveness-trust-client-loan-auth-only | TAPD-373401 / 1373384 |
| living | living-diversion-shadow-keep-low-build-facepp | living diversion |

## 同窗口已有（此前蒸馏，未重复写）

含 378150/378872/369579/374680 holdout / resolveNextCoupon / deductLimitTerms / zero360 scene·mock·免息天 等，见 `seed/{homepage,order,marketing,coupon}/`。

## Claude Code 增补（`~/.claude/projects/…-ec` / `…-ec-01`，8/1～今）

| 业务口 | slug | 来源 session |
|--------|------|----------------|
| coupon | historical-avg-activation-single-product | `66965bd3` / `48db0c77` |
| coupon | second-order-must-keep-passes-holdout | `c74b5a96` |
| coupon | openapp-grant-swimlane-kafka-shared-group | `f0d3df11` |
| living | privy-upload-images-are-qiniu-keys | `718f1d79` / `5dad8f7a` |
| living | privy-request-hide-images-in-log | `0bf9076c` |
| living | supplement-liveness-not-write-auth-master | `45e09cda` / `53572b99` |
| marketing | zero360-lottery-claim-api-means-granted | `5ff363da` |
| marketing | appresource-strategy-empty-payload-full-check | `26138b86` |
| auth | exception-advice-registration-oracle | `b7220538` |

证据字段写 `Claude×ec · session \`…\``（与 Cursor transcript 区分）。

## 扫描未成条（刻意跳过）

- 纯 SDD reflect / verify / implement-loop / test-harness 文档改造
- Cursor 侧 TAPD-377605 前端「不要 mock」过程向（Claude specify 已另成条）
- 单次泳道账号排障无符号结论
- Claude 纯你好 / 上下文压缩吐槽 / skill 安装
