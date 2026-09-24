# 同事怎么从「仓内对话」贡献 Chat Memory（训练）

> 给你同事看：不改 EC 主仓，只从自己 Cursor 历史对话里挖**可复用判决**，写成 seed，提 PR。  
> 主线 Playbook：[`CHAT_MEMORY_EC_PLAYBOOK.md`](./CHAT_MEMORY_EC_PLAYBOOK.md)

## 你要交付什么

不是「把整个 transcript 交上来」，而是：

1. 若干条 **`ec/<业务>/<slug>.md`**（判决正文）  
2. 放在本仓：`ec-workbench/artifacts/memory-l2/seed/<业务>/<slug>.md`  
3. 开 PR；合并后再由有 Hub 的人写入运行时 Memory（或你本机按 Playbook 写入）

**密钥、identity、Docker 卷不要提交。**

---

## 一步：对话记录在哪

Cursor Agent 历史一般在本机（macOS）：

```text
~/.cursor/projects/<项目目录编码>/agent-transcripts/<uuid>/<uuid>.jsonl
```

例子（按同事本机项目改）：

```text
~/.cursor/projects/Users-<你>-IdeaProjects-ec/agent-transcripts/
~/.cursor/projects/Users-<你>-IdeaProjects-ec-1/agent-transcripts/
```

每个子目录一个会话；真正内容是里面的 `.jsonl`。

快速摸有多少会话：

```bash
find ~/.cursor/projects/Users-$USER-IdeaProjects-ec/agent-transcripts -name '*.jsonl' | wc -l
```

---

## 二步：什么算「值得进 Memory」

**要（durable）**

- 带符号的结论：应对标哪套 / 不要只改哪 / 字段语义 / 链路真相  
- 下次换人还会再问  
- 能落到 `Service` / `Controller` / 路由 / 表字段

**不要**

- 单次 userId / 单条 trace 排障过程  
- SDD / reflect / arc / 文档要不要更新  
- 「今天这批实验 UV」这类易过期观测（除非固化成规则）

口令：问自己——**三个月后别人只看到这一段，还能不能直接用？**

---

## 三步：怎么从 jsonl 里找

助手回复里常有：`结论先说` / `结论：` / `排查结论` / `根因` / `不要` / `应对标`。

示例（在某个项目的 transcripts 根下）：

```bash
ROOT=~/.cursor/projects/Users-$USER-IdeaProjects-ec/agent-transcripts
rg -l '结论先说|排查结论|应对标|关键方法' "$ROOT" --glob '*.jsonl' | head -50
```

打开命中会话，把**最终拍板的那几段**摘出来（不要整份对话）。

记下：

- transcript 短 id（目录名 uuid 前 8 位即可）  
- 关键符号 / 路由 / 表  

---

## 四步：写成 seed（格式固定）

路径：`ec-workbench/artifacts/memory-l2/seed/<业务>/<slug>.md`  
对外 Memory path 约定：`ec/<业务>/<slug>.md`（与 seed 相对路径一致）。

模板：

```markdown
# 一句话标题

## decision
（3～8 句：结论 + 为何 + 排障时先看什么）

## mouths
- 业务词 / 服务名

## anchors
- symbols: `FooService#bar`, `SomeController`
- routes / tables / enums: …

## constraints
- 不要… / 必须…

## evidence
- Cursor×<仓名> · transcript `<短id>`
- （可选）verified vs EC `<git sha>` on YYYY-MM-DD

## status
active

## updated
YYYY-MM-DD
```

样例可抄：

- `ec-workbench/artifacts/memory-l2/seed/auth/merge-user-id-vs-merged-user-id.md`
- `ec-workbench/artifacts/memory-l2/seed/bindcard/saqu-superbank-pattern.md`

写之前先扫一眼同目录有没有近义文件，避免重复。

---

## 五步：现码核对（强烈建议）

在 **EC 现仓**确认：

1. 文里点名的类/方法还在  
2. 结论方向没被改掉  

过时就别提，或写明「历史结论，待核实」。  
合并前理想状态：`status: active` 且能对上现码。

---

## 六步：提 PR

```text
仓库：TencentDB-Agent-Memory（或你们约定的旁路仓）
只加 seed md + 如有必要的说明
Commit 说明写清：来自哪个业务仓对话、几条、主题
```

**不要**提交：

- `ec-workbench/.p0-memory-identity.json`
- `ec-workbench/.claude-via-memory/`
- 任何 `.env` / `sk-mem-…`

---

## 七步：谁负责「写入 Hub」

- **挖判决 + 写 seed + PR** = 每位同事都能做（不必人人起 Memory）  
- **seed → scenario 落盘 + write** = 有本机 Hub / 共享 Hub 的人做（见 Playbook）

没有 Hub 的同事：**交 seed 即完成训练贡献。**

---

## 最小检查清单（交 PR 前）

- [ ] 路径 `seed/<biz>/<slug>.md`，slug 英文短横线  
- [ ] 有 `decision` / `anchors` / `constraints` / `status` / `evidence`  
- [ ] 至少 1 个可检索符号或路由  
- [ ] 不是单次 trace 流水账  
- [ ] 与已有 seed 不重复  
- [ ] （建议）EC 现码扫过符号仍在  

---

## 你（发起人）怎么跟同事说（可复制）

> 帮我从你本机 Cursor 里 EC / ec-1（或你负责的仓）的 `agent-transcripts` 挖可复用业务判决。  
> 按 `ec-workbench/docs/CHAT_MEMORY_EC_COLLEAGUE_TRAIN.md` 写成 seed md，提 PR。  
> 先不用起 Memory Hub；密钥别提交。优先「能落到类名/方法名」的结论。
