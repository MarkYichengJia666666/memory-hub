"""写出 markdown + json 报告。"""
from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

from .classify import VesselFinding

GRADE_CN = {
    "live": "还活着",
    "candidate_solidify": "全量固化修订候选",
    "candidate_bury": "可埋候选",
    "manual_only": "只能人工看",
    "unknown": "未知",
}

GRADE_ORDER = [
    "candidate_solidify",
    "candidate_bury",
    "manual_only",
    "live",
    "unknown",
]


def write_reports(findings: Iterable[VesselFinding], out_dir: Path) -> tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    ts = datetime.now(timezone.utc).astimezone().strftime("%Y%m%d-%H%M%S")
    items = list(findings)
    md_path = out_dir / f"capillary-{ts}.md"
    json_path = out_dir / f"capillary-{ts}.json"

    by_grade: dict[str, list[VesselFinding]] = {}
    for f in items:
        by_grade.setdefault(f.grade, []).append(f)

    lines: list[str] = []
    lines.append(f"# 毛细血管体检报告 · {ts}")
    lines.append("")
    lines.append(
        "定时 Agent 产出。删不删 / 固不固化由人定；本报告说明**为什么建议 / 不建议**。"
    )
    lines.append("")
    lines.append("## 汇总")
    lines.append("")
    for g in GRADE_ORDER:
        n = len(by_grade.get(g, []))
        if n:
            lines.append(f"- **{GRADE_CN.get(g, g)}**（`{g}`）：{n} 条")
    lines.append("")

    for g in GRADE_ORDER:
        rows = by_grade.get(g) or []
        if not rows:
            continue
        lines.append(f"## {GRADE_CN.get(g, g)}")
        lines.append("")
        for f in rows:
            lines.append(f"### `{f.vessel_id}` · {f.mouth_name}")
            lines.append("")
            lines.append(f"- 平台 key：`{f.platform_key}`")
            lines.append(f"- Java 符号：{', '.join(f'`{s}`' for s in f.java_symbols)}")
            lines.append(f"- 图：`{f.graph_id}`")
            if f.reason_hint:
                lines.append(f"- 备注：{f.reason_hint}")
            if f.solidify_keep_hint:
                lines.append(f"- 固化保留：{f.solidify_keep_hint}")
            lines.append("- **为什么：**")
            for w in f.why:
                lines.append(f"  - {w}")
            if f.graph_hits:
                lines.append("- 图查询摘要：")
                for sym, text in f.graph_hits.items():
                    snippet = text.replace("\n", " ").strip()[:220]
                    lines.append(f"  - `{sym}`：{snippet}")
            if g in {"candidate_bury", "candidate_solidify"}:
                lines.append(
                    f"- 下一步：确认后执行 "
                    f"`python3 propose_diff.py --report {md_path.name} "
                    f"--vessel-id {f.vessel_id} --dry-run`"
                )
            lines.append("")

    lines.append("## 怎么提 diff")
    lines.append("")
    lines.append(
        "1. `candidate_bury`：关量死枝，确认后提**删除**计划。"
    )
    lines.append(
        "2. `candidate_solidify`：已全量，确认后提**去门闸、留赢家**固化计划（不是整枝删）。"
    )
    lines.append("3. `manual_only` / 枚举分发：**不要**自动改。")
    lines.append("4. `propose_diff.py --apply` 会切分支；合不合并仍是人来。")
    lines.append("")

    md_path.write_text("\n".join(lines), encoding="utf-8")
    payload = {
        "generated_at": ts,
        "findings": [f.to_dict() for f in items],
    }
    json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return md_path, json_path
