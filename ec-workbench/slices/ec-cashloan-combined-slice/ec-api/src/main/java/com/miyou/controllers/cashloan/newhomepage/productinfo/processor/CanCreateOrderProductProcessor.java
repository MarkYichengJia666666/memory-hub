package com.miyou.controllers.cashloan.newhomepage.productinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProductProcessorType;
import com.miyou.controllers.cashloan.response.home.SeaProductDisplayData;
import com.miyou.controllers.cashloan.response.v5.product.ProductListResponse;
import com.miyou.controllers.cashloan.response.v5.product.ProductResponse;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.controllers.cashloan.utilities.HomepageProductTool;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.model.sql.loan.creditsaware.strategy.CreditAwareStrategyApplyResult;
import com.yqg.core.model.sql.loan.creditsaware.strategy.FirstLoanPopupDecisionSnapshot;
import com.yqg.core.service.agreement.AgreementService;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.UserProductDetailVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.loan.discount.DisCountFeeExperimentService;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CanCreateOrderProductProcessor extends AbstractProductProcessor {
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private HomepageProductTool homepageProductTool;
  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private AgreementService agreementService;
  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private DisCountFeeExperimentService disCountFeeExperimentService;

  @Override
  public void process(ProductListResponse response, HomePageContext homePageContext) {
    BigDecimal stepAmount = cashLoanConfig.getStepAmount(homePageContext.getSdkType());
    UserProductDetailVO userProductVO = homePageContext.getUserProductVO();
    Long build = homePageContext.getUserDeviceContextVO().getBuild();
    HomeDisplayStrategy styleFor367 = homepageContentTool.getStyleFor367(homePageContext.getUserId(), build, userProductVO.getProductIds().size());
    HomeDisplayStrategy use49Version = homepageContentTool.getDisplayCouponStrategy(homePageContext.getUserId(), build);
    boolean joinFreeTerms = disCountFeeExperimentService.isExpGroupFromLastResult(homePageContext.getUserId());

    List<SeaProductDisplayData> seaProductDisplayData = homepageProductTool.listDisplayProduct(
        homePageContext.getUserId(),
        userProductVO.getProductConfigList(),
        userProductVO.getEnableVirtualCredits(),
        build,
        use49Version,
        userProductVO.getMaxTermsProductId(),
        styleFor367,
        joinFreeTerms);
    List<ProductResponse> productResponseList = seaProductDisplayData.stream().map(ProductResponse::from).collect(Collectors.toList());
    homepageProductTool.checkProductDisplayList(homePageContext.getHomepageUserParamsVO(), homePageContext.getLoanAccountVO().loanUserTypeVO,
        homePageContext.getSdkType());
    BigDecimal minAmount = Optional.ofNullable(homePageContext.getUserProductVO().getMinAmount()).orElse(BigDecimal.ZERO);
    String agreementUrlWithParam = agreementService.getAgreementUrlWithParam(homePageContext.getUserId(), homePageContext.getSdkType(), homePageContext.getUserDeviceContextVO().getSourceType());
    Set<BigDecimal> quickInputAmountList = ProductListResponse.buildQuickInputAmountList(stepAmount,
        minAmount, userProductVO.getEnableVirtualCredits(), homePageContext.getHomePageServiceManager().getHomepageV5Config().getProductAmountQuickInputRatioList());

    String defaultProductId = Optional.ofNullable(ecHomePageProductTool.routeChooseDefaultOrMaxProduct(homePageContext.getUserId(), homePageContext.getSdkType(), userProductVO.getEnableVirtualCredits()))
        .map(item -> item.id)
        .map(YqgHashids::encode)
        .orElse(null);

    response
        .setStepAmount(stepAmount)
        .setData(productResponseList)
        .setAgreementUrl(agreementUrlWithParam)
        .setMaxAmount(userProductVO.getEnableVirtualCredits())
        .setMinAmount(minAmount)
        .setDefaultChooseProductId(defaultProductId)
        .setQuickInputAmountList(quickInputAmountList)
        .setMinActualInterestRate(homePageContext.getUserProductVO().getMinActualInterestRate())
        .setMaxTerms(userProductVO.getMaxTerms())
        .setReductionTermsContent(joinFreeTerms ? TT.gen("免一期") : null)
        .setDefaultAmount(homePageContext.getReloanDefaultAmount())
    ;

    FirstLoanPopupDecisionSnapshot firstLoanPopupDecisionSnapshot = homePageContext.getFirstLoanPopupDecisionSnapshot();
    if ("C".equals(firstLoanPopupDecisionSnapshot.getCreditEnhanceBucket())) {
      response.setBeforeMaxAmount(getBeforeMaxAmount(homePageContext));
      return;
    }

    CreditAwareStrategyApplyResult creditAwareStrategyApplyResult = homePageContext.getCreditsDetailsAwarePopupType();
    if (creditAwareStrategyApplyResult.getPopupType().hasPopup()) {
      response.setBeforeMaxAmount(creditAwareStrategyApplyResult.getBeforeMaxAmount().setScale(0, RoundingMode.DOWN));
    }
  }

  @Override
  protected HomepageProductProcessorType getProductProcessorType() {
    return HomepageProductProcessorType.CAN_CREATE_ORDER_PRODUCT_INFO_PROCESSOR;
  }

  private BigDecimal getBeforeMaxAmount(HomePageContext homePageContext) {
    LoanUserCreditsInfoVO vo = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    BigDecimal maxAmount = homePageContext.getUserProductVO().getEnableVirtualCredits();
    BigDecimal beforeMaxAmount;
    BigDecimal loanT0CreditPercent = BigDecimal.ONE.subtract(cashLoanConfig.getLoanT0CreditPercent());
    if (Objects.isNull(vo) || Objects.isNull(vo.tempCreditsForCoupon) || vo.tempCreditsForCoupon.compareTo(BigDecimal.ZERO) <= 0) {
      beforeMaxAmount = maxAmount.multiply(loanT0CreditPercent);
    } else {
      beforeMaxAmount = vo.getTotalFixedCreditsForVirtual().add(vo.tempCreditsForIncreaseCreditsRisk);
      if (beforeMaxAmount.compareTo(maxAmount) >= 0) {
        beforeMaxAmount = maxAmount.multiply(loanT0CreditPercent);
      }
    }
    return beforeMaxAmount.setScale(0, RoundingMode.DOWN);
  }

}
