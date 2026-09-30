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

## 身份（本机已验 · 双 Loop）

凭据（含 key，已 gitignore）：

| Loop | 文件 | team / agent |
|---|---|---|
| **team**（组共享） | `.p0-memory-identity.json` | `ec-memory-p0` / `ec-chat-memory` |
| **myself**（仅个人） | `.p0-memory-identity-myself.json` | `ec-memory-myself` / `ec-chat-memory-myself` |

| 项 | 值 |
|---|---|
| Panel | http://127.0.0.1:8125 |
| Core | http://127.0.0.1:8420 |
| Proxy | http://127.0.0.1:8096 |
| service_id | `default` |
| 登录 | **user_key**（`sk-mem-…`） |

说明：[`MEMORY_LOOP_SCOPES.md`](../../docs/MEMORY_LOOP_SCOPES.md)

新建：meta `team/create` → `agent/create`（自动挂 `chat_memory`）→ 可选 `task/create`。

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

## 会话收尾（自动写 Hub · 旁路回执 · 分 scope）

每次用 Memory 答完 / 挖出新结论，**必须**过一遍：

1. 有没有**新判决**？有 → 选 scope 后**先起草 seed**：

```bash
# 推荐：从会话文本/transcript 自动抽草稿（默认启发式 + Proxy LLM 精修，须人闸）
pbpaste | ./ec-workbench/bin/extract-seed-from-session --scope myself
./ec-workbench/bin/extract-seed-from-session --transcript path.jsonl --tail 40 --scope team
./ec-workbench/bin/extract-seed-from-session --text "…" --no-llm   # 强制只启发式
./ec-workbench/bin/extract-seed-from-session --text "…" --dry-run  # 只看 JSON 不出文件

# 或手填骨架
./ec-workbench/bin/draft-seed-from-session \
  --scope team --biz bindcard --slug saqu-foo \
  --title "一句话标题" \
  --decision "判决正文…" \
  --symbol SomeClass#method --mouth ec-api

# 个人试写成熟后晋升 team
./ec-workbench/bin/promote-myself-to-team ops/foo.md --import
```

   - **team** → `artifacts/memory-l2/seed/<业务口>/<slug>.md`
   - **myself** → `artifacts/memory-l2/seed-myself/<业务口>/<slug>.md`
2. 有没有**打脸旧 L2**？有 → 旧条改 `status: superseded`；新条另开 path（同 scope）
3. **核对 mouths/anchors 后人闸导入**，并把回执给用户看：

```bash
./ec-workbench/bin/seed-l2-auto-import --scope team path/in/seed/...
./ec-workbench/bin/seed-l2-auto-import --scope myself path/in/seed-myself/...
./ec-workbench/bin/on-seed-change   # 扫两档变更
```

4. **旁路回执**：`receipts/team/latest.md` · `receipts/myself/latest.md`
5. 召回会话：`./bin/claude-via-memory`（team）或 `--scope myself`
6. 纠错：改 seed / superseded → 再 auto-import

样例：`ec/ops/session-closeout-writeback.md`（team）；myself 冒烟见 `seed-myself/ops/`。

### 证明有用（token / 召回）

```bash
./ec-workbench/bin/memory-token-proof   # 静态：seed vs Java 全文
./ec-workbench/bin/memory-bill-proof    # 实跑：Proxy usage + 墙钟
./ec-workbench/bin/recall-variant-proof # 问法变体 L1 命中
./ec-workbench/bin/usefulness-batch-proof  # 有用性 8 题 Proxy+L2
./ec-workbench/bin/freshness-check && ./ec-workbench/bin/freshness-miss-queue
```

产物：`proof/token-proof-latest.*` · `bill-proof-latest.*` · `recall-variant-latest.*` · `usefulness-batch-latest.md` · `queues/freshness-miss-latest.md`

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
| **账单对照** | `bin/memory-bill-proof`（ON=读 L2；OFF=塞入口类 Java） | live usage Memory ≪ 搜仓；见 `proof/bill-proof-latest.md` |
| **问法变体** | `bin/recall-variant-proof` | L1 命中率过线；见 `proof/recall-variant-latest.md` |
| **有用性 8 题** | `bin/usefulness-batch-proof` | Proxy+L2 must_hit；见 `usefulness-batch-latest.md` |
| 回写闭环 | `extract-seed-from-session` / `draft-seed-from-session` → 人闸 → **自动** `seed-l2-auto-import` → 回执 | 用完能记；旁路可见 |
| **myself→team** | `bin/promote-myself-to-team` | 共享库存可读；myself 标 superseded |
| **保鲜 miss 队列** | `freshness-check` → `queues/freshness-miss-latest.md` | miss 有待办，不靠记性 |
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
- **2026-09-27**：负例 N1–N3 Proxy 工具环 **3/3 过** → `artifacts/memory-l2/negative-batch-20260927.md`（上午上游 502，恢复后复测）
