#!/usr/bin/env python3
"""资源位 / 开屏发券：各服务入口。图上的边用 callers/callees；配置和跨服务用手工。"""
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
    routes_page = post("search", {"query": "pageConfig", "kind": "route", "limit": 15})
    routes_check = post("search", {"query": "checkStrategy", "kind": "route", "limit": 10})
    routes_admin = post("search", {"query": "filter/rule", "kind": "route", "limit": 10})
    routes_open = post("search", {"query": "openApp", "kind": "route", "limit": 10})
    routes_startup = post("search", {"query": "startup", "kind": "route", "limit": 10})

    callers_mc = post("callers", {"symbol": "getResourceFromMc", "limit": 20})
    callers_proc = post("callers", {"symbol": "processAppStartupEvent", "limit": 15})
    callees_proc = post("callees", {"symbol": "processAppStartupEvent", "limit": 15})
    callees_send = post("callees", {"symbol": "sendAppStartupEvent", "limit": 15})
    callers_sched = post("callers", {"symbol": "schedule", "limit": 20})
    callers_gpc = post("callers", {"symbol": "getProcessorClass", "limit": 15})
    search_pool = post("search", {"query": "ConsumerPoolService", "limit": 5})
    search_job = post("search", {"query": "YqgBaseJob", "limit": 5})
    search_feign = post("search", {"query": "IEStrategyCheckService", "limit": 8})

    print("资源位入口")
    line(
        "graph" if "/api/pageConfig" in routes_page else "stitch",
        "HTTP",
        "GET /api/pageConfig",
        "C 端 ec-api",
    )
    line(
        "graph" if "GeneralPageConfigController" in callers_mc else "stitch",
        "GeneralPageConfigController",
        "getResourceFromMc",
    )
    line(
        "graph" if "BannerHomePopupProcessor" in callers_mc or "IBannerParamCreateService" in callers_mc else "stitch",
        "首页 banner processor / IBannerParamCreateService",
        "getResourceFromMc",
    )
    line(
        "stitch",
        "getResourceFromMc",
        "IAppResourceService.getAppResource（MC）",
        "客户端不在切片；MC 再回调 checkStrategy",
    )
    line(
        "graph" if "POST /ecInternalApi/checkStrategy" in routes_check else "stitch",
        "HTTP",
        "POST /ecInternalApi/checkStrategy",
        "internal-api",
    )
    line(
        "graph" if "IEStrategyCheckService" in search_feign else "stitch",
        "IEStrategyCheckService",
        "Feign 声明 checkStrategy",
        "切片里没有调用方",
    )
    line(
        "graph" if "/admin/operation/general/filter/rule" in routes_admin else "stitch",
        "HTTP",
        "POST /admin/operation/general/filter/rule",
        "ec-admin 配置，不是运行时过滤",
    )

    print("\n开屏发券入口")
    line(
        "graph" if "/api/realTimeEvent/openApp" in routes_open else "stitch",
        "HTTP",
        "POST /api/realTimeEvent/openApp",
    )
    line(
        "graph" if "RealTimeEventController" in callers_proc else "stitch",
        "RealTimeEventController.openApp",
        "processAppStartupEvent",
    )
    line(
        "graph" if "/api/app/startup" in routes_startup else "stitch",
        "HTTP",
        "POST /api/app/startup",
    )
    line(
        "graph" if "AppController" in callers_proc else "stitch",
        "AppController.startup",
        "processAppStartupEvent",
    )
    send_on_graph = "sendAppStartupEvent" in callees_proc
    line(
        "graph" if send_on_graph else "stitch",
        "processAppStartupEvent",
        "sendAppStartupEvent",
    )
    sched_on_send = "schedule" in callees_send or "IKafkaMessageService" in callees_send
    line(
        "graph" if sched_on_send else "stitch",
        "sendAppStartupEvent",
        "IKafkaMessageService.schedule",
        "实现类不在切片",
    )
    line(
        "graph" if "ConsumerPoolService" in search_pool else "stitch",
        "ec-kafka-consumer",
        "ConsumerPoolService.run",
    )
    line(
        "stitch",
        "ConsumerPoolService.run",
        "kafka.topic_config → getProcessorClass → new Processor",
        "配置 + Spring 反射，无调用边",
    )
    ctx_returns = "AppStartUpGrantCouponProcessor" in post(
        "callees", {"symbol": "getProcessorClass", "limit": 10}
    ) or "AppStartUpGrantCouponProcessor" in post(
        "search", {"query": "AppStartUpGrantCouponConsumerGroupContext", "limit": 5}
    )
    line(
        "graph" if ctx_returns else "stitch",
        "AppStartUpGrantCouponConsumerGroupContext.getProcessorClass",
        "AppStartUpGrantCouponProcessor.class",
        "返回 Class，不是调用 processRecord",
    )
    if "No callers found" in callers_gpc or "ConsumerPoolService" not in callers_gpc:
        line(
            "stitch",
            "getProcessorClass",
            "ConsumerPoolService",
            "反向也没有边，符合预期",
        )

    print("\nJob")
    job_hit = "Search Results" in search_job and "No results found" not in search_job
    if job_hit:
        print("  切片里搜到了 YqgBaseJob（预期没有）")
        print(search_job[:400])
    else:
        print("  [事实] 这两条链没有 *Job，切片里也没有 scheduler 文件")


if __name__ == "__main__":
    main()
