"""分级：live / candidate_solidify / candidate_bury / manual_only / unknown。"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from .experiment import ExperimentSnapshot


@dataclass
class VesselFinding:
    vessel_id: str
    mouth_id: str
    mouth_name: str
    platform_key: str
    java_symbols: list[str]
    grade: str
    why: list[str] = field(default_factory=list)
    experiment: ExperimentSnapshot | None = None
    graph_id: str = ""
    graph_hits: dict[str, str] = field(default_factory=dict)
    safe_auto: bool = False
    enum_dispatch_mouth: bool = False
    reason_hint: str = ""
    solidify_keep_hint: str = ""

    def to_dict(self) -> dict[str, Any]:
        exp = self.experiment
        return {
            "vessel_id": self.vessel_id,
            "mouth_id": self.mouth_id,
            "mouth_name": self.mouth_name,
            "platform_key": self.platform_key,
            "java_symbols": self.java_symbols,
            "grade": self.grade,
            "why": self.why,
            "graph_id": self.graph_id,
            "safe_auto": self.safe_auto,
            "enum_dispatch_mouth": self.enum_dispatch_mouth,
            "reason_hint": self.reason_hint,
            "solidify_keep_hint": self.solidify_keep_hint,
            "experiment": None
            if not exp
            else {
                "name": exp.name,
                "id": exp.experiment_id,
                "status": exp.status,
                "remain_days": exp.remain_days,
                "control_pct": exp.control_pct,
                "experiment_pct": exp.experiment_pct,
                "blank_pct": exp.blank_pct,
                "desc": exp.desc,
                "user_effective": exp.user_effective,
            },
            "graph_hits": self.graph_hits,
        }


def classify(
    exp: ExperimentSnapshot | None,
    *,
    enum_dispatch_mouth: bool,
    graph_found_any: bool,
    callers_empty_for_all: bool,
) -> tuple[str, list[str]]:
    why: list[str] = []
    if exp is None:
        return "unknown", ["实验缓存里没有这条 key，先 refresh 实验状态"]

    st = (exp.status or "").upper()
    why.append(f"平台状态={st}")
    why.append(
        f"流量：对照 {exp.control_pct}% / 实验 {exp.experiment_pct}% / 空白 {exp.blank_pct}%"
    )
    if exp.remain_days is not None:
        why.append(f"剩余约 {exp.remain_days} 天")
    if exp.desc:
        why.append(f"描述：{exp.desc}")

    # 全量优先：行为已固化到实验组 → 可进「去门闸、留赢家」修订候选
    if st == "FULL_PERCENTAGE":
        if not graph_found_any:
            why.append("已全量，但图上几乎搜不到符号 → 只人工看，补切片或核对称号")
            return "manual_only", why
        why.append(
            "已全量：建议 revision——去掉分流门闸，保留实验组赢家行为"
            "（需人确认；不是当死代码整枝删）"
        )
        return "candidate_solidify", why

    # 仍在跑 / 实验组有流量（非全量）
    if st in {"RUNNING", "PREPARE"} and exp.experiment_pct > 0:
        why.append("实验组仍有流量或在跑 → 不管")
        return "live", why
    if exp.user_effective:
        why.append("实验组仍有流量 → 不管")
        return "live", why

    if st in {"ZERO_PERCENTAGE", "CLOSE", "LIGHT"} and not exp.user_effective:
        if enum_dispatch_mouth and callers_empty_for_all:
            why.append("这张嘴有枚举/反射洞，callers 空不能当死代码 → 只人工看")
            return "manual_only", why
        if not graph_found_any:
            why.append("关量/结束，但图上几乎搜不到符号 → 先别自动删，补切片或核对称号")
            return "manual_only", why
        why.append("关量/结束且对用户不生效，图上能定位到符号 → 可进删除候选（需人确认）")
        return "candidate_bury", why

    why.append(f"未覆盖的状态组合 status={st}，标未知")
    return "unknown", why
