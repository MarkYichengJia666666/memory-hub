"""实验状态：读缓存；可选 HTTP；MCP 结果可写入缓存。"""
from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass
class ExperimentSnapshot:
    name: str
    experiment_id: int | None = None
    status: str = "UNKNOWN"
    remain_days: float | None = None
    control_pct: float = 0.0
    experiment_pct: float = 0.0
    blank_pct: float = 0.0
    desc: str = ""
    raw: dict[str, Any] = field(default_factory=dict)

    @property
    def experiment_has_traffic(self) -> bool:
        return self.experiment_pct > 0

    @property
    def user_effective(self) -> bool:
        """对用户生效：实验组流量 > 0。"""
        return self.experiment_has_traffic


def _pct_from_bypass(bypass: list[dict[str, Any]]) -> tuple[float, float, float]:
    control = experiment = blank = 0.0
    for g in bypass or []:
        pct = float(g.get("percentage") or 0)
        gt = (g.get("groupType") or g.get("data") or "").upper()
        if "CONTROL" in gt:
            control = pct
        elif "EXPERIMENT" in gt:
            experiment = pct
        elif "BLANK" in gt:
            blank = pct
    return control, experiment, blank


def parse_mcp_body(body: dict[str, Any]) -> ExperimentSnapshot:
    """解析 ExperimentController_get 的 body（或整包里的 body）。"""
    if "body" in body and isinstance(body["body"], dict):
        body = body["body"]
    if "status" in body and "body" in body and isinstance(body.get("body"), dict):
        # {status, body} wrapper
        body = body["body"]
    bypass = body.get("bypassFlow") or []
    c, e, b = _pct_from_bypass(bypass)
    remain = body.get("remainDuration")
    remain_days = None
    if remain is not None:
        try:
            remain_days = float(remain)
        except (TypeError, ValueError):
            remain_days = None
    return ExperimentSnapshot(
        name=str(body.get("name") or ""),
        experiment_id=body.get("id"),
        status=str(body.get("experimentStatus") or "UNKNOWN"),
        remain_days=remain_days,
        control_pct=c,
        experiment_pct=e,
        blank_pct=b,
        desc=str(body.get("experimentDesc") or ""),
        raw=body,
    )


class ExperimentCache:
    def __init__(self, path: Path):
        self.path = path
        self._data: dict[str, Any] = {"experiments": {}}
        if path.exists():
            self._data = json.loads(path.read_text(encoding="utf-8"))

    def save(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.path.write_text(
            json.dumps(self._data, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def put_raw(self, name: str, payload: dict[str, Any]) -> ExperimentSnapshot:
        snap = parse_mcp_body(payload)
        if not snap.name:
            snap.name = name
        self._data.setdefault("experiments", {})[name] = {
            "name": snap.name,
            "experiment_id": snap.experiment_id,
            "status": snap.status,
            "remain_days": snap.remain_days,
            "control_pct": snap.control_pct,
            "experiment_pct": snap.experiment_pct,
            "blank_pct": snap.blank_pct,
            "desc": snap.desc,
            "raw": snap.raw,
        }
        return snap

    def get(self, name: str) -> ExperimentSnapshot | None:
        row = (self._data.get("experiments") or {}).get(name)
        if not row:
            return None
        return ExperimentSnapshot(
            name=row.get("name") or name,
            experiment_id=row.get("experiment_id"),
            status=row.get("status") or "UNKNOWN",
            remain_days=row.get("remain_days"),
            control_pct=float(row.get("control_pct") or 0),
            experiment_pct=float(row.get("experiment_pct") or 0),
            blank_pct=float(row.get("blank_pct") or 0),
            desc=row.get("desc") or "",
            raw=row.get("raw") or {},
        )
