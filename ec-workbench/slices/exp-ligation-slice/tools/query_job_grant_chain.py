#!/usr/bin/env python3
"""统一发券：Job 入口 + internal-api 同步发。图边 vs 手工。"""
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


def has(text, *needles):
    return all(n in text for n in needles)


def line(kind, src, dst, note=""):
    tag = "图" if kind == "graph" else "手工"
    extra = f"  ({note})" if note else ""
    print(f"  [{tag}] {src}")
    print(f"       → {dst}{extra}")


def main():
    search_job = post("search", {"query": "CouponTaskGrantJob", "limit": 8})
    callers_exec = post("callers", {"symbol": "exec", "limit": 15})
    callees_exec = post("callees", {"symbol": "exec", "limit": 20})
    callees_do_task = post("callees", {"symbol": "doTask", "limit": 15})
    callees_do_exec = post("callees", {"symbol": "doExecute", "limit": 15})
    callers_batch = post("callers", {"symbol": "batchDoTask", "limit": 10})
    callers_list = post("callers", {"symbol": "listByStatusAndLimit", "limit": 10})
    callers_instant = post("callers", {"symbol": "grantCouponInstant", "limit": 10})
    callers_by_rule = post("callers", {"symbol": "grantCouponByRuleId", "limit": 15})
    routes_grant = post("search", {"query": "grantCoupon", "kind": "route", "limit": 15})
    routes_admin = post("search", {"query": "coupon/task", "kind": "route", "limit": 10})
    callers_create = post("callers", {"symbol": "createTask", "limit": 15})

    print("Job 入口")
    line(
        "graph" if "CouponTaskGrantJob" in search_job else "stitch",
        "ec-scheduler",
        "CouponTaskGrantJob",
    )
    line(
        "stitch",
        "调度平台",
        "YqgBaseJob.doExecute",
        "cron 不在图里；BaseJob 在依赖包",
    )
    line(
        "graph" if "exec" in callees_do_exec or "exec" in callees_do_task else "stitch",
        "YqgBaseJob.doExecute / doTask",
        "exec",
    )
    job_calls_list = "CouponTaskGrantJob" in callers_list
    job_calls_batch = "CouponTaskGrantJob" in callers_batch
    line(
        "graph" if job_calls_list else "stitch",
        "CouponTaskGrantJob.exec",
        "listByStatusAndLimit",
    )
    line(
        "graph" if job_calls_batch else "stitch",
        "CouponTaskGrantJob.exec",
        "batchDoTask",
    )
    consume_in_callees = "doConsume" in post("callees", {"symbol": "batchDoTask", "limit": 15})
    line(
        "graph" if consume_in_callees else "stitch",
        "batchDoTask",
        "doConsume",
        "同文件私有方法",
    )
    line(
        "stitch",
        "doGrantCoupon",
        "LoanUserCouponService / FinancingUserCouponService",
        "冻结类没进切片",
    )

    print("\ninternal-api 同步发（不排队给 Job）")
    line(
        "graph" if "/ecInternalApi/couponGrantRule/grantCoupon" in routes_grant else "stitch",
        "HTTP",
        "POST /ecInternalApi/couponGrantRule/grantCoupon",
    )
    line(
        "graph" if "CouponGrantRuleController" in callers_by_rule else "stitch",
        "CouponGrantRuleController.doGrantCoupon",
        "grantCouponByRuleId",
    )
    line(
        "graph" if "CouponGrantService" in callers_instant else "stitch",
        "grantCouponByRuleId",
        "grantCouponInstant",
    )
    create_from_instant = "grantCouponInstant" in callers_create or "NotifCouponGrantTaskService" in callers_create
    line(
        "graph" if create_from_instant else "stitch",
        "grantCouponInstant",
        "createTask",
        "生产插入任务只走这一处",
    )

    print("\nadmin")
    line(
        "graph" if "/admin/operation/loan/coupon/task/list" in routes_admin else "stitch",
        "HTTP",
        "GET /admin/operation/loan/coupon/task/list",
        "只查，不执行",
    )

    print("\nexec 的 callers（认路径，别数条数）")
    print(callers_exec[:600] if callers_exec else "  (空)")


if __name__ == "__main__":
    main()
