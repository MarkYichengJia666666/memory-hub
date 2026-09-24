# Memory 有用性验收 · 冒烟 4 题（2026-09-24）

> 衔接：训练/现码复核收尾后，用 **Claude Code × Memory Proxy**（空目录）验证「能用」。  
> 对照前一批：[`usefulness-batch-20260923.md`](./usefulness-batch-20260923.md)

## 环境

| 项 | 值 |
|---|---|
| 方式 | `ec-workbench/bin/claude-via-memory`，cwd=`/tmp/mem-proof`（无 EC 仓） |
| Proxy | `http://127.0.0.1:8096`（FULL_STACK，容器 healthy） |
| Team / Agent | `ec-memory-p0` / `ec-chat-memory`（`team-25ax2ljxko` / `agt-25ay1hg9ya`） |
| 约束 | 先 curl memory-bridge；勿搜代码仓；开头 `【来源：Memory】path=...` |
| 权限 | `bypassPermissions`（勿开 auto mode） |
| 库存参考 | Hub 业务 L2 ≈ 61；本地 active ≈ 55 / superseded ≈ 5 |
| EC 复核基线 | `d103aeefa32`（`release/20260922-5`） |

## 抽测结果（已跑 4/8）

| # | 问法要点 | 命中 path | 关键符号/要点 | 结果 |
|---|---|---|---|---|
| 1 | Saqu 绑卡对标哪套？方法名？ | `ec/bindcard/saqu-superbank-pattern.md` | `keepOnlySuperBankIfInExperiment`；并区分归因 L2 | **过** |
| 2 | MERGE_ vs MERGED_ 谁留存？ | `ec/auth/merge-user-id-vs-merged-user-id.md` | MERGE_=留存；insert 参数易误导 | **过** |
| 3 | H5 全流程为何不见「立即申请」？ | `ec/homepage/h5-whole-process-hides-main-button.md` | `showDownloadButton` / `isWholeProcess`；`MULTI_LOAN_INIT` 例外 | **过** |
| 6 | APP_STARTUP 是否 0 点 cron？ | `ec/kafka/app-startup-at-least-once-realtime.md` | 实时→Kafka→Processor；午夜=流量高峰 | **过** |

未跑（可选加强）：4 WEB 测额无 channel、5 Lazada 长拒下单仍拒、7 到期提醒 LRD、8 USER_REJECTED→1001。

## 观察

- 回答均带 `【来源：Memory】` + 明确 L2 path。
- 单题可在约 **10 秒内**出完整判决（例：H5 按钮题），符合「读 Memory 直接答」而非搜仓。
- 偶发引用相邻 L2 作边界澄清（Saqu 对标 vs 归因；H5 按钮 vs 老主卡），属加分。

## 结论

| 层级 | 标准 | 本轮 |
|---|---|---|
| 存 | L2 可写可读、现码可核 | 已完成（另见 `verify-and-extract-20260924.md`） |
| 核 | 过时条目 `superseded` / 错锚修正 | 已完成 |
| 用 | 空目录 + Memory 优先，关键符号命中 | **4/4 冒烟过线** |

**旁路主线收口：能存 → 能核 → 能用。**  
有用 = 短判决可召回并落到符号；不要求重讲背景，也不绑定 Capillary 删码。

## 建议日常

1. 对话拍板约束 → 写 `ec/<biz>/<slug>.md`（先落盘再 `scenario/write`）→ `active`
2. 代码变更后抽核；失效标 `superseded`
3. 不再从旧 transcript 批量灌池；Capillary/合图另开线

## 相关产物

- 启动：`bin/claude-via-memory` · 开场白：`MEMORY-FIRST-PROMPT.md`
- 核写收尾：`train-durable-closeout-20260924.md` · `verify-and-extract-20260924.md`
- Playbook：`../../docs/CHAT_MEMORY_EC_PLAYBOOK.md`
