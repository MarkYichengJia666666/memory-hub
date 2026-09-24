# 全量提炼 + 现码复核 · 2026-09-24

对照 EC：`d103aeefa32 Merge branch 'release/20260922-5'`

## 做了什么

1. 扫描 ec + ec-1 transcript：**461** 会话文件 → 判决候选 **223**（去重后）
2. 对照现码符号/实现复核既有 L2
3. **修正/归档** 过时条目；**新增** 现码核过的判决
4. 同步写入 Memory Hub L2（active 附带 L0）

## 结果概览

| 项 | 数量 |
|---|---:|
| Hub 业务 L2 | 51 |
| 本地 active | 45 |
| 本地 superseded | 5 |
| 本轮修正/归档 | 6 |
| 本轮新增（已核） | 6 |

## 本轮归档 / 修正

- `ec/apichannel/no-order-after-reloan-legacy-bean.md` → **superseded** (write_ok=True)
- `ec/apichannel/akulaku-npe-missing-channel-user.md` → **superseded** (write_ok=True)
- `ec/loanmarket/market-user-check-zombie.md` → **superseded** (write_ok=True)
- `ec/loanmarket/common-api-channel-zero-traffic.md` → **superseded** (write_ok=True)
- `ec/apichannel/collision-upgrade-fallback.md` → **superseded** (write_ok=True)
- `ec/gopay/api-channel-rejected-map-1001.md` → **active** (write_ok=True)

## 本轮新增（现码复核通过）

- `ec/auth/soft-delete-six-tables.md` (write_ok=True, L0=200)
- `ec/risk/multi-loan-normal-no-time-expire.md` (write_ok=True, L0=200)
- `ec/notif/ecresponse-error-hides-detailtt.md` (write_ok=True, L0=200)
- `ec/bindcard/saqu-no-dedicated-attribution.md` (write_ok=True, L0=200)
- `ec/config/request-channel-header-optional.md` (write_ok=True, L0=200)
- `ec/apichannel/collision-orchestrator-decision-tree-only.md` (write_ok=True, L0=200)

## 诚实边界

- **提炼**：来自历史对话结论，不是自动从代码生成
- **复核**：本轮对既有库做了符号/关键方法级核对，并对明确过时/错误条目做了 superseded 或改写
- **未完成**：223 候选里仍有大量「单次排障 / SDD 过程态 / 已覆盖近义」未全部写成 L2；后续可按 topic 继续核写
- 生产流量类结论（「零调用」）以对话当时日志为准，现码只能验证「代码还在不在」

## 产物

- `extract-full-20260924.json`
- `verify-existing-20260924.json`
- `verify-and-extract-20260924.json`
