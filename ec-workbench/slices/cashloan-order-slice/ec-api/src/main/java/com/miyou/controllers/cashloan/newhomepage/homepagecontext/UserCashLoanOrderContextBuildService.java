package com.miyou.controllers.cashloan.newhomepage.homepagecontext;

import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserCashLoanOrderContext;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.instalmentcutcoupon.vo.InstalmentCutInterestCouponDeductDetailVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCashLoanOrderContextBuildService {
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService couponDeductDetailService;

  public UserCashLoanOrderContext buildContext(HomepageUserParamsVO paramsVO) {
    List<Long> readyOrderIds = Optional.ofNullable(paramsVO.readyOrderList).orElse(new ArrayList<>()).stream().map(item -> item.orderVO.id).collect(Collectors.toList());
    Map<Long, List<InstalmentCutInterestCouponDeductDetailVO>> initOrderDeductDetail =
        couponDeductDetailService.getInitOrderDeductDetail(readyOrderIds);
    return UserCashLoanOrderContext.fromHomeStatusConfirmContext(paramsVO.readyOrderList,
        initOrderDeductDetail,
        paramsVO.getLatestOrderVO(),
        paramsVO.getOrderRejectReasonVO());
  }
}
