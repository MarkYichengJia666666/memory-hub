package com.miyou.controllers.cashloan.newhomepage.loanmarket.display;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 贷超入口展示策略聚合服务。 按当前请求的 display_status 选出需执行的策略，执行后聚合结果； 用于 processor 内“是否展示贷超”的统一调用，以及资源位请求时传入（展示贷超、loanMarketRule）。
 */
@Slf4j
@Service
public class LoanMarketEntranceDisplayStrategyService {

  private final Map<LoanMarketEntranceDisplayStrategyType, LoanMarketEntranceDisplayStrategy> strategyMap;

  @Autowired
  public LoanMarketEntranceDisplayStrategyService(List<LoanMarketEntranceDisplayStrategy> strategies) {
    this.strategyMap = strategies.stream()
        .collect(Collectors.toMap(LoanMarketEntranceDisplayStrategy::getType, Function.identity()));
  }


  /**
   * 按“单次请求中命中的 processor 对应策略”执行并聚合，用于请求资源位时表达「展示贷超入口的可能性」。 当 skipExperimentAndVersion=true 时，跳过版本与实验判断，仅按风控/context 判断“是否可能展示贷超入口”，
   * 供资源位侧使用 DISPLAY_LOAN_MARKET、LOAN_MARKET_RULE 表达该可能性。
   */
  public LoanMarketEntranceDisplayDecision getDecisionMatchAll(HomePageContext context, boolean skipExperimentAndVersion) {
    if (context.getStatus() == null) {
      return LoanMarketEntranceDisplayDecision.notShow();
    }
    List<LoanMarketEntranceDisplayStrategyType> types = LoanMarketEntranceDisplayStrategyType.getSupportedStrategyTypes(context.getStatus());
    for (LoanMarketEntranceDisplayStrategyType type : types) {
      LoanMarketEntranceDisplayStrategy strategy = strategyMap.get(type);
      if (strategy == null) {
        continue;
      }
      LoanMarketEntranceDisplayDecision decision = strategy.evaluate(context, skipExperimentAndVersion);
      if (decision != null && decision.isShowLoanMarketEntrance()) {
        return decision;
      }
    }
    return LoanMarketEntranceDisplayDecision.notShow();
  }

  /**
   * 供各 processor 调用：用指定策略类型做“是否展示贷超”判断，可跳过实验与版本。
   */
  public LoanMarketEntranceDisplayDecision evaluate(HomePageContext context,
      LoanMarketEntranceDisplayStrategyType type, boolean skipExperimentAndVersion) {
    LoanMarketEntranceDisplayStrategy strategy = strategyMap.get(Objects.requireNonNull(type));
    if (strategy == null) {
      return LoanMarketEntranceDisplayDecision.notShow();
    }
    return strategy.evaluate(context, skipExperimentAndVersion);
  }
}
