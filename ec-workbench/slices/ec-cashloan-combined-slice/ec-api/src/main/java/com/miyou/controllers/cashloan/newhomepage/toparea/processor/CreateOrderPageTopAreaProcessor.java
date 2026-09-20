package com.miyou.controllers.cashloan.newhomepage.toparea.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageTopAreaProcessorType;
import com.miyou.controllers.cashloan.response.v5.toparea.TopAreaResponse;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageContextHolderUtil;
import com.yqg.core.service.cashloan.homepage.vo.HomePageContextDataHolder;
import com.yqg.ec.common.enums.SDKType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Optional;

@Service
public class CreateOrderPageTopAreaProcessor extends AbstractTopAreaProcessor {

  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private HomepageContextHolderUtil homepageContextHolderUtil;
  @Override
  public HomepageTopAreaProcessorType getProcessorType() {
    return HomepageTopAreaProcessorType.CREATE_ORDER_PAGE_TOP_AREA;
  }

  @Override
  void doProcessor(TopAreaResponse fieldsInfo, HomePageContext homePageContext) {
    //版本号需要大于等于35213
    if (homePageContext.getUserDeviceContextVO().getBuild() < homepageV5Config.getDiscountDetailAnimationBuild()) {
      return;
    }
    fieldsInfo.updateValue(buildTopAreaFromProduct(homePageContext));
    if (fieldsInfo.show) {
      return;
    }
    fieldsInfo.updateValue(homePageTopAreaTool.getDefaultOrderPageTopArea(homePageContext.newHomePageUI(), homePageContext.getLoanAccountVO().name));
  }

  private TopAreaResponse buildTopAreaFromProduct(HomePageContext homePageContext) {
    if (homePageContext.getUserProductVO().hasLowInterestProduct()) {
      return getLowInterestTopArea(homePageContext);
    }
    return homePageTopAreaTool.getCutInterestCouponTopAreaResponse(homePageContext.getHomepageUserParamsVO());
  }

  private TopAreaResponse getLowInterestTopArea(HomePageContext homePageContext) {
    if (homePageContext.getUserDeviceContextVO().getBuild() >= homepageV5Config.getDiscountDetailAnimationV3Build()) {
      if (!homePageContext.getUserProductVO().isAllLowInterestProduct()) {
        return null;
      }
    } else {
      HomePageContextDataHolder homePageContextDataHolder = homepageContextHolderUtil.getAndSetDiscountDetailAnimationVO(homePageContext.getHomePageContextHolder(),
          homePageContext.getLoanAccountVO(),
          homePageContext.getUserDeviceContextVO().getBuild(),
          homePageContext.getUserProductVO(),
          homePageContext.getUserDeviceContextVO().getPlatformType(),
          homePageContext.getUserCreditsContext().getCreditsInfoVO().tempCreditsOrderTimesLimitForCoupon);
      if (homePageContextDataHolder.getDiscountDetailAnimationVO().getDiscountDetailAnimationStrategyNew() == null) {
        return null;
      }
    }

    TopAreaResponse response = new TopAreaResponse();
    response.show = true;
    response.imageUrl = homepageV5Config.getDiscountDetailAnimationTopAreaImageUrl();
    response.toolBarColor = homepageV5Config.getDiscountDetailAnimationTopAreaToolBarColor();

    BigDecimal lowInterestDiscountAmount = Optional.ofNullable(ecHomePageProductTool.getLowInterestDiscountAmount(homePageContext.getUserProductVO().getEnableVirtualCredits(),
            homePageContext.getUserDeviceContextVO().getPlatformType(), homePageContext.getUserDeviceContextVO().getBuild(),
            homePageContext.getUserId(), homePageContext.getSdkType()))
        .orElse(BigDecimal.ZERO);
    response.boldText = MessageFormat.format(homepageV5Config.getDiscountDetailAnimationTopAreaBoldText(),
        homePageContext.getUserProductVO().getMinActualInterestRateStr(),
        homepageContentTool.getDiscountAmountFormat(lowInterestDiscountAmount, SDKType.IDN_YQD));

    response.normalText = homepageV5Config.getDiscountDetailAnimationTopAreaNormalText();
    response.toolBarIconColor = homePageTopAreaConfig.getTopBarIconColor(response.toolBarColor);
    return response;
  }

}
