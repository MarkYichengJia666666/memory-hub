"""Stitch overlay：反射/枚举补丁边，与 CodeGraph 查询结果合并。"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

_DIR = Path(__file__).resolve().parents[5] / "artifacts" / "graph-stitch"


def overlay_path() -> Path:
    yml = _DIR / "overlay.yaml"
    js = _DIR / "overlay.json"
    return yml if yml.is_file() else js


def load_overlay(path: Path | None = None) -> dict[str, Any]:
    p = path or overlay_path()
    if not p.is_file():
        alt = _DIR / "overlay.json"
        p = alt if alt.is_file() else p
    if not p.is_file():
        return {"version": 0, "edges": []}
    text = p.read_text(encoding="utf-8")
    if p.suffix in {".yaml", ".yml"}:
        try:
            import yaml  # type: ignore

            data = yaml.safe_load(text) or {}
        except ImportError:
            js = _DIR / "overlay.json"
            data = json.loads(js.read_text(encoding="utf-8")) if js.is_file() else {}
    else:
        data = json.loads(text)
    data.setdefault("edges", [])
    return data


def edges_covering(symbols: list[str], overlay: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    """符号命中 edge.covers / from / to 任一片段。"""
    ov = overlay if overlay is not None else load_overlay()
    hits: list[dict[str, Any]] = []
    needles = [s for s in symbols if s]
    for e in ov.get("edges") or []:
        blob = " ".join(
            [
                str(e.get("from") or ""),
                str(e.get("to") or ""),
                str(e.get("via") or ""),
                " ".join(str(x) for x in (e.get("covers") or [])),
            ]
        )
        if any(n in blob for n in needles):
            hits.append(e)
    return hits


def format_callers_block(symbol: str, overlay: dict[str, Any] | None = None) -> str:
    """合成 callers 文本：stitch 入边（to/covers 命中 symbol）。"""
    ov = overlay if overlay is not None else load_overlay()
    lines = []
    for e in ov.get("edges") or []:
        covers = [str(x) for x in (e.get("covers") or [])]
        to = str(e.get("to") or "")
        if symbol in to or any(symbol in c or c in symbol for c in covers) or symbol in str(e.get("from") or ""):
            frm = e.get("from")
            kind = e.get("kind")
            eid = e.get("id")
            lines.append(f"  [stitch:{kind}] {frm} → {e.get('to')}  ({eid})")
    if not lines:
        return ""
    return "Stitch overlay callers\n" + "\n".join(lines)


def stitch_covers_symbols(symbols: list[str], overlay: dict[str, Any] | None = None) -> bool:
    return bool(edges_covering(symbols, overlay))
