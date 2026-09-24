---
name: chat-memory-ec
description: EC 旁路使用 Chat Memory——写判决、召回、Proxy 注入验收；不改 EC 主仓
triggers:
  - Chat Memory
  - EC 旁路记忆
  - 写 L2 判决
  - Memory 召回
  - 开屏灌券记忆
---

# Chat Memory × EC 旁路

把业务判决记在 Memory Hub Chat Memory 里，对着 EC 相关问题召回。**不改 EC 主仓。**

完整复现说明见 [`CHAT_MEMORY_EC_PLAYBOOK.md`](../../docs/CHAT_MEMORY_EC_PLAYBOOK.md)。

## 何时用

- 对话里刚拍板的约束/结论，要下次还能用
- 需要 mouths / 符号锚点，服务「对着代码干活」
- **不是** Wiki 长文；**不是** Capillary 删代码流程

## 身份（本机已验）

凭据文件（含 key，已 gitignore）：`ec-workbench/.p0-memory-identity.json`

| 项 | 值 |
|---|---|
| Panel | http://127.0.0.1:8125 |
| Core | http://127.0.0.1:8420 |
| Proxy | http://127.0.0.1:8096 |
| service_id | `default` |
| team | `ec-memory-p0` / `team-25ax2ljxko` |
| agent | `ec-chat-memory` / `agt-25ay1hg9ya` |
| 登录 | **user_key**（`sk-mem-…`），无账号密码 |

新建：meta `team/create` → `agent/create`（自动挂 `chat_memory`）→ `user-key/create`。

## 怎么写 L2

路径约定：`ec/<business>/<slug>.md`（路径即索引，L2 无关键字搜索）。

正文骨架：

```text
## decision
## mouths
## anchors（routes / symbols / topics）
## constraints
## evidence
## status   # active | superseded
```

样例（已验）：`ec/coupon/open-app-grant.md` — 发券在 Kafka，勿只改 Controller。

### 坑：`scenario/write` 不能新建

内核只更新已存在文件。新建流程：

1. 先落到  
   `/data/tdai-memory/profiles/{encodeURIComponent('team:'+team+'|agent:'+agent)}/scene_blocks/<path>`
2. 再 `POST /v3/scenario/write`（带 Bearer + 隔离三元组）同步 META / 索引

数据面需要：`Authorization: Bearer <gateway>`、`x-tdai-service-id`、body 里 `team_id`/`agent_id`/`user_id`。

## 会话收尾（半自动回写 · 人闸）

每次用 Memory 答完 / 挖出新结论，**必须**过一遍：

1. 有没有**新判决**？有 → 按骨架起草到 `artifacts/memory-l2/seed/<biz>/<slug>.md`
2. 有没有**打脸旧 L2**？有 → 旧条改 `status: superseded` + 原因；新条另开 path
3. **人确认**后导入 Hub（禁止静默直写）：

```bash
./ec-workbench/bin/seed-l2-to-hub ec-workbench/artifacts/memory-l2/seed/<biz>/<slug>.md
```

4. 新开空目录用 `claude-via-memory` 或 `scenario/read` 确认能召回

样例判决：`ec/ops/session-closeout-writeback.md`（本流程自身）。

## 保鲜抽检

- 抽 active L2（优先带 `anchors` 符号）对着当前 EC 搜类/方法
- 不在 → `superseded`；仍在 → 备注核对 commit / 日期
- 记录模板：`artifacts/memory-l2/freshness-batch-YYYYMMDD.md`

## 负例（不该答时）

验收时至少覆盖：

| 类型 | 期望 |
|---|---|
| Hub 无相关 L2 | 说不知道 / 去查代码，不编造「像记忆」的结论 |
| `status: superseded` | 不当真理；可指出已失效并指向替代 path |
| 题面像但业务不同 | 不串台（绑卡坑 ≠ 发券） |

## 怎么验「有用」

| 层级 | 做法 | 成功标准 |
|---|---|---|
| 库存 | Panel → Chat_Memory → L2 能看到 path | 存住了 |
| API 召回 | `scenario/read` 或 L1 `atomic/search` | 不问人也能答对要点 |
| 对话注入 | Proxy + session/team/agent/task 头；L2 注入仅为索引，需读场景工具（或已蒸馏 L1） | 答出判决关键符号，非瞎编 |
| 回写闭环 | 收尾起草 → 人闸 → `seed-l2-to-hub` → 再召回 | 用完能记 |
| 负例 | 见上表 | 不瞎用 |

Proxy 注入**必须**有会话头，否则 `injectedSkipped=true`：

- `x-tdai-user-key` / `Authorization: Bearer sk-mem-…`
- `x-session-id` 或 `x-conversation-id`
- `x-team-id` / `x-agent-id` / `x-task-id`（可先 `task/create`）

起全栈 Proxy：`PROXY_FULL_STACK=1 ./deploy/global-images/start-proxy.sh`

Panel **没有**聊天窗；Chat_Memory 页只查看 L0–L3。真对话走 Proxy（Claude Code 等）；Cursor 自定义模型往往带不齐头。

## 和 Wiki / Capillary

| | Chat Memory | Wiki | Capillary |
|---|---|---|---|
| 职 | 短判决、即时召回 | 长文全貌 | 实验死枝体检 |
| 本 Skill | ✅ | 另册 | 不要和 Memory 验收绑在一起 |

## 验收记录

- **2026-09-23**：L2 `open-app-grant.md` + L0；Panel 可见；Proxy 开屏灌券命中 Kafka；批量 3 条 → `artifacts/memory-l2/usefulness-batch-20260923.md`
- **2026-09-24**：训练 + 现码复核收尾；空目录 `claude-via-memory` 冒烟 **4/4**（Saqu / MERGE_ / H5 按钮 / APP_STARTUP）→ `artifacts/memory-l2/usefulness-batch-20260924.md`；主线收口：能存 → 能核 → 能用
- **2026-09-24 傍晚**：半自动回写 + 保鲜抽检 + 负例清单 → `artifacts/memory-l2/writeback-freshness-20260924.md`
