package com.miyou.controllers.cashloan.newhomepage.elementmodel.loanmarket;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.constant.CustomerElementType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.NoneAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementColor;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.LoanMarketCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.CustomerElement;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.loanmarket.LoanMarketProductSortService;
import com.yqg.core.service.loanmarket.config.LoanMarketConfig;
import com.yqg.core.service.loanmarket.vo.ProductSortInfoVO;
import com.yqg.core.service.risk.riskflowcheck.enums.RiskFlowCheckType;
import com.yqg.core.service.statictext.StaticTextService;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LoanMarketCardElementProvider {

  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private LoanMarketConfig loanMarketConfig;
  @Autowired
  private StaticTextService staticTextService;
  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private LoanMarketProductSortService sortService;

  public List<IElement> buildLoanMarketCard(HomePageContext homePageContext, ElementColor titleBackGroundColor) {
    RedirectAction.LoanMarketLinkActionParam param = new RedirectAction.LoanMarketLinkActionParam();
    param.redirectUrl = buildRedirectUrl(homepageV5Config.getRejectLoanMarketInfoUrl(),
        homePageContext.getUserCreditsContext().getLoanMarketUserQualifyCheckResult().riskFlowCheckType);
    param.loanMarket = true;
    param.loanMarketRule = homePageContext.getUserCreditsContext().getLoanMarketUserQualifyCheckResult().riskFlowCheckType;

    //渲染贷超卡片标题
    IElement loanMarketCardTitle = ElementBuilder.text()
        .id(LoanMarketCardElementId.TITLE)
        .text(TT.gen("为您精选的贷款产品"))
        .elementParam(CustomerElement.ElementBackgroundParam.from(titleBackGroundColor))
        .build();

    //渲染贷超卡片下方跳转链接
    IElement linkedFullUrlElement = ElementBuilder.link()
        .id(LoanMarketCardElementId.LOAN_MARKET_FULL_URL)
        .text(TT.gen("更多选择"))
        .action(new RedirectAction(param))
        .build();

    NoneAction.LoanMarketNoneActionParam noneActionParam = new NoneAction.LoanMarketNoneActionParam();
    noneActionParam.loanMarket = true;
    noneActionParam.loanMarketRule = homePageContext.getUserCreditsContext().getLoanMarketUserQualifyCheckResult().riskFlowCheckType;
    //渲染贷超卡片下方主元素
    IElement mainDisplayArea = ElementBuilder.customerElement(CustomerElementType.JSON_STRING)
        .id(LoanMarketCardElementId.LOAN_MARKET_DISPLAY_MAIN_AREA)
        .elementParam(CustomerElement.LoanMarketDisplayMainAreaParam.from(cutLoanListStaticTextString()))
        .action(new NoneAction(noneActionParam))
        .build();

    List<IElement> elementList = new ArrayList<>
        (Arrays.asList(loanMarketCardTitle, linkedFullUrlElement, mainDisplayArea));
    return elementList;
  }

  private String cutLoanListStaticTextString() {
    // 从service获取排序数据（优先手动配置，如果没有则获取自动配置）
    List<ProductSortInfoVO> productList = sortService.getSortData();

    // 截取前N个产品
    int displayCount = loanMarketConfig.getLoanMarketMainCardDisplayCount();
    int endIndex = Math.min(displayCount, productList.size());
    List<ProductSortInfoVO> subList = productList.subList(0, endIndex);

    return JsonUtils.toString(subList);
  }

  private String buildRedirectUrl(String baseUrl, RiskFlowCheckType riskFlowCheckType) {
    return baseUrl + "&loanMarketRule=" + riskFlowCheckType.name();
  }
}
