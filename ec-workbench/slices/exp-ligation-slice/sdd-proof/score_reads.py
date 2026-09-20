#!/usr/bin/env python3
"""给两臂 explore/tasks 账本打分：token + 嘴召回。编码器 cl100k_base。"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    import tiktoken
except ImportError:
    raise SystemExit("需要 tiktoken：pip install tiktoken")

ENC = tiktoken.get_encoding("cl100k_base")


def encode_slice(ec: Path, rel: str, start: int, end: int) -> int:
    path = ec / rel
    if not path.is_file():
        raise SystemExit(f"账本路径不在磁盘: {rel}")
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    n = len(lines)
    if start < 1 or end < start or end > n:
        raise SystemExit(f"行号越界 {rel}:{start}-{end}（文件 {n} 行）")
    return len(ENC.encode("".join(lines[start - 1 : end])))


def arm_tokens(ec: Path, ledger: dict) -> tuple[int, list[tuple[str, int]]]:
    rows = []
    total = 0
    for item in ledger.get("reads") or []:
        rel = item["path"]
        start = int(item["start"])
        end = int(item["end"])
        tok = encode_slice(ec, rel, start, end)
        rows.append((f"{rel}:{start}-{end}", tok))
        total += tok
    return total, rows


def blob(ledger: dict) -> str:
    parts = [json.dumps(ledger.get("mouths") or [], ensure_ascii=False)]
    parts.append(str(ledger.get("notes") or ""))
    return "\n".join(parts)


def find_needles(text: str, needles: list[str]) -> bool:
    return all(n in text for n in needles)


def any_needles(text: str, needles: list[str]) -> bool:
    return any(n in text for n in needles)


def score_mouths(ledger: dict, oracle: dict) -> list[str]:
    text = blob(ledger)
    mouths = ledger.get("mouths") or []
    tag_by_id = {m.get("id"): str(m.get("tag") or "") for m in mouths if isinstance(m, dict)}
    problems = []

    for item in oracle.get("must_find") or []:
        if not find_needles(text, item["needles"]):
            problems.append(f"漏嘴 {item['id']}：{item['label']}")

    for item in oracle.get("must_tag_stitch") or []:
        if any_needles(text, item["if_needles"]):
            tag = tag_by_id.get(item["id"], "")
            joined = " ".join(str(m) for m in mouths)
            if item["required_tag"] not in tag and item["required_tag"] not in joined:
                problems.append(f"{item['id']} 提到了但未标 {item['required_tag']}")

    for item in oracle.get("must_not_claim") or []:
        if any_needles(text, item["forbidden_needles"]):
            problems.append(f"误报 {item['id']}：{item['label']}")

    return problems


def dump_arm(title: str, total: int, rows: list[tuple[str, int]], problems: list[str]) -> None:
    print(title)
    for label, tok in rows:
        print(f"  {tok:6d}  {label}")
    print(f"  {total:6d}  合计")
    if problems:
        print("  嘴：输")
        for p in problems:
            print(f"    - {p}")
    else:
        print("  嘴：过")


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--ec", required=True)
    p.add_argument("--naive", required=True)
    p.add_argument("--graph", required=True)
    p.add_argument("--oracle", required=True)
    args = p.parse_args()

    ec = Path(args.ec)
    naive = json.loads(Path(args.naive).read_text(encoding="utf-8"))
    graph = json.loads(Path(args.graph).read_text(encoding="utf-8"))
    oracle = json.loads(Path(args.oracle).read_text(encoding="utf-8"))

    nt, nr = arm_tokens(ec, naive)
    gt, gr = arm_tokens(ec, graph)
    np = score_mouths(naive, oracle)
    gp = score_mouths(graph, oracle)

    dump_arm("对照", nt, nr, np)
    dump_arm("处理（图）", gt, gr, gp)

    if nt <= 0:
        print("对照 token 为 0，账本作废")
        return 2
    print(f"token 比率 {gt / nt:.1%}   处理-对照 {gt - nt}")
    naive_ok, graph_ok = not np, not gp
    if graph_ok and gt < nt:
        print("刀 1 判定：处理臂赢（更便宜且嘴过）")
        return 0
    if graph_ok and not naive_ok:
        print("刀 1 判定：处理臂嘴赢、token 未更低（tasks 刀可记召回）")
        return 0
    if graph_ok and gt >= nt:
        print("刀 1 判定：嘴过但 token 未省，不能报少读")
        return 1
    print("刀 1 判定：处理臂输或未完成")
    return 1


if __name__ == "__main__":
    sys.exit(main())
