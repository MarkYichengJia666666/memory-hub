package com.yqg.core.service.cashloan.repay.strategy;

import com.yqg.core.service.cashloan.repay.vo.RepaySplitUnitVO;

import java.util.List;

/**
 * 还款验证策略接口
 */
public interface RepaymentValidationStrategy {
  /**
   * 验证EC还款单元和其他还款单元
   *
   * @param ecUnits    EC还款单元列表
   * @param otherUnits 其他还款单元列表
   * @param transNo    交易号
   */
  void validate(List<RepaySplitUnitVO> ecUnits, List<RepaySplitUnitVO> otherUnits, String transNo);
}