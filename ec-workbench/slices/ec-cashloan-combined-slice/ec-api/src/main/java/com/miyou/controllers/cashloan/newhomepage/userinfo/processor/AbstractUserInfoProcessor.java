package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.loanmarket.LoanMarketCardElementProvider;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.miyou.controllers.cashloan.utilities.HomePageMainCardInfoTool;
import com.miyou.controllers.cashloan.utilities.HomepageCommonTool;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.service.abtest.ABTestVersionConfigService;
import com.yqg.core.service.bizcheck.BizCheckListService;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.homepage.display.RejectedSupplier;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loanmarket.LoanMarketUserQualifyService;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.sensors.SensorsService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 每个processor都设置对应的字段
 */
public abstract class AbstractUserInfoProcessor implements IHomePageFieldProcessor<UserResponse, HomepageUserInfoProcessorType> {

  /**
   * 该类后续迁移完成将代码全部迁移至processor
   */
  @Deprecated
  @Autowired
  protected HomepageContentTool homepageContentTool;
  @Autowired
  protected ProductConfigService productConfigService;
  @Autowired
  protected HomePageMainCardInfoTool homePageMainCardInfoTool;
  @Autowired
  protected EcOrderService ecOrderService;
  @Autowired
  protected HomepageCommonTool homepageCommonTool;
  @Autowired
  protected LoanUserCouponService loanUserCouponService;
  @Autowired
  protected BizCheckListService bizCheckListService;
  @Autowired
  protected RejectedSupplier rejectedSupplier;
  @Autowired
  protected PaymentService paymentService;
  @Autowired
  protected ABTestVersionConfigService abTestVersionConfigService;
  @Autowired
  protected HomepageV5Config homepageV5Config;
  @Autowired
  protected EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  protected LoanMarketUserQualifyService loanMarketUserQualifyService;
  @Autowired
  protected SensorsService sensorsService;
  @Autowired
  protected LoanMarketCardElementProvider loanMarketCardElementProvider;

  @Override
  public HomepageUserInfoProcessorType getProcessorType() {
    return getUserInfoProcessorType();
  }

  protected abstract HomepageUserInfoProcessorType getUserInfoProcessorType();
}
