# SDD × CodeGraph 怎么证

评委只认脚本输出和两臂阅读清单，不认口述百分数。  
**禁止**把代谢那刀的 26.8% / 20.6% 写进本实验结论——那是少读说明书，不是图。

图：`cg-pl6vfmyi`（切片，不是全市地图）。第一张单必须落在切片已有的链上。

## 证什么 / 不证什么

| 刀 | 证 | 不证 |
|---|---|---|
| 1 explore | 先问图再抽查，是否 **token 更低且嘴找全** | 整次 plan 更便宜 |
| 2 tasks | 只看各自 explore 拆任务，是否 **少漏嘴** | tasks 少几百 token |
| 3 implement | 任务已点名文件时，图 **几乎不再省阅读**（负对照） | implement 少 30% |

不含写业务代码、跑 UT、`/verify`、泳道。

## 第一张单（开屏发券）

探索题（两臂同一句，不许改题）：

> 开屏要发降息券时，请求从哪进来、真正发券在哪、还有没有 Job / internal-api / Kafka？列出每一张嘴，并标明依据。

标准答案在 `oracle-open-app-grant.json`。事先用全仓 grep + 已对过的图链写死，跑实验时不要改。

符号入口（处理臂必须先从图搜这些，不许先海翻）：

- `grantCutInterestCouponForOpenApp`
- `AppStartUpGrantCouponProcessor`
- `processAppStartupEvent`

对照臂禁止调 CodeGraph / 禁止读本切片 `PLAYBOOK.md`。处理臂必须先问图，再 Read EC 源码核验。

## 刀 1：explore（只读）

两个新开的只读 agent，不准改 EC、不准改切片。

1. 对照：把 `prompts/explore-naive.md` 当 system，题贴上去。  
2. 处理：把 `prompts/explore-graph.md` 当 system，题贴上去。

每臂结束交一份账本，存成：

- `sdd-proof/live/explore-naive.json`
- `sdd-proof/live/explore-graph.json`

格式见 `live/LEDGER.schema.json`。`reads` 必须是实际 Read 过的 EC 路径 + 行号（全文则 start=1、end=文件行数）。`mouths` 是它交出来的入口清单。

计时：agent 启动到交出清单的墙上时钟，写进账本 `wall_seconds`。

评分：

```bash
python3 sdd-proof/score_reads.py \
  --ec /Users/lipeng/IdeaProjects/ec \
  --naive sdd-proof/live/explore-naive.json \
  --graph sdd-proof/live/explore-graph.json \
  --oracle sdd-proof/oracle-open-app-grant.json
```

**处理臂赢**：token 合计更低，且 oracle 里每张必须找到的嘴都找到，且没有把「手工洞」写成「没有调用方」。  
**处理臂输**：token 低但漏嘴；或把 `ConsumerPoolService` 拉起 Processor 写成图上有边；或把 `CouponTaskGrantJob` 当成开屏发券入口。

## 刀 2：tasks（只读）

两臂只准读**自己那份** explore 结论（从账本 `mouths` + `notes` 贴进 prompt），禁止再 grep 全仓、禁止再问图。

题：

> 若要改「开屏降息券发放」的核心逻辑，tasks 里必须点名哪些入口文件？不要写实现细节。

对照用 naive 的 mouths，处理用 graph 的 mouths。  
评分仍跑 `score_reads.py`，看 `mouths` 对 oracle 的召回。token 差忽略。

**赢**：处理臂覆盖 oracle 必填嘴，对照有漏。  
**无增益**：两边一样全或一样漏。  
**输**：处理臂更漏。

## 刀 3：implement 负对照（只读）

给两臂同一份**已经点名文件和符号**的假任务（`prompts/implement-named-task.md`）。  
只计量动手前 Read 的 token，不准真改 ec-core。

**预期**：两臂 token 同量级（比率靠近 100%）。  
若处理臂仍少很多，是任务写糊了或有人没守「只读点名文件」，不能记成 CodeGraph 增益。

可选加刀（不默认跑）：把任务改糊（只写「改开屏发券」），再比一臂整读现网、一臂先问图。那才是 implement 走偏时图能不能拉回来。

## 什么叫失败（整场作废）

- 账本路径不在 EC 磁盘，或行号对不上  
- 处理臂用了切片外的调用，却在 mouths 里写「没有」  
- 把代谢 26.8% / 20.6% 写进结论  
- 第一张单换成切片里没有的业务（风控 / 资金等）  
- 把写代码、UT、verify 算进 token  
- 处理臂读了 `PLAYBOOK.md` 却把手工边标成 [图]

## 当场复核

```bash
# 图还在、开屏链还能跑
python3 tools/query_entry_mouths.py
python3 tools/query_coupon_chain.py

# 两臂账本交齐后
python3 sdd-proof/score_reads.py \
  --ec /Users/lipeng/IdeaProjects/ec \
  --naive sdd-proof/live/explore-naive.json \
  --graph sdd-proof/live/explore-graph.json \
  --oracle sdd-proof/oracle-open-app-grant.json
```

编码器写死 `tiktoken cl100k_base`。抽查账本任一条 Read：打开该文件对应行，重跑必须同量级。

## 第二张单（可选）

`CouponTaskGrantJob` 统一发券。oracle 另写，勿和开屏发券共用。  
那张单要证的是：Job 和 `grantCouponInstant` 不是同一条排队。

## 实跑

刀 1（开屏发券 explore），2026-09-01 19:18 起步。两臂只读、并行。账本按 transcript **实际 Read** 记，不是交上来的精简行号。处理臂交清单里写了但没 Read 的 `AppController` / `AppStartupService` / `AppStartUpGrantCouponProcessor` 已剔除。PLAYBOOK / 查图脚本不进 token。

| 臂 | agent | 墙上 |
|---|---|---|
| 对照 | [Naive explore](f87e22fa-15c3-4758-b0a9-9f5ac172a40c) | 67s |
| 处理 | [Graph explore](6c781ed5-6cb3-456f-b463-edc8dd0b08de) | 77s |

`score_reads.py` 原文（不要和代谢那刀的百分数混谈）：

```
对照
   27086  合计
  嘴：过
处理（图）
    5578  合计
  嘴：输
    - 误报 not-job：开屏发券没有 *Job
token 比率 20.6%   处理-对照 -21508
刀 1 判定：处理臂输或未完成
```

脚本认输的原因：处理臂 mouths 里写了「`CouponTaskGrantJob` 存在但无 callers 连到本链」。oracle `must_not_claim` 是针匹配，提到类名即输。人工看它没把 Job 当成开屏入口，也把 `topic_config` / `getProcessorClass` 标了 **[手工]**。三张必找嘴都在。

**刀 1 按脚本记：处理臂输。** token 更低不算赢。不改 oracle。

刀 2（tasks），2026-09-01 19:29 起步。只贴各自 explore 的 mouths+notes，transcript 无 Read/Grep。`score_reads.py` 因 reads 为空会作废，本刀只跑 `score_mouths`，token 忽略。

| 臂 | agent | 墙上 |
|---|---|---|
| 对照 | [Naive tasks](ede74136-a1c9-45b9-8322-48eccf44545a) | 40s |
| 处理 | [Graph tasks](e763e9e0-4140-47da-bf6f-9e2093aad51e) | 37s |

```
对照嘴: 漏嘴 kafka-processor（有 AppStartUpGrantCouponProcessor，notes 没写 grantCutInterestCouponForOpenApp）
处理嘴: kafka-wiring 提到了但未标 手工；误报 not-job（说明里写「不要把 CouponTaskGrantJob 写成入口」）
```

必找三张嘴：处理全，对照缺方法名。按协议「处理覆盖、对照有漏」可记召回赢；加上针和手工标签，脚本两边都输。

**刀 2 按脚本记：两边嘴都输，无增益。** 不改 oracle。

刀 3（implement 负对照），2026-09-01 19:29 起步。两臂同一份已点名任务，禁止问图。账本按实际 Read。两臂都没 Grep，在 `OpenAppGrantCouponService` 里分段翻页找方法，都超出「只读该方法」——违规对称，token 仍可对比。

| 臂 | agent | 墙上 | token |
|---|---|---|---|
| 对照 | [Naive implement](157669e4-1081-40b0-b466-65ce2553dddb) | 53s | 12406 |
| 处理 | [Graph implement](6e262202-8a22-4b5c-a885-a1e012daa93c) | 62s | 12589 |

```
token 比率 101.5%   处理-对照 +183
刀 3 判定：同量级，负对照成立（任务已点名时图不再省阅读）
```

两臂都答：发券在消费端，不是 HTTP。

**刀 3 按脚本记：负对照成立。** 不能把 implement 记成 CodeGraph 增益。

三刀汇总（脚本，不改 oracle）：刀 1 处理输（token 低但嘴针输）；刀 2 两边嘴都输、无增益；刀 3 比率 101.5%，任务点名后图不再省读。
