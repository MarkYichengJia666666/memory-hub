#!/usr/bin/env python3
"""复贷灌券：v1 常量是否还有发放；v2 从 Kafka 到分流。"""
import json
import urllib.request

KS = "http://127.0.0.1:8424/v3/code-graph"
CG = "cg-pl6vfmyi"
HEADERS = {
    "Content-Type": "application/json",
    "x-tdai-service-id": "default",
}


def post(action, body):
    payload = json.dumps({"code_graph_id": CG, **body}).encode()
    req = urllib.request.Request(f"{KS}/{action}", data=payload, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = json.load(resp).get("data") or {}
    return data.get("text") or ""


def line(kind, src, dst, note=""):
    tag = "图" if kind == "graph" else "手工"
    extra = f"  ({note})" if note else ""
    print(f"  [{tag}] {src}")
    print(f"       → {dst}{extra}")


def main():
    print("对照：top_willing_coupon_v1 → RELOAN_TOP_LOW_WILL_COUPON")
    raw = post("search", {"query": "top_willing_coupon_v1", "limit": 5})
    key_hit = "Search Results" in raw and "No results found" not in raw
    print("  直接搜平台 key：", "有结果" if key_hit else "空（预期）")

    const_search = post("search", {"query": "RELOAN_TOP_LOW_WILL_COUPON", "limit": 10})
    print("  搜 v1 常量：", "命中" if "RELOAN_TOP_LOW_WILL_COUPON" in const_search else "空")
    callers_v1 = post("callers", {"symbol": "RELOAN_TOP_LOW_WILL_COUPON", "limit": 20})
    no_callers = "No callers found" in callers_v1
    only_build_map = "ABTestConfig" in callers_v1 and "OpenAppGrantCoupon" not in callers_v1
    if no_callers:
        print("  v1 callers：空（常量只出现在版本表 put，图常常不建调用边）")
    elif only_build_map:
        print("  v1 callers 是否只剩版本表：是")
    else:
        print("  v1 callers 还有发放？原文：")
        print(callers_v1[:800])

    v2_search = post("search", {"query": "RELOAN_TOP_WILLING_COUPON_V2", "limit": 10})
    print("  搜 v2 枚举：", "命中" if "RELOAN_TOP_WILLING_COUPON_V2" in v2_search else "空")

    callers_v2 = post("callers", {"symbol": "RELOAN_TOP_WILLING_COUPON_V2", "limit": 20})
    callers_resolve = post("callers", {"symbol": "resolveReloanHeadCutInterestCouponIds", "limit": 10})
    callers_grant = post("callers", {"symbol": "grantCutInterestCouponForOpenApp", "limit": 10})
    callees_proc = post("callees", {"symbol": "processRecord", "limit": 20})
    callers_get = post("callers", {"symbol": "getResult", "limit": 20})

    print("\n活着的 v2 发放")
    kafka_edge = "grantCutInterestCouponForOpenApp" in callees_proc and "AppStartUpGrantCouponProcessor" in callees_proc
    line(
        "graph" if kafka_edge else "stitch",
        "AppStartUpGrantCouponProcessor.processRecord",
        "grantCutInterestCouponForOpenApp",
        "Kafka 消费，不是 Job",
    )
    line(
        "graph" if "OpenAppGrantCouponService" in callers_resolve else "stitch",
        "grantCutInterestCouponForOpenApp",
        "resolveReloanHeadCutInterestCouponIds",
    )
    v2_in_service = "OpenAppGrantCouponService" in callers_v2 or "resolveReloanHeadCutInterestCouponIds" in callers_v2
    line(
        "graph" if v2_in_service else "stitch",
        "resolveReloanHeadCutInterestCouponIds",
        "RELOAN_TOP_WILLING_COUPON_V2",
    )
    get_on_parent = "AbstractExpClient" in callers_get or "OpenAppGrantCouponService" in callers_get
    line(
        "graph" if get_on_parent else "stitch",
        "resolveReloanHeadCutInterestCouponIds",
        "ExpDiversionClient.getResult（实现在 AbstractExpClient）",
        "和 817 的 IExperimentAdapter 不是同一套",
    )

    print("\nC 端开屏 HTTP")
    callers_build = post("callers", {"symbol": "buildOpenAppGrantCouponParam", "limit": 10})
    if "RealTimeEventController" in callers_build:
        line("graph", "RealTimeEventController", "buildOpenAppGrantCouponParam", "组参发事件，不在这里发券")
    else:
        line("stitch", "RealTimeEventController", "buildOpenAppGrantCouponParam", "图上没连上")


if __name__ == "__main__":
    main()
