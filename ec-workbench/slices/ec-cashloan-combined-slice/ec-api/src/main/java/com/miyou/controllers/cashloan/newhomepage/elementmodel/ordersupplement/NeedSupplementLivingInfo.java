package com.miyou.controllers.cashloan.newhomepage.elementmodel.ordersupplement;

import com.google.common.collect.ImmutableMap;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.yqg.core.service.cashloan.homepage.config.NativePathConstant;
import com.yqg.core.service.marketing.channel.HomeMainCardQian1Service;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.*;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.FOREST_GREEN;

@Service
public class NeedSupplementLivingInfo extends AbstractPageCardV3Processor {

  @Autowired
  private HomeMainCardQian1Service homeMainCardQian1Service;

  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(TITLE)
        .text(TT.gen("请完成人脸验证"))
        .build());
    // 未过件场景：命中千1 承接时覆盖副标题为 0.1% 营销条（主按钮跳转不变，spec US4-1）
    boolean qian1SubTitleHit = homeMainCardQian1Service.shouldShowMarketingBar(
        homePageContext.getUserId(),
        homePageContext.getUserDeviceContextVO().getDeviceToken(),
        homePageContext.getUserDeviceContextVO().getBuild(),
        homePageContext.getUserDeviceContextVO().getSourceType(),
        homePageContext.getSdkType(), false);
    TT subTitle = qian1SubTitleHit
        ? TT.gen(HomeMainCardQian1Service.SUB_TITLE_TEXT_NOT_AUTH)
        : TT.gen("为保证资金安全，请确保本人操作");
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(SUB_TITLE)
        .text(subTitle)
        .elementParam(qian1SubTitleHit
            ? TextElement.TextParam.builder()
                .highlightMap(ImmutableMap.of(HomeMainCardQian1Service.SUB_TITLE_BOLD_PLACEHOLDER,
                    HomeMainCardQian1Service.SUB_TITLE_BOLD_DISPLAY))
                .highlightStyle(TextElement.TextStyle.BOLD)
            .highlightColor(FOREST_GREEN)
                .build()
            : TextElement.TextParam.builder().build())
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MAIN_BUTTON)
        .text(TT.gen("立即验证"))
        .action(new RedirectAction(RedirectAction.LinkActionParam.builder()
            .redirectUrl(elementConfig.getRedirectPage(NativePathConstant.SUPPLEMENT_LIVING_PAGE))
            .build()))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.image()
        .id(MAIN_ICON)
        .imageUrl(elementConfig.getAuthElementsConfigByElementId(MAIN_ICON.name(), "SUPPLEMENT_LIVING_INFO"))
        .build());
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.ORDER_NEED_SUPPLEMENT_LIVING_INFO;
  }
}
