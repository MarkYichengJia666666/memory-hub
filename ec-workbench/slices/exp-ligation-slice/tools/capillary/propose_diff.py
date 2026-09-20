#!/usr/bin/env python3
"""确认后：切分支 + 写计划；可选尝试安全自动改（默认 dry-run）。

支持两种候选：
- candidate_bury：关量死枝 → 删除计划
- candidate_solidify：已全量 → 去门闸、保留赢家行为的固化计划

MVP：
- 始终写出 approved/*.md
- --apply：在 EC 仓切分支 capillary/<vessel-id>-<date>
- 自动改代码仅当 finding.safe_auto=true（当前配置全是 false）
- --arc：若本机有 arc，尝试 arc diff（需人已看过 plan）
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import date
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent

ALLOWED_GRADES = {"candidate_bury", "candidate_solidify"}


def _load_finding(report_json: Path, vessel_id: str) -> dict:
    data = json.loads(report_json.read_text(encoding="utf-8"))
    for f in data.get("findings") or []:
        if f.get("vessel_id") == vessel_id:
            return f
    raise SystemExit(f"vessel_id not found: {vessel_id}")


def _report_json_from_md(md: Path) -> Path:
    js = md.with_suffix(".json")
    if not js.exists():
        raise SystemExit(f"需要同名 json 报告：{js}")
    return js


def _plan_bury(finding: dict, vessel_id: str) -> str:
    why = finding.get("why") or []
    symbols = finding.get("java_symbols") or []
    key = finding.get("platform_key")
    return "\n".join(
        [
            f"# Capillary 删除计划 · {vessel_id}",
            "",
            f"- 平台 key：`{key}`",
            f"- grade：`{finding.get('grade')}`",
            f"- safe_auto：{finding.get('safe_auto')}",
            "",
            "## 为什么删",
            "",
            *[f"- {w}" for w in why],
            "",
            "## 涉及符号",
            "",
            *[f"- `{s}`" for s in symbols],
            "",
            "## 操作要求",
            "",
            "- 只删实验枝 / 废弃常量，不要动仍在用的主业务路径。",
            "- 首页枚举分发、Kafka 反射拉起：必须人工核。",
            "- 改完跑相关单测，再 arc diff；合入前 Code Review。",
            "",
        ]
    )


def _plan_solidify(finding: dict, vessel_id: str) -> str:
    why = finding.get("why") or []
    symbols = finding.get("java_symbols") or []
    key = finding.get("platform_key")
    keep = finding.get("solidify_keep_hint") or finding.get("reason_hint") or ""
    hits = finding.get("graph_hits") or {}
    return "\n".join(
        [
            f"# Capillary 全量固化计划 · {vessel_id}",
            "",
            f"- 平台 key：`{key}`",
            f"- grade：`candidate_solidify`",
            f"- safe_auto：{finding.get('safe_auto')}",
            "",
            "## 目标",
            "",
            "实验已 **FULL_PERCENTAGE**：去掉分流门闸，**保留实验组赢家行为**，",
            "不要把整段业务当死代码删掉。",
            "",
            "## 为什么要固化",
            "",
            *[f"- {w}" for w in why],
            "",
            "## 建议保留的行为",
            "",
            f"- {keep}" if keep else "- （config 未写 solidify_keep_hint，请对照实验组逻辑人工写明）",
            "",
            "## 建议改动步骤",
            "",
            "1. 找到分流调用（`ExpFacade` / `getResult` / `abTestByUserId` 等），确认赢家组取值。",
            "2. 删除「是否进实验组」判断；直接执行原实验组分支。",
            "3. 删除仅服务于分流的私有方法 / 枚举项 / ABTestConfig 版本表行（确认无其它引用）。",
            "4. 更新/删除只测分流门闸的单测；补一条「固化后行为」单测。",
            "5. 跑相关单测 → arc diff → Code Review。",
            "",
            "## 涉及符号（图）",
            "",
            *[f"- `{s}`" for s in symbols],
            "",
            "## 图查询摘要",
            "",
            *(
                [
                    f"- `{k}`：{(v or '').replace(chr(10), ' ').strip()[:240]}"
                    for k, v in hits.items()
                ]
                if hits
                else ["- （无）"]
            ),
            "",
            "## 禁止",
            "",
            "- 不要删赢家行为本身（例如 Superbank 只留 FAMA 的过滤逻辑）。",
            "- 不要在未确认产运的情况下改其它渠道 / 白名单逻辑。",
            "",
        ]
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", required=True, help="reports/capillary-*.md 或 .json")
    parser.add_argument("--vessel-id", required=True)
    parser.add_argument("--config", default=str(ROOT / "config.yaml"))
    parser.add_argument("--dry-run", action="store_true", default=True)
    parser.add_argument("--apply", action="store_true", help="真切分支（关闭 dry-run）")
    parser.add_argument("--arc", action="store_true", help="切完后尝试 arc diff")
    args = parser.parse_args()
    dry = not args.apply

    report_path = Path(args.report)
    if not report_path.is_absolute():
        candidate = ROOT / report_path
        if not candidate.exists():
            candidate = ROOT / "reports" / report_path.name
        report_path = candidate
    if report_path.suffix == ".md":
        report_json = _report_json_from_md(report_path)
    else:
        report_json = report_path

    finding = _load_finding(report_json, args.vessel_id)
    grade = finding.get("grade")
    if grade not in ALLOWED_GRADES:
        print(
            f"拒绝：grade={grade}，只允许 {sorted(ALLOWED_GRADES)}。"
            f" live/manual_only 请人工处理。",
            file=sys.stderr,
        )
        return 2

    cfg = yaml.safe_load(Path(args.config).read_text(encoding="utf-8"))
    ec = Path(cfg["ec_repo"])
    if not ec.is_dir():
        raise SystemExit(f"ec_repo 不存在: {ec}")

    if grade == "candidate_solidify":
        plan_text = _plan_solidify(finding, args.vessel_id)
        plan_kind = "solidify"
    else:
        plan_text = _plan_bury(finding, args.vessel_id)
        plan_kind = "bury"

    approved_dir = ROOT / "approved"
    approved_dir.mkdir(parents=True, exist_ok=True)
    plan_file = approved_dir / f"{args.vessel_id}-{plan_kind}-{date.today().isoformat()}.md"
    plan_file.write_text(plan_text, encoding="utf-8")
    print(f"plan → {plan_file}")
    print(f"kind → {plan_kind}")

    branch = f"capillary/{args.vessel_id}-{plan_kind}-{date.today().isoformat()}"
    print(f"branch → {branch}")
    print(f"dry_run → {dry}")

    if dry:
        print("dry-run：未切分支、未改文件。加 --apply 才会在 EC 仓执行 git checkout -b。")
        print(
            "当前 config 里 safe_auto 全为 false：即便 --apply 也只切分支 + 写入计划，不自动改 Java。"
        )
        return 0

    subprocess.run(["git", "status", "-sb"], cwd=ec, check=False)
    r = subprocess.run(
        ["git", "checkout", "-b", branch],
        cwd=ec,
        capture_output=True,
        text=True,
    )
    if r.returncode != 0:
        print(r.stderr or r.stdout, file=sys.stderr)
        r2 = subprocess.run(
            ["git", "checkout", branch], cwd=ec, capture_output=True, text=True
        )
        if r2.returncode != 0:
            print(r2.stderr, file=sys.stderr)
            return 1
        print(f"switched to existing {branch}")
    else:
        print(f"created and checked out {branch}")

    dest = ec / "CAPILLARY_PLAN.md"
    dest.write_text(plan_text, encoding="utf-8")
    print(f"wrote {dest}")

    if finding.get("safe_auto"):
        print("safe_auto=true：此处可接自动改符号逻辑（MVP 未开，请人工改）。")
    else:
        print("safe_auto=false：请按 CAPILLARY_PLAN.md 人工改完再提交。")

    if args.arc:
        print("尝试 arc diff …")
        arc = subprocess.run(
            ["arc", "diff", "--preview"],
            cwd=ec,
            capture_output=True,
            text=True,
        )
        print(arc.stdout or arc.stderr)
        if arc.returncode != 0:
            print("arc 未成功；请本机手动 arc diff。", file=sys.stderr)
            return 1
    else:
        print("下一步（人工）：改代码 → git add → arc diff")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
