"""扫描入口逻辑。"""
from __future__ import annotations

from pathlib import Path
from typing import Any

import yaml

from .classify import VesselFinding, classify
from .codegraph import CodeGraphClient
from .experiment import ExperimentCache


def load_config(path: Path) -> dict[str, Any]:
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def _graph_looks_empty(text: str) -> bool:
    t = (text or "").lower()
    if not t.strip():
        return True
    empty_markers = [
        "0 found",
        "no callers",
        "(no callers)",
        "not found",
        "no results",
        "search results (0",
    ]
    return any(m in t for m in empty_markers)


def scan(config_path: Path, root: Path) -> list[VesselFinding]:
    cfg = load_config(config_path)
    cg_cfg = cfg["codegraph"]
    client = CodeGraphClient(cg_cfg["base_url"], cg_cfg.get("service_id", "default"))
    cache = ExperimentCache(root / cfg["experiment"]["cache_path"])

    findings: list[VesselFinding] = []
    for mouth in cfg.get("mouths") or []:
        mouth_id = mouth["id"]
        mouth_name = mouth.get("name") or mouth_id
        enum_dispatch = bool(mouth.get("enum_dispatch"))
        default_graph = mouth["graph_id"]
        vessels = mouth.get("vessels") or []
        if not vessels:
            findings.append(
                VesselFinding(
                    vessel_id=f"{mouth_id}-empty",
                    mouth_id=mouth_id,
                    mouth_name=mouth_name,
                    platform_key="",
                    java_symbols=[],
                    grade="unknown",
                    why=["这张嘴还没登记实验 key，先在 config.yaml 里补 vessels"],
                    graph_id=default_graph,
                    enum_dispatch_mouth=enum_dispatch,
                )
            )
            continue

        for v in vessels:
            graph_id = v.get("graph_id_override") or default_graph
            symbols = list(v.get("java_symbols") or [])
            key = v.get("platform_key") or ""
            exp = cache.get(key) if key else None

            hits: dict[str, str] = {}
            found_any = False
            callers_empty_all = True
            for sym in symbols:
                search_txt = client.search(graph_id, sym, limit=5)
                callers_txt = client.callers(graph_id, sym, limit=8)
                hits[f"search:{sym}"] = search_txt
                hits[f"callers:{sym}"] = callers_txt
                if not _graph_looks_empty(search_txt):
                    found_any = True
                if not _graph_looks_empty(callers_txt):
                    callers_empty_all = False

            grade, why = classify(
                exp,
                enum_dispatch_mouth=enum_dispatch,
                graph_found_any=found_any,
                callers_empty_for_all=callers_empty_all and bool(symbols),
            )
            if v.get("never_bury") and grade == "candidate_bury":
                grade = "manual_only"
                why.append("配置了 never_bury（主路径保护）→ 降为只能人工看，不进自动可埋")
            if v.get("never_solidify") and grade == "candidate_solidify":
                grade = "manual_only"
                why.append("配置了 never_solidify → 全量也不自动进固化修订候选")
            if v.get("reason_hint"):
                why.append(str(v["reason_hint"]))
            keep_hint = str(v.get("solidify_keep_hint") or "")
            if keep_hint and grade == "candidate_solidify":
                why.append(f"固化保留：{keep_hint}")

            findings.append(
                VesselFinding(
                    vessel_id=v["id"],
                    mouth_id=mouth_id,
                    mouth_name=mouth_name,
                    platform_key=key,
                    java_symbols=symbols,
                    grade=grade,
                    why=why,
                    experiment=exp,
                    graph_id=graph_id,
                    graph_hits=hits,
                    safe_auto=bool(v.get("safe_auto")),
                    enum_dispatch_mouth=enum_dispatch,
                    reason_hint=str(v.get("reason_hint") or ""),
                    solidify_keep_hint=keep_hint,
                )
            )
    return findings
