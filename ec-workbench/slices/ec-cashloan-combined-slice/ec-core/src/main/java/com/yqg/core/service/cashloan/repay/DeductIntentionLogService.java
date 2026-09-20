package com.yqg.core.service.cashloan.repay;

import com.yqg.core.model.sql.cashloan.DeductIntentionLogModel;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanInstalmentService;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repay.vo.DeductIntentionVO;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentPlanVO;
import com.yqg.core.service.cashloan.util.rate.CalcFeeUtil;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.orderview.CashLoanInstalmentViewVO;
import com.yqg.core.service.jbp.goldencard.JbpCardService;
import com.yqg.core.service.jbp.goldencard.vo.JbpOrderVO;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.EcAsserts;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeductIntentionLogService {
  @Autowired
  private DeductIntentionLogModel deductIntentionLogModel;
  @Autowired
  private CashLoanInstalmentService cashLoanInstalmentService;
  @Autowired
  private JbpCardService jbpCardService;
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService instalmentCutInterestCouponDeductDetailService;

  public DeductIntentionVO findLatestDeductIntention(Long userId) {
    return DeductIntentionVO.from(deductIntentionLogModel.fetchLatestDeductIntention(userId));
  }

  // 辅助方法2: 计算各类型应还金额
  public Map<UnionRepaymentType, BigDecimal> calculateOwedAmounts(List<UnionRepaymentPlanVO> repaymentPlans) {
    Map<UnionRepaymentType, BigDecimal> owedAmountMap = new HashMap<>();
    repaymentPlans.forEach(plan ->
        owedAmountMap.put(plan.unionRepaymentType,
            queryOwedAmountByBusinessIds(plan.unionRepaymentType, plan.businessIds))
    );
    return owedAmountMap;
  }


  //根据输入的合并还款type，以及businessId去查询实时欠款金额
  private BigDecimal queryOwedAmountByBusinessIds(UnionRepaymentType unionRepaymentType, List<Long> businessIds) {
    switch (unionRepaymentType) {
      case EC_REPAY:
        List<CashLoanInstalmentVO> listByInstalmentIds = cashLoanInstalmentService.findListByInstalmentIds(businessIds);
        List<CashLoanInstalmentViewVO> viewOrderInstalments = instalmentCutInterestCouponDeductDetailService.getViewInstalments(listByInstalmentIds);
        return CalcFeeUtil.getOwedAmount(viewOrderInstalments.stream()
            .filter(
            CashLoanInstalmentVO::needRepay)
            .collect(Collectors.toList()));
      case JBP:
        EcAsserts.assertTrue(businessIds.size() == 1, "JBP OrderIds can only equals to 1! businessIds:{}", JsonUtils.toString(businessIds));
        JbpOrderVO jbpOrderVO = jbpCardService.queryByJbpOrder(businessIds.get(0));
        return jbpOrderVO.getNeedPayAmount();
      default:
        throw EcException.error("Unsupported UnionRepaymentType! unionRepaymentType:{}, businessIds:{}", unionRepaymentType, JsonUtils.toString(businessIds));
    }
  }

}
