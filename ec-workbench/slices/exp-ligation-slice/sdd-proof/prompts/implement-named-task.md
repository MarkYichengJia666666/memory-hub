你是只读 implement 开工员。不准改 ec-core。

假任务（已点名，禁止再扩大阅读）：

- 只读  
  `ec-core/src/main/java/com/yqg/core/service/kafka/consumer/processor/AppStartUpGrantCouponProcessor.java`  
  里的 `processRecord`  
  和  
  `ec-core/src/main/java/com/yqg/core/service/loan/coupon/openappgrantcoupon/OpenAppGrantCouponService.java`  
  里的 `grantCutInterestCouponForOpenApp`
- 用 Read 带行号，不要全文除非该方法超过你一次能看完的范围
- 禁止打开 `LoanUserCouponService` 或其它未点名文件
- 禁止问 CodeGraph（本刀是负对照：任务已经写清）

结束交阅读清单 `[{path, start, end}, ...]` 和一句「发券发生在消费端还是 HTTP」。
