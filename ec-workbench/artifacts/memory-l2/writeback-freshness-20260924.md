# 半自动回写 + 保鲜抽检 · 2026-09-24

> EC 对码：`d103aeefa32 Merge branch 'release/20260922-5'`

## A. 回写闭环（答完 → 记）

| 步骤 | 结果 |
|---|---|
| 起草 seed | `artifacts/memory-l2/seed/ops/session-closeout-writeback.md` |
| 人闸规则 | Skill「会话收尾」：禁止静默直写 Hub |
| 导入 | `./bin/seed-l2-to-hub` → `ec/ops/session-closeout-writeback.md` |
| `scenario/write` + `read` | ✅ content 可读（summary=会话收尾：新判决半自动回写） |
| L0 import | ✅ `imported=true`（需 `x-tdai-service-id: default`） |

**证明**：用完之后能按流程变成下一条记忆（半自动，人闸）。

## B. 保鲜抽检（10 条 active）

对 EC 现码按 `anchors` 符号搜类/方法（抽样跨 biz）：

| seed | 结果 |
|---|---|
| abtest/sub-abtest-not-deadloop | ok（命中 RealTimeEventController / OpenAppGrantCouponService） |
| apichannel/collision-orchestrator-decision-tree-only | ok |
| apichannel/collision-process-is-new-flow | ok |
| apichannel/context-channel-unreliable | ok |
| apichannel/enrich-nik-fallback-fanout | ok |
| apichannel/gopay-submit-renamed-decision-tree | ok |
| auth/device-token-mismatch-risk | ok |
| bindcard/saqu-no-dedicated-attribution | ok |
| config/downstream-repay-sync-needs-restart | ok |
| coupon/orthogonal-anti-settle-exp | ok |

**本批**：10/10 符号仍在；无新增 superseded。  
（历史已 supersede 例：`apichannel/collision-upgrade-fallback.md` — `executeUpgradeWithFallback` 已不存在。）

## C. 负例清单（验收用，不造假答）

| # | 问法 | 期望 |
|---|---|---|
| N1 | 问库里没有的冷门题（如某已下线且无 L2 的接口） | 不编「Memory 说…」；应查代码/说未知 |
| N2 | 问 `executeUpgradeWithFallback` 还在不在 | 应落到 superseded 条或现码结论「不在」，不当真理 |
| N3 | 把「Saqu 绑卡」问成「开屏发券怎么过滤银行」 | 不串台；发券走 Kafka 判决 ≠ 绑卡 SuperBank |

跑负例时用空目录 `claude-via-memory`；本文件只固化清单与期望。

## D. 文档 / 工具落点

- Skill / Playbook：收尾、保鲜、负例三节
- 导入脚本：`ec-workbench/bin/seed-l2-to-hub`
- 新 L2：`ec/ops/session-closeout-writeback.md`
