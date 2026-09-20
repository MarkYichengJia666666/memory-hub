package com.miyou.controllers.cashloan.newhomepage.loanmarket.display;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;

/**
 * 贷超入口展示策略：仅负责“是否展示贷超入口”及对应 loanMarketRule 的判断，
 * 不包含贷超入口构建逻辑。策略判断仅使用 context 已有内容，不依赖 processor 内独有状态。
 */
public interface LoanMarketEntranceDisplayStrategy {

  LoanMarketEntranceDisplayStrategyType getType();

  /**
   * 判断当前 context 下是否应展示贷超入口及对应的 loanMarketRule。
   *
   * @param context                首页上下文
   * @param skipExperimentAndVersion 为 true 时跳过实验与版本校验，仅用风控/资格等 context 已有内容判断
   */
  LoanMarketEntranceDisplayDecision evaluate(HomePageContext context, boolean skipExperimentAndVersion);
}
