#!/usr/bin/env python3
"""817 首页 banner → 分流。图上的边用 KS callers/callees；穿不过的用手工标注。"""
import json
import urllib.request

KS = "http://127.0.0.1:8424/v3/code-graph"
CG = "cg-pl6vfmyi"
HEADERS = {
    "Content-Type": "application/json",
    "x-tdai-service-id": "default",
}

# 平台 key → 图能搜的符号（见 KEYS.md）
KEY_TO_SYMBOL = {
    "first_loan_product_26h1-lending-abroad-loan-Rayakan_Kemerdekaan": "NATIONAL_DAY_817_FIRST_LOAN",
    "reloan_order_26h1-lending-abroad-loan_all-Rayakan_Kemerdekaan": "NATIONAL_DAY_817_RELOAN",
}


def post(action, body):
    payload = json.dumps({"code_graph_id": CG, **body}).encode()
    req = urllib.request.Request(f"{KS}/{action}", data=payload, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = json.load(resp).get("data") or {}
    return data.get("text") or ""


def has(text, *needles):
    return all(n in text for n in needles)


def line(kind, src, dst, note=""):
    tag = "图" if kind == "graph" else "手工"
    extra = f"  ({note})" if note else ""
    print(f"  [{tag}] {src}")
    print(f"       → {dst}{extra}")


def main():
    print("对照：Rayakan_Kemerdekaan →", KEY_TO_SYMBOL["first_loan_product_26h1-lending-abroad-loan-Rayakan_Kemerdekaan"])
    raw = post("search", {"query": "Rayakan_Kemerdekaan", "limit": 5})
    key_hit = "Search Results" in raw and "No results found" not in raw
    print("  直接搜平台 key：", "有结果" if key_hit else "空（预期）")
    enum_hit = post("search", {"query": "NATIONAL_DAY_817_FIRST_LOAN", "limit": 5})
    print("  搜枚举名：", "命中" if "NATIONAL_DAY_817_FIRST_LOAN" in enum_hit else "空")

    routes = post("search", {"query": "checkStrategy", "kind": "route", "limit": 10})
    callers_cs = post("callers", {"symbol": "getCheckStrategyResult", "limit": 10})
    callees_gcsr = post("callees", {"symbol": "getCheckStrategyResult", "limit": 20})
    callees_hit = post("callees", {"symbol": "hitStrategy", "limit": 20})
    callers_rule_svc = post("callers", {"symbol": "hitRule", "limit": 20})
    callers_817 = post(
        "callers",
        {"symbol": "isAtmosphereGroup", "limit": 20},
    )
    callees_atm = post("callees", {"symbol": "isAtmosphereGroup", "limit": 20})
    callers_ab = post("callers", {"symbol": "abTestByUserId", "limit": 20})

    print("\n首页 banner → 分流")
    line(
        "graph" if "POST /ecInternalApi/checkStrategy" in routes else "stitch",
        "HTTP",
        "POST /ecInternalApi/checkStrategy",
    )
    line(
        "graph" if has(callers_cs, "checkStrategy", "StrategyCheckController") else "stitch",
        "StrategyCheckController.checkStrategy",
        "getCheckStrategyResult",
    )
    line(
        "graph" if has(callees_gcsr, "hitStrategy", "GeneralPageConfigFilterStrategyService") else "stitch",
        "getCheckStrategyResult",
        "FilterStrategyService.hitStrategy",
    )
    aviator_on_graph = "AviatorEvaluator" in callees_hit or "execute" in callees_hit
    line(
        "graph" if aviator_on_graph else "stitch",
        "hitStrategy",
        'AviatorEvaluator.execute("rule(id)")',
        "表达式运行时才进 rule()",
    )
    line(
        "graph" if has(callers_rule_svc, "FilterRuleFunction", "call") else "stitch",
        "FilterRuleFunction.call",
        "FilterRuleService.hitRule",
    )
    line(
        "stitch",
        "hitRule + getProcessor(ND817HB)",
        "NationalDay817HomeBannerFilterRuleProcessor.hitRule",
        "多态，图只连到基类",
    )
    line(
        "graph" if "NationalDay817HomeBannerFilterRuleProcessor" in callers_817 else "stitch",
        "817 processor.hitRule",
        "isAtmosphereGroup",
    )
    line(
        "graph" if "abTestByUserId" in callees_atm and "IExperimentAdapter" in callees_atm else "stitch",
        "isAtmosphereGroup",
        "IExperimentAdapter.abTestByUserId",
    )
    impl_no_callers = "ExperimentAdapter.java:41" in callers_ab and "(no callers)" in callers_ab
    line(
        "stitch" if impl_no_callers else "graph",
        "IExperimentAdapter.abTestByUserId",
        "ExperimentAdapter.abTestByUserId",
        "调用落在接口上",
    )

    print("\n下单页（不走 Aviator）")
    if "ExpInfoDisplayDataBuilder" in callers_817:
        line("graph", "ExpInfoDisplayDataBuilder.assembleNationalDay817Expe", "isAtmosphereGroup")
    else:
        line("stitch", "下单页 builder", "isAtmosphereGroup", "图上没看到")


if __name__ == "__main__":
    main()
