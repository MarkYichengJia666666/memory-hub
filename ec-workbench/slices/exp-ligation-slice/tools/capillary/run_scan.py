#!/usr/bin/env python3
"""跑一轮毛细血管体检，写出 reports/。"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))

from capillary.report import write_reports
from capillary.scan import scan


def main() -> int:
    parser = argparse.ArgumentParser(description="Capillary scan")
    parser.add_argument(
        "--config",
        default=str(ROOT / "config.yaml"),
        help="config.yaml path",
    )
    args = parser.parse_args()
    config_path = Path(args.config)
    findings = scan(config_path, ROOT)
    md, js = write_reports(findings, ROOT / "reports")
    print(f"wrote {md}")
    print(f"wrote {js}")
    grades: dict[str, int] = {}
    for f in findings:
        grades[f.grade] = grades.get(f.grade, 0) + 1
    for g, n in sorted(grades.items()):
        print(f"  {g}: {n}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
