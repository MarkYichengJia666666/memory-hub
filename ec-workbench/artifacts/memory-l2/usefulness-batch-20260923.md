# Memory 有用性验收 · 批量 3 条（2026-09-23）

- 身份：team `team-25ax2ljxko` / agent `agt-25ay1hg9ya` / task `task-3dg4w2ix5t`
- Proxy：`http://127.0.0.1:8096/proxy/default/v1/chat/completions` · model `deepseek-v4-flash-0731`
- 原始结果：`recall-batch-20260923.json`

## 抽测条目

| id | path | 问法要点 |
|---|---|---|
| saqu-superbank | `ec/bindcard/saqu-superbank-pattern.md` | Saqu 绑卡对标哪套？关键方法名？ |
| ec-secured-api | `ec/auth/ec-secured-api.md` | 鉴权注解名？登录前能否加注解？ |
| lazada-credit-push | `ec/order/lazada-credit-push-only-risk.md` | 额度变更是否都推？看哪条链路？ |

## 结果

| 层级 | 做法 | 结果 |
|---|---|---|
| 库存 | `scenario/read` | **3/3** 正文含 decision / anchors |
| 对话注入 | Proxy：L2 索引 → 模型调读场景 → 回填正文再答 | **3/3** 关键符号命中 |
| 零样本（无读全文） | 只靠注入索引直接答 | 易幻觉（会串到开屏 Kafka 等）——符合「L2 仅索引」设计 |

### Proxy 命中摘要

- **Saqu**：`keepOnlySuperBankIfInExperiment` + SuperBank/FAMA
- **鉴权**：`@ECSecuredApi`（非 `@SecureAPI`）+ 登录前勿乱加
- **Lazada**：并非所有额度变更都推 + `pushCreditModificationResultToApiChannel` / 风控结果链路

## 结论

换会话、不问人：L2 库存可读；经 Proxy 按设计「索引 → 读场景」后，三条判决都能答对。  
**有用 = 短判决可召回且能落到符号，不必重讲背景。**

说明：产品注入对 L2 只给索引；Agent 客户端需支持读场景工具（或先 L0 蒸馏 L1）才能零工具答准。本验收用两轮 tool-loop 模拟 Agent 读全文。
