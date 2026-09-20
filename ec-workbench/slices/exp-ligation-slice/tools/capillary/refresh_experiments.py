#!/usr/bin/env python3
"""把实验 MCP / JSON 写入 state/experiment_cache.json。

Cron 若没有 HTTP，可在 Cursor 里查完实验后：
  把结果保存成 state/experiment_live.json（数组或 {name: payload}）
  再 python3 refresh_experiments.py --from-json state/experiment_live.json
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))

from capillary.experiment import ExperimentCache


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default=str(ROOT / "config.yaml"))
    parser.add_argument(
        "--from-json",
        help="MCP 原始结果文件：list[{name,body|...}] 或 {key: payload}",
    )
    args = parser.parse_args()
    cfg = yaml.safe_load(Path(args.config).read_text(encoding="utf-8"))
    cache = ExperimentCache(ROOT / cfg["experiment"]["cache_path"])

    if not args.from_json:
        print(
            "请提供 --from-json <file>。\n"
            "在 Cursor 里对每条 key 调 ExperimentController_get(needReopen=true)，\n"
            "把返回存进 JSON 后再跑本命令。HTTP 直连以后再接。",
            file=sys.stderr,
        )
        return 2

    raw = json.loads(Path(args.from_json).read_text(encoding="utf-8"))
    count = 0
    if isinstance(raw, list):
        for item in raw:
            name = item.get("name") or item.get("platform_key")
            payload = item.get("payload") or item.get("body") or item
            if not name:
                continue
            cache.put_raw(name, payload if isinstance(payload, dict) else item)
            count += 1
    elif isinstance(raw, dict):
        # either single experiment body or map
        if "experimentStatus" in raw or "bypassFlow" in raw or "body" in raw:
            name = raw.get("name") or (raw.get("body") or {}).get("name")
            if not name:
                print("单条 payload 缺 name", file=sys.stderr)
                return 2
            cache.put_raw(name, raw)
            count = 1
        else:
            for name, payload in raw.items():
                if isinstance(payload, dict):
                    cache.put_raw(name, payload)
                    count += 1
    cache.save()
    print(f"cached {count} experiments → {cache.path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
