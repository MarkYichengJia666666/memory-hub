package com.miyou.controllers.cashloan.newhomepage.elementmodel.auth;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 未授信processer，主button动作为跳转鉴权页面，对应以下状态
 * NEVER_APPLIED("首贷未授信", IDNHomepageDisplayStatusV5.NOT_APPLIED),
 */
@Component
@Slf4j
public class NeverAppliedProcessor extends AbstractAuthProcessor {

  @Override
  public final void processCustomElement(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    //按钮
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("查看额度"))
        .action(new RedirectAction(new RedirectAction.LinkActionParam(elementConfig.getAuthElementsConfigByElementId(MainCardElementId.MAIN_BUTTON.name(), IDNHomepageLoanStatusV5.NEVER_APPLIED.name()))))
        .build());
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.NEVER_APPLIED;
  }
}