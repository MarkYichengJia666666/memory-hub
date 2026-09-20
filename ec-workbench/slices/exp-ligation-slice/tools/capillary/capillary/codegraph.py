"""CodeGraph HTTP client (Memory Hub KS)."""
from __future__ import annotations

import json
import urllib.error
import urllib.request
from typing import Any


class CodeGraphClient:
    def __init__(self, base_url: str, service_id: str = "default", timeout: int = 30):
        self.base_url = base_url.rstrip("/")
        self.service_id = service_id
        self.timeout = timeout

    def _post(self, action: str, body: dict[str, Any]) -> dict[str, Any]:
        data = json.dumps(body).encode("utf-8")
        req = urllib.request.Request(
            f"{self.base_url}/{action}",
            data=data,
            headers={
                "Content-Type": "application/json",
                "x-tdai-service-id": self.service_id,
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                return json.load(resp)
        except urllib.error.HTTPError as e:
            raw = e.read().decode("utf-8", "replace")
            raise RuntimeError(f"CodeGraph {action} HTTP {e.code}: {raw[:400]}") from e

    def text(self, action: str, code_graph_id: str, **extra: Any) -> str:
        raw = self._post(action, {"code_graph_id": code_graph_id, **extra})
        data = raw.get("data") or {}
        if isinstance(data, dict) and "text" in data:
            return str(data.get("text") or "")
        return json.dumps(raw, ensure_ascii=False)[:2000]

    def search(self, code_graph_id: str, query: str, limit: int = 8) -> str:
        return self.text("search", code_graph_id, query=query, limit=limit)

    def callers(self, code_graph_id: str, symbol: str, limit: int = 12) -> str:
        return self.text("callers", code_graph_id, symbol=symbol, limit=limit)

    def get_status(self, code_graph_id: str) -> dict[str, Any]:
        raw = self._post("get", {"code_graph_id": code_graph_id})
        return raw.get("data") or {}
