import {
  Callout,
  Divider,
  Grid,
  H1,
  H2,
  H3,
  Pill,
  Row,
  Stack,
  Stat,
  Table,
  Text,
} from "cursor/canvas";

type Status = "done" | "partial" | "todo" | "skip";

const STATUS_PILL: Record<
  Status,
  { label: string; tone: "success" | "warning" | "neutral" | "deleted" }
> = {
  done: { label: "已做", tone: "success" },
  partial: { label: "部分", tone: "warning" },
  todo: { label: "未做", tone: "neutral" },
  skip: { label: "不做/做不成", tone: "deleted" },
};

function StatusPill({ status }: { status: Status }) {
  const s = STATUS_PILL[status];
  return (
    <Pill tone={s.tone} active={status === "done" || status === "partial"}>
      {s.label}
    </Pill>
  );
}

function rows(
  items: { id: string; what: string; status: Status; note: string }[],
) {
  return items.map((r) => [
    r.id,
    r.what,
    <StatusPill status={r.status} />,
    r.note,
  ]);
}

export default function MemoryHubCapabilities() {
  return (
    <Stack gap={24}>
      <Stack gap={8}>
        <H1>Memory Hub 能力全表 × 你们进度</H1>
        <Text tone="secondary">
          左侧是产品全部能力（编号沿用路线）；右侧标你们实际进度。
          CodeGraph 标探索进度；Capillary 是基于 CodeGraph 探索之后做出的**实现层
          Agent**（不是 Hub 新产品件）。
        </Text>
        <Text size="small" tone="tertiary">
          进度更新 Sep 24, 2026 · Hub :8125 / Core :8420 / Proxy :8096 ·
          隔离：只改切片/Hub，EC 与 ~/.cursor 不动
        </Text>
      </Stack>

      <Grid columns={4} gap={12}>
        <Stat value="1" label="合并图 ready" tone="success" />
        <Stat value="712" label="files · cg-zp42c34n" tone="success" />
        <Stat value="~55" label="L2 active（旁路）" tone="success" />
        <Stat value="4/4" label="Memory 有用性冒烟" tone="success" />
      </Grid>

      <Callout tone="info" title="怎么读这张表">
        **§1–9**：Memory Hub 能力清单。主线曾在 CodeGraph；**§3 Chat Memory
        旁路 P0 已收口**（存/核/用）。
        **§1.x**：切片建图 → 合成一图 → 查询/sync 已验证。
        **§10**：Capillary Agent（图 + 实验 MCP + 人闸；独立于 Memory 验收）。
      </Callout>

      <H2>在线图（当前）</H2>
      <Table
        headers={["图 id", "范围", "仓", "规模"]}
        rows={[
          [
            "cg-zp42c34n",
            "发券+首页下单+还款+风控+绑卡",
            "ec-cashloan-combined-slice",
            "712 files · 23629 nodes · 38576 edges",
          ],
        ]}
      />

      <Divider />

      <H2>1. CodeGraph（能力 + 你们进度）</H2>
      <Text tone="secondary" size="small">
        产品：索引符号/文件/调用/impact；tools：search、explore、callers、callees、impact、node、status、files。引擎
        @colbymchenry/codegraph（tree-sitter → SQLite）。一仓一图（同
        repo_url+branch 唯一）。
      </Text>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        columnAlign={["left", "left", "left", "left"]}
        rows={rows([
          {
            id: "1.1",
            what: "发券/开屏：搜类、方法、接口、HTTP 路径",
            status: "done",
            note: "已并入 cg-zp42c34n；原 cg-pl6vfmyi",
          },
          {
            id: "1.2",
            what: "首页+下单图（含 processor）",
            status: "done",
            note: "已并入合并图；原 cg-b0xmzi3v",
          },
          {
            id: "1.3",
            what: "问方法调了谁（callees）",
            status: "done",
            note: "链上用过",
          },
          {
            id: "1.4",
            what: "对某一个方法要源码",
            status: "done",
            note: "首页改卡片样式等调研用过",
          },
          {
            id: "1.5",
            what: "文件清单、图是否 ready",
            status: "done",
            note: "面板 / status 核过",
          },
          {
            id: "1.6",
            what: "补源码后 sync",
            status: "done",
            note: "切片扩文件后 sync；增量 syncIndex",
          },
          {
            id: "1.7",
            what: "本机 SDD：探索先问 Hub",
            status: "partial",
            note: "先不做。对照曾跑：读量≈1/5；日常未固化",
          },
          {
            id: "1.8",
            what: "还款图",
            status: "done",
            note: "已并入合并图；原 cg-locevwhf",
          },
          {
            id: "1.9",
            what: "风控图",
            status: "done",
            note: "已并入合并图；原 cg-1z1tgq4n",
          },
          {
            id: "1.13",
            what: "绑卡图 → 五业务合成一张",
            status: "done",
            note: "cg-zp42c34n；旧五图已删",
          },
          {
            id: "1.10",
            what: "全仓一张图（真绑 EC git）",
            status: "skip",
            note: "本机不稳；现用合并切片模拟一仓一图",
          },
          {
            id: "1.11",
            what: "只用 callers 判死代码",
            status: "skip",
            note: "做不成；见 §10 实现层 Agent",
          },
          {
            id: "1.12",
            what: "枚举 / 反射 / 跨仓连线",
            status: "skip",
            note: "图穿不过；PLAYBOOK 标手工",
          },
        ])}
      />

      <H2>10. CodeGraph 实现层 · Capillary Agent</H2>
      <Text tone="secondary" size="small">
        在 §1 探索清楚「图只能答谁连谁、判死代码还要实验平台」之后，基于 CodeGraph
        落地的实现层：毛细血管 Agent。落点
        `exp-ligation-slice/tools/capillary`。输入：合并图 + 实验 MCP；输出：分级报告
        + 人闸计划/切分支。不是 Memory Hub 四件套之一。
      </Text>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        columnAlign={["left", "left", "left", "left"]}
        rows={rows([
          {
            id: "10.0",
            what: "从 CodeGraph 探索收成清理 Agent",
            status: "done",
            note: "方案：图=结构，MCP=死活，人闸=动手",
          },
          {
            id: "10.1",
            what: "白名单嘴 + vessel（挂合并图）",
            status: "done",
            note: "homepage / order / coupon / bindcard → cg-zp42c34n",
          },
          {
            id: "10.2",
            what: "问图 + 实验 → 分级报告",
            status: "done",
            note: "live / solidify / bury / manual_only / unknown",
          },
          {
            id: "10.3",
            what: "全量固化候选 solidify",
            status: "done",
            note: "Superbank 计划已出；非整枚举删",
          },
          {
            id: "10.4",
            what: "关量埋葬候选 bury",
            status: "partial",
            note: "券 v1 等；主路径 never_bury",
          },
          {
            id: "10.5",
            what: "人闸切分支 + CAPILLARY_PLAN",
            status: "done",
            note: "propose_diff；Java 人工改",
          },
          {
            id: "10.6",
            what: "自动改 EC / 无人 land",
            status: "skip",
            note: "safe_auto=false",
          },
          {
            id: "10.7",
            what: "定时 cron + 实验 HTTP 刷新",
            status: "partial",
            note: "crontab.example；多靠 --from-json",
          },
          {
            id: "10.8",
            what: "有图 vs 无图 token 对照",
            status: "done",
            note: "Sep9：「有用否/删」有图约 −74%（当次无图读更凶）",
          },
        ])}
      />

      <Divider />

      <H2>2. Wiki</H2>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        rows={rows([
          {
            id: "2.1",
            what: "导入设计/流程文档 → 可搜页面",
            status: "todo",
            note: "产品有；这条线没做",
          },
          {
            id: "2.2",
            what: "搜完读一页，挂 Agent 问「为什么」",
            status: "todo",
            note: "",
          },
          {
            id: "2.3",
            what: "代替 CodeGraph 找调用",
            status: "skip",
            note: "做不成",
          },
          {
            id: "2.4",
            what: "代替 EC 仓本地 wiki/",
            status: "skip",
            note: "不是同一套",
          },
        ])}
      />

      <H2>3. Chat Memory（旁路 P0 · 2026-09-24 收口）</H2>
      <Text tone="secondary" size="small">
        L0→L1→L2→L3；路径约定 ec/&lt;biz&gt;/&lt;slug&gt;.md。不改 EC
        主仓。详见 Playbook / Skill / usefulness-batch-20260924。
      </Text>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        rows={rows([
          {
            id: "3.1",
            what: "记下偏好、决策、坑",
            status: "done",
            note: "L2 seed≈55 active + Hub 写入；现码核过 durable",
          },
          {
            id: "3.2",
            what: "导入历史对话",
            status: "done",
            note: "ec/ec-1 transcript 提炼；L0 import 蒸馏 L1",
          },
          {
            id: "3.3",
            what: "按原文/事实/场景/画像召回",
            status: "partial",
            note: "scenario/read + L1 search 已验；L3 未深用",
          },
          {
            id: "3.4",
            what: "私有或分享给团队",
            status: "partial",
            note: "seed 已推 memory-hub；同事按 COLLEAGUE_TRAIN 贡献",
          },
          {
            id: "3.5",
            what: "经 Proxy 自动写入、下一轮自动带上",
            status: "partial",
            note: "注入已验；半自动回写 seed-l2-to-hub+人闸已通；全自动写入未做",
          },
          {
            id: "3.6",
            what: "当用户名单 / 发送流水库",
            status: "skip",
            note: "做不成",
          },
        ])}
      />

      <H2>4. Skill</H2>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        rows={rows([
          {
            id: "4.1",
            what: "跑通过程收成带版本 Skill",
            status: "done",
            note: "ec-workbench/skills/chat-memory-ec + Playbook",
          },
          {
            id: "4.2",
            what: "「问哪张图」收成 Skill",
            status: "todo",
            note: "已一图，优先级下降；Capillary README 可当素材",
          },
          {
            id: "4.3",
            what: "审核后分给其他 Agent",
            status: "todo",
            note: "旁路 Skill 在 Git；未挂 Hub Skill 资产分发",
          },
          {
            id: "4.4",
            what: "官方 SDD 自动变成 Hub Skill",
            status: "todo",
            note: "要人导出或重写",
          },
          {
            id: "4.5",
            what: "Skill 自己按点跑、发短信",
            status: "skip",
            note: "做不成",
          },
        ])}
      />

      <H2>5. 组队与配装</H2>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        rows={rows([
          {
            id: "5.1",
            what: "建 Team、登记 Agent 身份",
            status: "done",
            note: "ec-memory-p0 / ec-chat-memory（key 本机 gitignore）",
          },
          {
            id: "5.2",
            what: "图 / Wiki / 记忆挂给多个身份",
            status: "partial",
            note: "chat_memory 已挂该 Agent；图未与同身份统一配装",
          },
          {
            id: "5.3",
            what: "private / team / ACL，解绑",
            status: "todo",
            note: "产品有；未深用",
          },
          {
            id: "5.4",
            what: "登记后远程叫醒别人的 Agent",
            status: "skip",
            note: "做不成",
          },
        ])}
      />

      <H2>6. Proxy 接入</H2>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        rows={rows([
          {
            id: "6.1",
            what: "开 Proxy",
            status: "done",
            note: "FULL_STACK :8096 healthy",
          },
          {
            id: "6.2",
            what: "Claude Code 指过去读档",
            status: "done",
            note: "bin/claude-via-memory 隔离配置；空目录冒烟 4/4",
          },
          {
            id: "6.3",
            what: "Codex / CodeBuddy / 名单其它客户端",
            status: "todo",
            note: "",
          },
          {
            id: "6.4",
            what: "Cursor 官方接入",
            status: "skip",
            note: "自定义模型带头弱；用 CC 旁路验收",
          },
          {
            id: "6.5",
            what: "只在面板点分配、不指 Proxy",
            status: "skip",
            note: "不生效",
          },
        ])}
      />

      <H2>7. 冷启动导入</H2>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        rows={rows([
          {
            id: "7.1",
            what: "Git → CodeGraph",
            status: "done",
            note: "合并切片 HTTPS 导入 ready",
          },
          {
            id: "7.2",
            what: "文档 → Wiki",
            status: "todo",
            note: "",
          },
          {
            id: "7.3",
            what: "旧对话 → 记忆和 Skill",
            status: "done",
            note: "transcript→seed→L2；同事贡献指南已发",
          },
          {
            id: "7.4",
            what: "官方私有仓 / SSH",
            status: "skip",
            note: "产品未完善",
          },
          {
            id: "7.5",
            what: "全量 EC 一张图",
            status: "skip",
            note: "别挑；用合并切片",
          },
        ])}
      />

      <H2>8. 面板</H2>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        rows={rows([
          {
            id: "8.1",
            what: "看资产来源、版本、分给谁、ready",
            status: "partial",
            note: "用过看图状态；未系统用配装",
          },
          {
            id: "8.2",
            what: "搜和管理四类资产",
            status: "partial",
            note: "主要管 CodeGraph",
          },
          {
            id: "8.3",
            what: "建 Task，Proxy 里挂对话到任务",
            status: "todo",
            note: "要 Proxy",
          },
          {
            id: "8.4",
            what: "当编排器跑感知/决策/发送",
            status: "skip",
            note: "做不成",
          },
        ])}
      />

      <H2>9. 路线图 / 先别当交付</H2>
      <Table
        headers={["编号", "能力", "进度", "你们备注"]}
        rows={rows([
          {
            id: "9.1",
            what: "全自动记忆路由",
            status: "skip",
            note: "仍在迭代",
          },
          {
            id: "9.2",
            what: "更快 Wiki / 自定义 Prompt / Skill 导出",
            status: "skip",
            note: "roadmap v2.0.1",
          },
        ])}
      />

      <Divider />
      <H3>下一步</H3>
      <Row gap={8}>
        <Pill tone="success" active>
          半自动回写 + 保鲜抽检
        </Pill>
        <Pill tone="warning" active>
          负例 N1–N3 空目录再跑
        </Pill>
        <Pill tone="neutral">同事按 COLLEAGUE_TRAIN 贡献 seed</Pill>
        <Pill tone="neutral">Capillary bury（独立 Loop）</Pill>
      </Row>
      <Text size="small" tone="tertiary">
        结构：§1–9 = Hub 能力大表 → §3/4/5/6/7 含 Memory
        旁路进度 → §10 = Capillary 实现层 Agent（勿与 Memory 验收绑死）。
      </Text>
    </Stack>
  );
}
