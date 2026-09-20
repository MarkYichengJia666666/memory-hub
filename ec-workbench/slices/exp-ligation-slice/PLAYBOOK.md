# EC 上怎么用这张 CodeGraph

图只答「谁连着谁」。实验还活不活问生产实验 MCP。Job 有没有挂调度、topic 有没有配进 `kafka.topic_config`，图都不知道。

**一仓一图（已合并）**：业务仍按嘴收集进各 slice，再并进合并仓建唯一一张图。

| 角色 | 路径 / id |
|---|---|
| 合并切片仓 | `ec-cashloan-combined-slice`（五业务源码并集） |
| 唯一图 id | `cg-zp42c34n`（712 files / ~23k nodes） |
| 源切片（只作补文件素材，不再单独建图） | `exp-ligation-slice` / `cashloan-order-slice` / `cashloan-repay-slice` / `cashloan-risk-slice` / `cashloan-bindcard-slice` |

问任何业务（开屏发券、首页、还款、风控、绑卡）都用 `cg-zp42c34n`。

收集一条业务时，按**服务入口**补文件，不按实验 key。C 端、internal-api、admin、kafka-consumer、scheduler 各是一张嘴。没有实验也要带。

## 入口（资源位 / 开屏发券）

资源位 / 开屏发券在 EC 里没有 `*Job`。统一发券任务有：`CouponTaskGrantJob`。

### 资源位

C 端、运营后台、internal-api 都在切片里。C 端不直接调 `checkStrategy`：先去营销中心，MC 再回调 internal-api。

```
C 端 GET /api/pageConfig | POST /api/appResourceV2     [图：route]
  → GeneralPageConfigController                        [图]
  → AppResourceManagerService.getResourceFromMc        [图]
  → IAppResourceService.getAppResource                 [手工：MC 客户端，不在切片]

首页 banner 另一张嘴：
  BannerHomePopupProcessor / IBannerParamCreateService
  → getResourceFromMc                                  [图]

MC 回调：
  POST /ecInternalApi/checkStrategy                    [图：route]
  → StrategyCheckController → 过滤链（见下方 817）

其它服务怎么打 internal-api：
  IEStrategyCheckService（Feign，ec-client-spring）    [图有接口，切片里没有调用方]

运营配置（不是运行时过滤）：
  POST /admin/operation/general/filter/rule            [图：route]
  → GeneralPageConfigFilterController.createRule
  → FilterRuleService.create
```

### 开屏发券

```
C 端 POST /api/realTimeEvent/openApp                   [图：route]
  → AppStartupService.processAppStartupEvent           [图]
  → sendAppStartupEvent
  → IKafkaMessageService.schedule(APP_STARTUP_...)     [图到接口]

另一张 C 端嘴：
  POST /api/app/startup → AppController.startup
  → processAppStartupEvent                             [图]

消费应用拉起（配置 + 反射，图上没有边）：
  ConsumerPoolService.run
  → iSiteVars.getString("kafka.topic_config")          [手工]
  → SpringUtils.getInstance(context.getProcessorClass()) [手工]
  → AppStartUpGrantCouponConsumerGroupContext
       .getProcessorClass() = AppStartUpGrantCouponProcessor.class  [手工：返回 Class，不是调用]
  → processRecord → 发券（见下方灌券）
```

`openApp` 里 `buildOpenAppGrantCouponParam` 只组参，真正发券在 Kafka 消费。`publishSystemEvent` 是另一条去 MC 的通知，不是这根 Kafka。

跑一遍：`python3 tools/query_entry_mouths.py`

SDD explore / tasks / implement 要证「图有没有增益」，协议在 `sdd-proof/PROOF.md`。刀 1 少读记在下方「优点」，不要和代谢的 26.8% / 20.6% 混谈。

## 优点（有了图之后能做什么）

1. **少翻仓库**  
   先问谁连着谁，再只打开图指到的文件。  
   测过一次（开屏发券）：大约少读五分之四。没有更快。记分没赢，少读仍算有。不要和代谢的百分数混谈。

2. **把门问全**  
   App、内部调用、消息队列、定时任务都能问到，不用只盯着你记得的那个网页接口。

3. **把长得像的分开**  
   名字很像的接口或任务，问它连不连到真正干活的那个方法。连不上，就当另一件事。中间共用的代码还是会缠在一起，分不干净。

4. **知道可以停了**  
   问完「谁调用了这个方法」，只有一处就可以停，不用再把仓库翻下去猜有没有漏的。

5. **下次还能接着问**  
   图会留着。换人、换一次聊天，不用从头搜。复用还没测。

6. **改之前看还有谁用**  
   避免只改了一处，漏了队列或定时任务。

7. **认清「没连线」**  
   图上找不到连线，有时只是配置或反射，不是没人用，不能当死代码删。

8. **只凭方法名也能找**  
   不知道文件在哪，也能问出位置和谁在用。

做不到：任务里已经写明改哪个文件时，不能再少读。图自己不管实验还活不活、定时任务挂没挂、队列配没配。

## 活不活（图旁边另做，不要做进图里）

「这段实验代码 / 这个分支还活不活」要四层对，单独用哪一层都会骗人。

| 问的是 | 谁回答 |
|---|---|
| 代码还连得上吗 | 图 |
| 实验开着、还有流量吗 | 实验平台 |
| 最近还有人进组吗 | 实验平台近 20 条入组 |
| 这段 Java / 这个 Job 真的跑过吗 | 线上日志、调度最后一次触发 |

实验平台（详情 + 入组）和日志查询已经有。缺的是调度：Job 挂没挂、上次几点跑。  
先接实验入组 + 日志搜 key，能覆盖大部分「实验还活不活」。调度用来判断定时任务还在不在跑，判断不了里面哪段实验分支。  
对不上就标不确定，别自动删。流量很小、分支没打日志、Job 在跑但走对照组，都会像死的。全量后的默认路径一直有日志，那是产品路径，不是实验还开着。

### 统一发券任务（第一条带 Job 的链）

`CouponTaskGrantJob` 轮询 `notif_coupon_grant_task`。调度平台怎么 cron 到这个类，图不知道。

```
调度平台触发 BaseJob.doExecute                         [手工：平台 + 父类在依赖包]
  → YqgBaseJob.doExecute → doTask → exec               [图：callers 落在抽象 exec]
  → CouponTaskGrantJob.exec                            [图：实现上显示无 callers，和接口实现同一类坑]
  → listByStatusAndLimit + batchDoTask                 [图]
  → doConsume → doGrantCoupon                          [图：同文件私有方法]
  → LoanUserCouponService / FinancingUserCouponService [手工：冻结类，没进切片]
```

另一张嘴是同步发，不经过 Job 排队：

```
POST /ecInternalApi/couponGrantRule/grantCoupon        [图：route]
  → CouponGrantRuleController.doGrantCoupon
  → CouponGrantService.grantCouponByRuleId             [图]
  → grantCouponInstant → createTask + doConsume        [图]
```

admin `GET /admin/operation/loan/coupon/task/list` 只查任务，不执行。

`createTask` 的生产调用只来自 `grantCouponInstant`。internal-api 是当下发完，不是「先入库等 Job」。Job 自己的 `batchDoTask` 里还有「停止该 job 后续删除」的注释。图能证明 Job 调了谁，不能证明线上还在跑。

跑一遍：`python3 tools/query_job_grant_chain.py`

## 活不活（先于查图）

对每个平台 key 调 `ExperimentController_get(name=<key>, needReopen=true)`。看 `experimentStatus` 和各组 `percentage`。重开实验是另一条 `name`，代码里的 key 对不上就不算同一根血管。

## 查之前先换名字

平台 key、配置码，图都搜不到。先换成 Java 符号，表在 `KEYS.md`。

例：`Rayakan_Kemerdekaan` → `NATIONAL_DAY_817_FIRST_LOAN` 或直接问 `isAtmosphereGroup`。

同名方法会有多份定义（接口/实现、两个 `hitStrategy`）。看 callers 时认文件路径，别数条数。import、字段别当调用点。

## 工具怎么挑

| 你想问 | 用 |
|---|---|
| 这个名字在哪 | search（符号名） |
| 谁调用它 / 它调谁 | callers / callees |
| 改它碰谁 | impact |
| 把相关源码打包给 Agent | explore（人别当浏览器用） |

HTTP 入口可以 search `kind=route`，例如 `POST /ecInternalApi/checkStrategy`。

## 图穿不过去的两截（EC 写法）

1. **资源位 Aviator**：`hitStrategy` 里 `AviatorEvaluator.execute(表达式)`。表达式里的 `rule(id)` 运行时才进 `FilterRuleFunction.call`。`addFunction` 在图上没有边。
2. **按枚举找 processor**：`getProcessor(ruleVO.type)` 返回基类，再 `processor.hitRule(...)`。图能连到基类 `hitRule`，连不到 817 那一个。切片里 Factory.init 能看到 `getRuleType`（只有一个 processor）；全仓会有一长串，不能当「checkStrategy 调用了 817」。

接口 `abTestByUserId` 有调用方；实现类那份显示没有调用方。Spring 注入的是接口。

## 817 首页 banner 这条链（对过图 + 源码）

```
POST /ecInternalApi/checkStrategy          [图：route]
  → StrategyCheckController.checkStrategy  [图]
  → getCheckStrategyResult                 [图]
  → FilterStrategyService.hitStrategy      [图]
  → Aviator execute("rule(id)")            [手工：不是方法调用]
  → FilterRuleFunction.call                [图：再往下 hitRule]
  → FilterRuleService.hitRule
  → getProcessor(ND817HB)                  [手工：枚举 → 817 processor]
  → NationalDay817HomeBannerFilterRuleProcessor.hitRule
  → isAtmosphereGroup                      [图]
  → IExperimentAdapter.abTestByUserId      [图]
  → ExperimentAdapter.abTestByUserId       [手工：接口实现]
```

下单页氛围不走 Aviator：`ExpInfoDisplayDataBuilder` 直接调 `isAtmosphereGroup`。

没有 Job 调 817（这条链本身就没有 Job）。

跑一遍：`python3 tools/query_banner_chain.py`

## 复贷灌券（说明书第二问）

v1 key 换成 `RELOAN_TOP_LOW_WILL_COUPON` 之后，callers 应只有版本表，没有发放。活着的是枚举 `RELOAN_TOP_WILLING_COUPON_V2`。

```
Kafka AppStartUpGrantCouponProcessor.processRecord   [图]
  → OpenAppGrantCouponService.grantCutInterestCouponForOpenApp
  → resolveReloanHeadCutInterestCouponIds
  → ExpDiversionClient.getResult(V2 key)             [图，方法在父类 AbstractExpClient]
```

C 端开屏 HTTP 走 `RealTimeEventController` / `AppController`，真正发券在 Kafka 消费。没有 Job。消费应用怎么拉起见上方「开屏发券」。

分流客户端和 817 不是同一套：券用 `ExpDiversionClient`，817 用 `IExperimentAdapter`。适配业务时不要假定全仓只有一种分流调用。

跑一遍：`python3 tools/query_coupon_chain.py`
