package com.miyou.controllers.cashloan.productdetail.builder.impl;

import com.google.common.collect.Maps;
import com.miyou.controllers.cashloan.productdetail.builder.OrderPageDisplayDataBuilder;
import com.miyou.controllers.cashloan.productdetail.context.ProductDetailContext;
import com.miyou.controllers.cashloan.productdetail.displaydata.ExpInfoDisplayData;
import com.miyou.controllers.cashloan.productdetail.key.OrderPageBaseKeyConstants;
import com.miyou.controllers.cashloan.productdetail.key.OrderPageDisplayKey;
import com.miyou.controllers.cashloan.productdetail.key.OrderPageDisplayKeyConstants;
import com.miyou.controllers.cashloan.productdetail.basedata.FeeBaseData;
import com.miyou.controllers.cashloan.productdetail.module.BaseDataContainer;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.service.cashloan.AntiSettleCicilanPromoExpService;
import com.yqg.core.service.experiment.InterestSplitExpService;
import com.yqg.core.service.experiment.NationalDay817ExpService;
import com.yqg.core.service.experiment.enums.FirstTermPromoResGroup;
import com.yqg.core.service.experiment.enums.InterestSplitResGroup;
import com.yqg.core.service.experiment.enums.Qian1AcquisitionResGroup;
import com.yqg.core.service.experiment.enums.WanyAcquisitionResGroup;
import com.miyou.controllers.cashloan.productdetail.service.WanyShowRateTagResolver;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.discount.DisCountFeeExperimentService;
import com.yqg.core.service.loan.discount.enums.DiscountFeeResGroup;
import com.yqg.core.service.loan.discount.vo.DiscountDetailResult;
import com.yqg.core.service.marketing.channel.AdChannelAcquisitionService;
import com.yqg.core.userflow.domain.loan.service.style.IOrderPageExpeService;
import com.yqg.ec.common.enums.SDKType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ExpInfoDisplayDataBuilder implements OrderPageDisplayDataBuilder<ExpInfoDisplayData> {

  @Autowired
  private InterestSplitExpService interestSplitExpService;
  @Autowired
  private DisCountFeeExperimentService disCountFeeExperimentService;
  @Autowired
  private IOrderPageExpeService orderPageExpeService;
  @Autowired
  private AdChannelAcquisitionService adChannelAcquisitionService;
  @Autowired
  private AntiSettleCicilanPromoExpService antiSettleCicilanPromoExpService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private WanyShowRateTagResolver wanyShowRateTagResolver;
  @Autowired
  private NationalDay817ExpService nationalDay817ExpService;

  @Override
  public OrderPageDisplayKey<ExpInfoDisplayData> key() {
    return OrderPageDisplayKeyConstants.EXPINFO;
  }

  @Override
  public ExpInfoDisplayData build(ProductDetailContext context, BaseDataContainer baseDataContainer) {
    Long userId = context.getUserId();
    Long build = context.getBuild();

    InterestSplitExpService.ExcludeFeeFromLastResult excludeFee =
        interestSplitExpService.resolveExcludeFeeFromLastResult(userId);
    boolean excludePlatformFee = excludeFee.isExcludePlatformFee();
    boolean excludePpn = excludeFee.isExcludePpn();
    InterestSplitResGroup interestSplitResGroup = excludePlatformFee ? InterestSplitResGroup.EXP_GROUP : InterestSplitResGroup.CONTROL_GROUP;
    InterestSplitResGroup interestSplitV2ResGroup = InterestSplitResGroup.CONTROL_GROUP;
    InterestSplitResGroup interestSplitV3ResGroup = excludePpn ? InterestSplitResGroup.EXP_GROUP : InterestSplitResGroup.CONTROL_GROUP;

    DiscountFeeResGroup freeTermsResGroup = disCountFeeExperimentService.lastResultRunning(userId, build);

    Map<String, String> expeResultMap = Maps.newHashMap();
    // 首贷-账户成长2.0:下单提额降息实验
    assembleFirstLoanOrderToBoostExpe(userId, build, expeResultMap);
    // 国庆端内氛围：命中实验组下发样式标记，前端据此包装下单页
    assembleNationalDay817Expe(context, expeResultMap);

    // Meta 千1 承接实验分组（供前端结合 showOneThousandthRate 渲染 rateDiscountCornerMark 文案）
    // 下单页已过件，真实利率已知，需用过件后路径 isQian1Hit（与 LoanInfoDisplayDataBuilder/OrderPageAbTestService 对齐）
    String deviceToken = context.getViewerContext() != null ? context.getViewerContext().deviceToken : null;
    SDKType sdkType = context.getSdkType();
    Qian1AcquisitionResGroup qian1ResGroup = Qian1AcquisitionResGroup.from(
        adChannelAcquisitionService.isQian1Hit(userId, deviceToken, build, context.getSourceType(), sdkType));

    WanyAcquisitionResGroup wanyResGroup = WanyAcquisitionResGroup.CONTROL_GROUP;
    boolean wanyShowRateTag = false;
    try {
      boolean landing = wanyShowRateTagResolver.resolvePostApprovalLanding(context, sdkType);
      wanyResGroup = WanyAcquisitionResGroup.from(landing);
      if (landing) {
        wanyShowRateTag = wanyShowRateTagResolver.resolve(context, sdkType);
      }
    } catch (Exception e) {
      log.warn("[ExpInfoDisplayDataBuilder] resolve wany acquisition failed, userId={}", userId, e);
      wanyResGroup = WanyAcquisitionResGroup.CONTROL_GROUP;
      wanyShowRateTag = false;
    }

    // 防结清 Cicilan 首期强化优惠 UI 子实验（TAPD-367516）：实验组→EXP_GROUP，未入组/取数失败→CONTROL_GROUP（fail-open）
    FirstTermPromoResGroup firstTermPromoResGroup = resolveFirstTermPromoResGroup(context, baseDataContainer);

    log.info("[ExpInfoDisplayDataBuilder] Built expInfo,userId={}, build={}, interestSplitResGroup={}, qian1ResGroup={}, "
            + "wanyResGroup={}, wanyShowRateTag={}, interestSplitV2ResGroup={}, interestSplitV3ResGroup={}, "
            + "firstTermPromoResGroup={}",
        userId, build, interestSplitResGroup, qian1ResGroup, wanyResGroup, wanyShowRateTag,
        interestSplitV2ResGroup, interestSplitV3ResGroup, firstTermPromoResGroup);

    return ExpInfoDisplayData.builder()
        .interestSplitResGroup(interestSplitResGroup)
        .interestSplitV2ResGroup(interestSplitV2ResGroup)
        .interestSplitV3ResGroup(interestSplitV3ResGroup)
        .freeTermsResGroup(freeTermsResGroup)
        .qian1ResGroup(qian1ResGroup)
        .wanyResGroup(wanyResGroup)
        .wanyShowRateTag(wanyShowRateTag)
        .firstTermPromoResGroup(firstTermPromoResGroup)
        .expeResultMap(expeResultMap)
        .build();
  }

  /**
   * 解析首期强化优惠子实验分组（TAPD-367516）。取 FEE 模块的 {@link DiscountDetailResult} 与 accountId 判入组；
   * FEE 缺失 / discountDetail 为 null / accountId 解析失败 → 返回 CONTROL_GROUP（fail-open，不抛异常）。
   */
  private FirstTermPromoResGroup resolveFirstTermPromoResGroup(ProductDetailContext context,
      BaseDataContainer baseDataContainer) {
    Long userId = context.getUserId();
    FeeBaseData feeBaseData = baseDataContainer.get(OrderPageBaseKeyConstants.FEE);
    if (feeBaseData == null) {
      return FirstTermPromoResGroup.CONTROL_GROUP;
    }
    DiscountDetailResult discountDetail = feeBaseData.getDiscountDetailResult();
    if (discountDetail == null) {
      return FirstTermPromoResGroup.CONTROL_GROUP;
    }
    Long accountId = loanAccountService.getAccountIdByUserId(userId, context.getSdkType());
    if (accountId == null) {
      return FirstTermPromoResGroup.CONTROL_GROUP;
    }
    boolean experimentGroup = Boolean.TRUE.equals(context.getOrComputeAbResult(
        AntiSettleCicilanPromoExpService.AB_KEY_FIRST_TERM_PROMO_EXPERIMENT_GROUP, Boolean.class,
        () -> antiSettleCicilanPromoExpService.isExperimentGroup(userId, accountId, context.getBuild(), discountDetail)));
    return FirstTermPromoResGroup.from(experimentGroup);
  }

  /**
   * 817 国庆氛围实验（TAPD-1371279）：命中实验组时置样式标记，未命中不下发该键（对照组与空白组无差异）。
   * 本次请求内只调用一次分流，无需再走 {@code ProductDetailContext} 的 AB 结果缓存。
   */
  private void assembleNationalDay817Expe(ProductDetailContext context, Map<String, String> expeResultMap) {
    if (nationalDay817ExpService.isAtmosphereGroup(context.getUserId())) {
      expeResultMap.put(NationalDay817ExpService.MARK_ATMOSPHERE, Boolean.TRUE.toString());
    }
  }

  private void assembleFirstLoanOrderToBoostExpe(Long userId, Long build, Map<String, String> expeResultMap) {

    String key = UserFlowExperimentEnum.FIRST_LOAN_ORDER_TO_BOOST.getKey();
    String result = orderPageExpeService.abTestFirstLoanOrderToBoostExpe(userId, build, key);
    expeResultMap.put(key, result);
  }

}
