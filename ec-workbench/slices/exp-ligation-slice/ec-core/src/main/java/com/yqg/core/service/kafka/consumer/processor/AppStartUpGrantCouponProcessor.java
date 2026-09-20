package com.yqg.core.service.kafka.consumer.processor;

import static com.yqg.core.configure.devconfig.DynamicThreadExecutorConfiguration.APP_START_UP_GRANT_COUPON_EXECUTOR;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.service.app.vo.AppStartupAtLeastOnceVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.coupon.festivalboost.FestivalAntiSettlementBoostService;
import com.yqg.core.service.loan.coupon.openappgrantcoupon.OpenAppGrantCouponService;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.util.scope.Scope;
import com.yqg.core.util.scope.ScopeKeyConstants;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.serialization.JsonUtils;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AppStartUpGrantCouponProcessor implements IKafkaMessageProcessor<String, String> {

  @Autowired
  private LoanAccountService loanAccountService;

  @Autowired
  private OpenAppGrantCouponService openAppGrantCouponService;

  @Autowired
  private FestivalAntiSettlementBoostService festivalAntiSettlementBoostService;

  @Resource(name = APP_START_UP_GRANT_COUPON_EXECUTOR)
  private Executor appStartUpGrantCouponExecutor;

  private Cache<Long, Boolean> userProcessedCache = CacheBuilder
      .newBuilder()
      .maximumSize(5_000L)
      .expireAfterWrite(3, TimeUnit.SECONDS)
      .concurrencyLevel(16)
      .recordStats()
      .build();

  @Override
  public void processRecord(ConsumerRecord<String, String> record) {
    log.info("AppStartUpGrantCouponProcessor processing message, key: {}, value: {}", record.key(), record.value());
    try {
      AppStartupAtLeastOnceVO event = JsonUtils.fromOrException(record.value(), AppStartupAtLeastOnceVO.class);
      Long userId = event.getUserId();
      Boolean ifPresent = userProcessedCache.getIfPresent(userId);
      if (Objects.nonNull(ifPresent) && ifPresent) {
        return;
      }
      Long build = event.getAppBuild();
      RequestClientType requestClientType = event.getRequestClientType();
      SDKType sdkType = event.getSdkType();
      if (Objects.isNull(userId) || Objects.isNull(build) || Objects.isNull(requestClientType) || Objects.isNull(sdkType)) {
        log.info("Invalid message, userId: {}, build: {}, requestClientType: {}, sdkType: {}", userId, build, requestClientType, sdkType);
        return;
      }
      if (RequestClientType.isWholeProcess(requestClientType)) {
        return;
      }
      LoanAccountVO loanAccountVO = loanAccountService.getAccountByUserIdOrNull(userId, sdkType);
      if (Objects.isNull(loanAccountVO) ) {
        log.info("User {} does not have a loan account", userId);
        return;
      }
      if (loanAccountVO.deleted) {
        log.info("User {} has been deleted.", userId);
        return;
      }

      boolean isReloan = !loanAccountVO.firstLoan();
      CompletableFuture<Void> cutInterestFuture = CompletableFuture.runAsync(
          withEventScope(event, () -> openAppGrantCouponService.grantCutInterestCouponForOpenApp(event, isReloan)),
          appStartUpGrantCouponExecutor
      );
      CompletableFuture<Void> creditsCouponFuture = CompletableFuture.runAsync(
          () -> openAppGrantCouponService.grantCreditsCouponForOpenApp(event, isReloan, loanAccountVO.id),
          appStartUpGrantCouponExecutor
      );
      CompletableFuture<Void> festivalBoostFuture = CompletableFuture.runAsync(
          () -> festivalAntiSettlementBoostService.grantForOpenApp(event, isReloan, loanAccountVO.id),
          appStartUpGrantCouponExecutor
      );
      CompletableFuture.allOf(cutInterestFuture, creditsCouponFuture, festivalBoostFuture).join();
      userProcessedCache.put(userId, true);
    } catch (Exception e) {
      log.error("AppStartUpGrantCouponProcessor processing message error, record:{}", record.value(),e);
    }
  }

  /**
   * 把发券试算任务包裹进新建 Scope，从消息 VO 回灌设备号/客户端版本/渠道来源等执行上下文，
   * 使该任务内 {@code ImpliedContextUtils} 读到的值与同步 HTTP 链路一致（plan Decision 6），
   * 从而让 {@code isQian1MaterialAttributed} / {@code isQian1Hit} 等归因判断在异步链路真正生效。
   *
   * <p>{@link Scope#runWithNewScope} 的 begin/end 严格配对在 finally 中：任务正常结束或抛出
   * 异常，Scope 均会被清理，避免线程池复用导致下一个用户的任务读到上一个用户的残留上下文
   * （TC-035）。
   *
   * @param event 消费到的开屏事件，字段本就携带回灌所需的执行上下文
   * @param task  实际要在新 Scope 内执行的发券试算逻辑
   * @return 包裹后的 Runnable，供 {@link CompletableFuture#runAsync} 提交
   */
  private Runnable withEventScope(AppStartupAtLeastOnceVO event, Runnable task) {
    return () -> Scope.runWithNewScope(() -> {
      Scope scope = Scope.getCurrentScope();
      scope.set(ScopeKeyConstants.USER_ID_SCOPE_KEY, event.getUserId());
      scope.set(ScopeKeyConstants.DEVICE_TOKEN_SCOPE_KEY, event.getDeviceToken());
      scope.set(ScopeKeyConstants.BUILD_SCOPE_KEY, event.getAppBuild());
      scope.set(ScopeKeyConstants.SOURCE_TYPE_SCOPE_KEY, event.getSourceType());
      scope.set(ScopeKeyConstants.REQUEST_CLIENT_TYPE_SCOPE_KEY, event.getRequestClientType());
      scope.set(ScopeKeyConstants.SDK_TYPE_SCOPE_KEY, event.getSdkType());
      task.run();
    });
  }

  @PreDestroy
  private void logCacheStats() {
    log.info("userProcessedCache stats:{}", userProcessedCache.stats());
  }
}
