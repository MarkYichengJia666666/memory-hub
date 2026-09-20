package com.miyou.controllers.cashloan.utilities;

import com.miyou.controllers.cashloan.enums.IncreaseCreditsEntranceDisplayLocation;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.enums.ABTestParentGroupType;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageParamTool;
import com.yqg.core.service.cashloan.vo.enums.DiscountDetailTopAreaVipUIDisplayStrategy;
import com.yqg.core.service.loan.extrainfo.LoanAccountExtraInfoService;
import com.yqg.core.service.loan.extrainfo.enums.ExtraInfoCollectSource;
import com.yqg.ec.common.enums.SDKType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_V2;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_V3;
import static com.yqg.core.service.cashloan.vo.enums.DiscountDetailTopAreaVipUIDisplayStrategy.ENHANCE_DISCOUNT_REMOVE_TOP_AREA_AND_VIP_UI;

/**
 * @author chaoye
 * @date 2023/10/13
 */
@Slf4j
@Component
public class HomepageCommonTool {

  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private LoanAccountExtraInfoService extraInfoService;
  @Autowired
  private ExpFacade expFacade;
  @Autowired
  private HomepageParamTool homepageParamTool;

  /**
   * 判断首页通知、中通位、右下角挂件是否展示增信提额入口。
   *
   * @return 如果配置关闭、或不满足入口展示条件则返回 false ，否则返回 true 。
   * @see IncreaseCreditsEntranceDisplayLocation
   */
  public boolean shouldDisplayIncreaseCreditsEntrance(IncreaseCreditsEntranceDisplayLocation increaseCreditsEntranceDisplayLocation,
                                                      IDNHomepageLoanStatusV5 status,
                                                      Long userId,
                                                      Long accountId,
                                                      Long build) {
    if (!homepageV5Config.needIncreaseCreditsEntrance(increaseCreditsEntranceDisplayLocation.name(), status.name())) {
      return false;
    }
    return extraInfoService.shouldDisplayIncreaseCreditsEntrance(userId, accountId, build, ExtraInfoCollectSource.H5);
  }

  public DiscountDetailTopAreaVipUIDisplayStrategy getDiscountDetailTopAreaVipUIDisplayStrategy(Long userId, Long build) {
    if (userId == null) {
      return DiscountDetailTopAreaVipUIDisplayStrategy.CONTROL_GROUP;
    }
    ABTestParentGroupType abTestParentGroupType = ABTestParentGroupType.valueOf(
        expFacade.fetchResult(ABTestSceneType.OPERATION_MARKETING_GROUP_2024H2, ABTestUtil.genDiversionKeyMapByUserId(userId),
            "EXPERIMENTAL_GROUP"));
    if (abTestParentGroupType.isControlGroup()) {
      return DiscountDetailTopAreaVipUIDisplayStrategy.CONTROL_GROUP;
    }
    if (build == null || build < homepageV5Config.getDiscountDetailTopAreaVipUIDisplayStrategyVersion()) {
      return ENHANCE_DISCOUNT_REMOVE_TOP_AREA_AND_VIP_UI;
    }
    if (!homepageParamTool.allLowInterestProduct(userId, SDKType.IDN_YQD)) {
      return ENHANCE_DISCOUNT_REMOVE_TOP_AREA_AND_VIP_UI;
    }
    if (build < homepageV5Config.getDiscountDetailTopAreaVipUIDisplayStrategyV3()) {
      String result = expFacade.fetchResult(DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_V2,
          ABTestUtil.genDiversionKeyMapByUserId(userId), "ENHANCE_DISCOUNT_REMOVE_TOP_AREA_AND_VIP_UI_V2");
      return DiscountDetailTopAreaVipUIDisplayStrategy.valueOf(result);
    }
    return DiscountDetailTopAreaVipUIDisplayStrategy.ENHANCE_DISCOUNT_REMOVE_TOP_AREA_AND_VIP_UI_V3;
  }

  public CommonABTestResultGroup get371CouponListStyle(Long userId, Long build, DiscountDetailTopAreaVipUIDisplayStrategy strategy) {
    if (strategy != DiscountDetailTopAreaVipUIDisplayStrategy.ENHANCE_DISCOUNT_REMOVE_TOP_AREA_AND_VIP_UI_V3) {
      return CommonABTestResultGroup.A;
    }
    if (userId == null) {
      return CommonABTestResultGroup.A;
    }
    if (build == null || build < homepageV5Config.getDiscountDetailTopAreaVipUIDisplayStrategyV3()) {
      log.error("get371CouponListStyle build is null or less than DiscountDetailTopAreaVipUIDisplayStrategyV3 version, userId: {}, build: {}", userId, build);
      return CommonABTestResultGroup.A;
    }
    if (!homepageParamTool.allLowInterestProduct(userId, SDKType.IDN_YQD)) {
      log.error("get371CouponListStyle allLowInterestProduct is false, userId: {}, build: {}", userId, build);
      return CommonABTestResultGroup.A;
    }
    return CommonABTestResultGroup.B;
  }
}
