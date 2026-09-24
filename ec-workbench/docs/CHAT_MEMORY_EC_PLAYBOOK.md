# Chat Memory × EC 旁路 · Playbook

> 状态：2026-09-24 主线收口（存 / 核 / 用）+ 半自动回写 / 保鲜抽检  
> Skill：[`../skills/chat-memory-ec/SKILL.md`](../skills/chat-memory-ec/SKILL.md)  
> 有用性：[`../artifacts/memory-l2/usefulness-batch-20260924.md`](../artifacts/memory-l2/usefulness-batch-20260924.md)  
> 回写+保鲜：[`../artifacts/memory-l2/writeback-freshness-20260924.md`](../artifacts/memory-l2/writeback-freshness-20260924.md)  
> 同事从对话挖判决：[`CHAT_MEMORY_EC_COLLEAGUE_TRAIN.md`](./CHAT_MEMORY_EC_COLLEAGUE_TRAIN.md)

## 目标

用 Memory Hub **Chat Memory** 记住 EC 相关业务判决，换会话能召回。  
**旁路 = 不改 EC 主仓**；切片 / Hub / Capillary 可动。

## 一句话结论

| 已证 | 未做 / 不做 |
|---|---|
| 建 team/agent/key，自动挂 chat_memory | 改 EC、真删 Java |
| L2 判决可写可读；Panel 可见 | Cursor 自定义模型带头（能力弱） |
| L0 导入可蒸馏 L1，可 search | 把 Memory 验收绑成 Capillary 删码 |
| Proxy 注入后模型答对「发券在 Kafka」 | 用 Wiki 替代短判决 |
| 半自动回写（seed → 人闸 → `seed-l2-to-hub`） | Proxy **全自动**静默写入 |
| 保鲜抽检 + superseded / 负例清单 | 实时对码引擎 |

## 最小闭环

```text
1. 起 Core :8420 + Hub :8125
2. 建/用 team + agent + user_key
3. 写 L2：ec/<business>/<slug>.md（本地 seed 或收尾草稿）
4. 人确认后：./ec-workbench/bin/seed-l2-to-hub <seed.md>
5. Panel Chat_Memory 确认 L2
6. （可选）PROXY_FULL_STACK=1 起 Proxy :8096
7. 带 session + team/agent/task 头问一句 → 看是否命中判决
8. 会话收尾：新结论再走 3–4；打脸旧条则 superseded
```

## 会话收尾 / 保鲜 / 负例

详见 Skill 同名三节。导入脚本：[`../bin/seed-l2-to-hub`](../bin/seed-l2-to-hub)。
## 路径与正文

- 路径 = 索引：`ec/coupon/open-app-grant.md`
- 字段：`decision` / `mouths` / `anchors` / `constraints` / `status`
- 样例正文：`artifacts/memory-l2/open-app-grant.md`
- 有用性记录：`artifacts/memory-l2/usefulness-first-judgment.md`

## API 要点

数据面 `POST http://127.0.0.1:8420/v3/scenario/{write,read,ls}`：

- Header：`Authorization: Bearer <gateway>`、`x-tdai-service-id: default`
- Body：`team_id`、`agent_id`、`user_id` + `path` / `content`

**`scenario/write` 不能创建文件**（存在才可写）。新建先 `docker exec` 落到：

```text
/data/tdai-memory/profiles/<urlencode(team:TID|agent:AID)>/scene_blocks/<path>
```

再调用 write。

L0 导入（触发 L1 蒸馏）：Panel `POST /api/v1/chat-memory/import`。

## Proxy 注入

```bash
cd deploy/global-images
PROXY_FULL_STACK=1 ./start-proxy.sh
```

请求示例头：

```http
Authorization: Bearer sk-mem-…
x-tdai-user-key: sk-mem-…
x-session-id: <任意会话 id>
x-team-id: team-…
x-agent-id: agt-…
x-task-id: task-…    # 可先 meta task/create
```

无 `x-session-id` / `x-conversation-id` → `injectedSkipped=true` → 模型瞎编。

Endpoint（OpenAI 兼容）：`POST http://127.0.0.1:8096/proxy/default/v1/chat/completions`

## Panel 入口

- 登录：粘贴 user_key（无密码）
- **Chat_Memory**：只查看 L0–L3，**不是**聊天窗
- 团队：`ec-memory-p0`；Agent：`ec-chat-memory`

本地凭据：`ec-workbench/.p0-memory-identity.json`（勿提交）

## 和别的能力边界

- **Wiki**：长文全貌；Memory 是短判决。冲突以 Memory `active` 为准。
- **CodeGraph**（`cg-zp42c34n`）：结构探路；Memory 不管图。
- **Capillary**：实验枝体检；可另作 Loop，勿冒充 Memory 验收。
