# Memory 负例实跑 · N1–N3（2026-09-27）

> 清单来源：[`writeback-freshness-20260924.md`](./writeback-freshness-20260924.md) §C  
> 身份：`team-25ax2ljxko` / `agt-25ay1hg9ya` · Proxy `:8096` · model `deepseek-v4-flash-0731`  
> 原始：[`negative-batch-20260927.json`](./negative-batch-20260927.json)

## 通道

| 通道 | 结果 |
|---|---|
| 上午 | 上游 `all-in-one-ai` **502**；`claude -p` 卡住 |
| 复测 | 上游恢复；走 Proxy `/proxy/default` **工具环**（search / ls / read）**3/3 过** |

## 结果

| # | 问法 | 期望 | 结果 |
|---|---|---|---|
| N1 | 保险理赔 Kafka topic / Consumer | 无命中、不编 | **过** · 答「无命中 / 不知道」 |
| N2 | `executeUpgradeWithFallback` 还在否 | superseded / 现行 execute | **过** · 明确「已不在」+ 决策树 |
| N3 | 开屏发券×绑自家卡 | 不串台 | **过** · 发券≠绑卡过滤；Saqu 方法另条链路 |

## 抽样摘录

- **N1**：`【来源：Memory】无命中` + L2 无 `ec/insurance`，不给 topic/类名  
- **N2**：读到 `collision-orchestrator-decision-tree-only` / superseded 条，结论「不要再找 executeUpgradeWithFallback」  
- **N3**：开屏发券走 Kafka；`keepOnlySuperBankIfInExperiment` 属绑卡接口，不当发券答案  

**结论：正例会用 + 负例不瞎用，本轮模型对话也过线。**
